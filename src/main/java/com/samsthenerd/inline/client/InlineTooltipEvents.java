package com.samsthenerd.inline.client;

import com.samsthenerd.inline.api.InlineAPI;
import com.samsthenerd.inline.api.MatchContext;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.item.MediaTooltip;
import at.petra_k.hexcasting.common.item.ItemPatternScroll;
import at.petra_k.hexcasting.interop.inline.InlinePatternChatRenderer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Applies the common Inline matcher pipeline to 1.12.2 item tooltips. */
@EventBusSubscriber(modid = "hexcasting", value = Side.CLIENT)
public final class InlineTooltipEvents {
    private static final List<TooltipCapture> CAPTURES = new ArrayList<>();
    private static final int PREVIEW_SIZE = 128;
    private static final int PREVIEW_RESERVED_LINES = 14;
    private static final int PREVIEW_RESERVED_WIDTH = 32;

    private InlineTooltipEvents() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (event == null || event.getToolTip() == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        MatchContext context = new MatchContext(
            minecraft == null ? null : minecraft.player,
            minecraft == null ? null : minecraft.world);
        List<String> formatted = new ArrayList<>(event.getToolTip().size());
        boolean hasTokens = false;
        MediaTooltip.Entry media = null;
        int mediaLineIndex = -1;
        int textLineCount = event.getToolTip().size();
        int originalTooltipWidth = 0;
        for (int index = 0; index < event.getToolTip().size(); index++) {
            String line = event.getToolTip().get(index);
            MediaTooltip.Entry parsedMedia = MediaTooltip.parse(line);
            if (parsedMedia != null) {
                media = parsedMedia;
                mediaLineIndex = index;
                String blank = MediaTooltip.blankText(parsedMedia,
                    minecraft == null ? null : minecraft.fontRenderer);
                formatted.add(blank);
                event.getToolTip().set(index, blank);
                if (minecraft != null && minecraft.fontRenderer != null) {
                    originalTooltipWidth = Math.max(originalTooltipWidth,
                        minecraft.fontRenderer.getStringWidth(blank));
                }
                continue;
            }
            String rendered = InlineAPI.formatPlainText(line, context);
            formatted.add(rendered);
            hasTokens |= InlinePatternChatRenderer.containsTokens(rendered);
            if (minecraft != null && minecraft.fontRenderer != null
                && !InlinePatternChatRenderer.containsTokens(rendered)) {
                originalTooltipWidth = Math.max(originalTooltipWidth,
                    minecraft.fontRenderer.getStringWidth(rendered));
            }
            event.getToolTip().set(index,
                InlinePatternChatRenderer.stripTokenText(rendered));
        }

        HexPattern previewPattern = null;
        boolean ancient = false;
        boolean ancientShiftAccent = false;
        if (event.getItemStack() != null
            && event.getItemStack().getItem() instanceof ItemPatternScroll) {
            previewPattern = ItemPatternScroll.getPattern(
                event.getItemStack(), minecraft == null ? null : minecraft.world);
            net.minecraft.nbt.NBTTagCompound tag = event.getItemStack().getTagCompound();
            ancient = tag != null && (tag.hasKey(ItemPatternScroll.TAG_OP_ID, 8)
                || tag.getBoolean(ItemPatternScroll.TAG_ANCIENT));
            ancientShiftAccent = ancient && minecraft != null && minecraft.gameSettings != null
                && minecraft.gameSettings.keyBindSneak.isKeyDown();
            if (previewPattern != null) {
                // Forge 1.12 has no TooltipComponent hook. Invisible lines
                // reserve the same 128x128 image area as 1.20.1's component;
                // the actual image is painted in PostText.
                event.getToolTip().add(repeat(' ', PREVIEW_RESERVED_WIDTH));
                for (int i = 1; i < PREVIEW_RESERVED_LINES; i++) {
                    event.getToolTip().add("");
                }
                formatted.add(repeat(' ', PREVIEW_RESERVED_WIDTH));
                for (int i = 1; i < PREVIEW_RESERVED_LINES; i++) {
                    formatted.add("");
                }
            }
        }

        synchronized (CAPTURES) {
            removeCapture(event.getItemStack());
            if (hasTokens || previewPattern != null || media != null) {
                TooltipCapture capture = new TooltipCapture(event.getItemStack(), formatted,
                    previewPattern, ancient, ancientShiftAccent, textLineCount,
                    media, mediaLineIndex, originalTooltipWidth);
                if (hasTokens && minecraft != null && minecraft.fontRenderer != null
                    && originalTooltipWidth > 0) {
                    // ItemTooltipEvent owns the mutable list.  Wrapping here
                    // is early enough to let GuiUtils calculate the final box
                    // from the already-split placeholder rows; the later
                    // RenderTooltipEvent.Pre list is unmodifiable on some
                    // Forge/OptiFine GUI paths.
                    capture.wrapTokenLines(minecraft.fontRenderer,
                        originalTooltipWidth, event.getToolTip());
                }
                CAPTURES.add(0, capture);
                while (CAPTURES.size() > 8) {
                    CAPTURES.remove(CAPTURES.size() - 1);
                }
            }
        }
    }

