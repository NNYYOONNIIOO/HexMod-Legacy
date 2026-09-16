package com.samsthenerd.inline.client;

import com.samsthenerd.inline.api.InlineAPI;
import com.samsthenerd.inline.api.MatchContext;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.item.ItemPatternScroll;
import at.petra_k.hexcasting.interop.inline.InlinePatternChatRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Applies the common Inline matcher pipeline to 1.12.2 item tooltips. */
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
        int textLineCount = event.getToolTip().size();
        for (int index = 0; index < event.getToolTip().size(); index++) {
            String line = event.getToolTip().get(index);
            String rendered = InlineAPI.formatPlainText(line, context);
            formatted.add(rendered);
            hasTokens |= InlinePatternChatRenderer.containsTokens(rendered);
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
            }
        }

        synchronized (CAPTURES) {
            removeCapture(event.getItemStack());
            if (hasTokens || previewPattern != null) {
                CAPTURES.add(0, new TooltipCapture(event.getItemStack(), formatted,
                    previewPattern, ancient, ancientShiftAccent, textLineCount));
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
            SourceLine source = capture.findSource(renderedLine, sourceIndex);
            if (source != null) {
                boolean drawn = InlinePatternChatRenderer.drawInlinePatterns(
                    event.getFontRenderer(), source.text, renderedLine,
                    event.getX(), lineY, 0xFFFFFFFF);
                if (drawn || !InlinePatternChatRenderer.containsTokens(source.text)) {
                    sourceIndex = source.index + 1;
                }
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

    private static final class TooltipCapture {
        private final ItemStack stack;
        private final List<String> lines;
        private final HexPattern previewPattern;
        private final boolean ancient;
        private final boolean ancientShiftAccent;
        private final int textLineCount;

        private TooltipCapture(ItemStack stack, List<String> lines,
                               HexPattern previewPattern, boolean ancient,
                               boolean ancientShiftAccent,
                               int textLineCount) {
            this.stack = stack;
            this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
            this.previewPattern = previewPattern;
            this.ancient = ancient;
            this.ancientShiftAccent = ancientShiftAccent;
            this.textLineCount = Math.max(0, textLineCount);
        }

        private SourceLine findSource(String renderedLine, int fromIndex) {
            if (renderedLine == null) {
                return null;
            }
            for (int i = Math.max(0, fromIndex); i < lines.size(); i++) {
                String stripped = InlinePatternChatRenderer.stripTokenText(lines.get(i));
                if (stripped.equals(renderedLine)
                    || (!renderedLine.isEmpty() && stripped.contains(renderedLine))) {
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