    /** Draw the decoded glyphs after Forge has drawn the normal tooltip text. */
    @SubscribeEvent
    public static void onTooltipPostText(RenderTooltipEvent.PostText event) {
        if (event == null || event.getLines() == null
            || event.getFontRenderer() == null) {
            return;
        }

        TooltipCapture capture;
        synchronized (CAPTURES) {
            capture = findCapture(event.getStack());
            if (capture != null) {
                CAPTURES.remove(capture);
            }
        }
        if (capture == null && event.getStack() != null
            && event.getStack().getItem() instanceof at.petra_k.hexcasting.common.item.ItemHexStaff) {
            // Some 1.12.2 GUI paths bypass ItemTooltipEvent's object identity
            // and hand PostText a copied stack.  Reconstruct the capture from
            // the already-prepared lines instead of losing the glyph row.
            List<String> lines = new ArrayList<>(event.getLines());
            boolean hasTokens = false;
            for (String line : lines) {
                hasTokens |= InlinePatternChatRenderer.containsTokens(line);
            }
            if (hasTokens) {
                capture = new TooltipCapture(event.getStack(), lines, null,
                    false, false, lines.size(), null, -1, event.getWidth());
            }
        }
        if (capture == null) {
            return;
        }

        if (capture.previewPattern != null) {
            int previewY = event.getY() + capture.textLineCount * 10
                + (capture.textLineCount > 0 ? 2 : 0);
            InlinePatternChatRenderer.drawTooltipPattern(capture.previewPattern,
                event.getX(), previewY, capture.ancient, capture.ancientShiftAccent);
        }

        int lineY = event.getY();
        int sourceIndex = 0;
        List<String> renderedLines = event.getLines();
        for (int lineIndex = 0; lineIndex < renderedLines.size(); lineIndex++) {
            String renderedLine = renderedLines.get(lineIndex);
            String normalizedRenderedLine =
                InlinePatternChatRenderer.stripFormatting(
                    InlinePatternChatRenderer.stripTokenText(renderedLine));
            SourceLine source = capture.findSource(normalizedRenderedLine, sourceIndex);
            if (source != null) {
                boolean drawn = InlinePatternChatRenderer.drawInlinePatterns(
                    event.getFontRenderer(), source.text, normalizedRenderedLine,
                    event.getX(), lineY, 0xFFFFFFFF);
                if (drawn || !InlinePatternChatRenderer.containsTokens(source.text)) {
                    sourceIndex = source.index + 1;
                }
            }
            if (capture.media != null && lineIndex == capture.mediaLineIndex) {
                drawMediaTooltip(event.getFontRenderer(), event.getX(), lineY,
                    capture.media);
            }
            lineY += 10;
            // GuiUtils inserts the same two-pixel title/description gap after
            // the first tooltip line when there is more than one line.
            if (lineIndex == 0 && renderedLines.size() > 1) {
                lineY += 2;
            }
        }
    }

    private static TooltipCapture findCapture(ItemStack stack) {
        if (CAPTURES.isEmpty()) {
            return null;
        }
        for (TooltipCapture capture : CAPTURES) {
            if (capture.stack == stack) {
                return capture;
            }
        }
        // Forge normally passes the same object through both events. If a
        // renderer copied the stack, the most recent capture is still the
        // only safe candidate because tooltip rendering is synchronous.
        return CAPTURES.get(0);
    }

    private static void removeCapture(ItemStack stack) {
        for (int i = CAPTURES.size() - 1; i >= 0; i--) {
            if (CAPTURES.get(i).stack == stack) {
                CAPTURES.remove(i);
            }
        }
    }

    private static String repeat(char value, int count) {
        StringBuilder out = new StringBuilder(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            out.append(value);
        }
        return out.toString();
    }

    /** Draw the media line with the same RGB values as Hex 1.20.1. */
    private static void drawMediaTooltip(FontRenderer font, int x, int y,
                                         MediaTooltip.Entry entry) {
        if (font == null || entry == null) {
            return;
        }
        String[] values = MediaTooltip.formattedValues(entry);
        String template = MediaTooltip.advancedTemplate();
        int cursor = 0;
        for (int valueIndex = 0; valueIndex < values.length; valueIndex++) {
            int placeholder = findPlaceholder(template, cursor);
            if (placeholder < 0) {
                break;
            }
            String prefix = template.substring(cursor, placeholder);
            font.drawString(prefix, x, y, 0xFFFFFFFF);
            x += font.getStringWidth(prefix);
            int color = valueIndex == values.length - 1
                ? mediaBarColor(entry.getMedia(), entry.getMaxMedia())
                : MediaTooltip.HEX_COLOR;
            font.drawString(values[valueIndex], x, y, color);
            x += font.getStringWidth(values[valueIndex]);
            cursor = placeholder + placeholderLength(template, placeholder);
        }
        if (cursor < template.length()) {
            font.drawString(template.substring(cursor), x, y, 0xFFFFFFFF);
        }
    }

    private static int findPlaceholder(String template, int from) {
        int stringPlaceholder = template.indexOf("%s", from);
        int integerPlaceholder = template.indexOf("%d", from);
        if (stringPlaceholder < 0) {
            return integerPlaceholder;
        }
        if (integerPlaceholder < 0) {
            return stringPlaceholder;
        }
        return Math.min(stringPlaceholder, integerPlaceholder);
    }

    private static int placeholderLength(String template, int at) {
        return at + 1 < template.length() && template.charAt(at + 1) == 'd' ? 2 : 2;
    }

    private static int mediaBarColor(long media, long maxMedia) {
        float amount = maxMedia <= 0L ? 0.0F
            : Math.max(0.0F, Math.min(1.0F, (float) media / (float) maxMedia));
        int red = Math.round(84.0F + 170.0F * amount);
        int green = Math.round(57.0F + 146.0F * amount);
        int blue = Math.round(138.0F + 92.0F * amount);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static final class TooltipCapture {
        private final ItemStack stack;
        private List<String> lines;
        private final HexPattern previewPattern;
        private final boolean ancient;
        private final boolean ancientShiftAccent;
        private final int textLineCount;
        private final MediaTooltip.Entry media;
        private int mediaLineIndex;
        private final int originalTooltipWidth;

        private TooltipCapture(ItemStack stack, List<String> lines,
                               HexPattern previewPattern, boolean ancient,
                               boolean ancientShiftAccent,
                               int textLineCount, MediaTooltip.Entry media,
                               int mediaLineIndex, int originalTooltipWidth) {
            this.stack = stack;
            this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
            this.previewPattern = previewPattern;
            this.ancient = ancient;
            this.ancientShiftAccent = ancientShiftAccent;
            this.textLineCount = Math.max(0, textLineCount);
            this.media = media;
            this.mediaLineIndex = mediaLineIndex;
            this.originalTooltipWidth = Math.max(0, originalTooltipWidth);
        }

        private boolean hasTokens() {
            for (String line : lines) {
                if (InlinePatternChatRenderer.containsTokens(line)) {
                    return true;
                }
            }
            return false;
        }

        private void wrapTokenLines(FontRenderer font, int maxWidth,
                                    List<String> tooltipLines) {
            if (font == null || tooltipLines == null || maxWidth <= 0
                || !hasTokens()) {
                return;
            }

            List<String> wrapped = new ArrayList<>();
            int wrappedMediaLineIndex = -1;
            for (int i = 0; i < lines.size(); i++) {
                String source = lines.get(i);
                List<String> rows = InlinePatternChatRenderer.containsTokens(source)
                    ? InlinePatternChatRenderer.wrapTokenLine(font, source, maxWidth)
                    : Collections.singletonList(source);
                if (i == mediaLineIndex) {
                    wrappedMediaLineIndex = wrapped.size();
                }
                wrapped.addAll(rows);
            }

            lines = Collections.unmodifiableList(wrapped);
            mediaLineIndex = wrappedMediaLineIndex;
            tooltipLines.clear();
            for (String source : wrapped) {
                tooltipLines.add(InlinePatternChatRenderer.stripTokenText(source));
            }
        }

        private SourceLine findSource(String renderedLine, int fromIndex) {
            if (renderedLine == null) {
                return null;
            }
            for (int i = Math.max(0, fromIndex); i < lines.size(); i++) {
                String stripped = InlinePatternChatRenderer.stripTokenText(lines.get(i));
                String comparable = InlinePatternChatRenderer.stripFormatting(stripped);
                String candidate = InlinePatternChatRenderer.stripFormatting(renderedLine);
                if (comparable.equals(candidate)
                    || (!candidate.isEmpty() && comparable.contains(candidate))) {
                    return new SourceLine(i, lines.get(i));
                }
                // A caller that did not receive ItemTooltipEvent may hand us
                // the original private-use token line.  Compare both sides
                // after replacing that token with its measured spaces.
                String normalized = InlinePatternChatRenderer.stripFormatting(
                    InlinePatternChatRenderer.stripTokenText(renderedLine));
                if (comparable.equals(normalized)
                    || (!normalized.isEmpty() && comparable.contains(normalized))) {
                    return new SourceLine(i, lines.get(i));
                }
            }
            return null;
        }
    }

    private static final class SourceLine {
        private final int index;
        private final String text;

        private SourceLine(int index, String text) {
            this.index = index;
            this.text = text;
        }
    }
}
