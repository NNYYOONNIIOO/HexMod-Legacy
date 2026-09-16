package at.petra_k.hexcasting.interop.inline;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client-side renderer for pattern tokens embedded in chat text.
 *
 * <p>Vanilla 1.12.2 stores a separate {@link ChatLine} for every wrapped
 * line. The renderer mirrors that list after vanilla has drawn it, so the
 * pattern glyph uses the same row index, scroll offset and fade alpha as the
 * text. This is important because a single global "last message" position
 * cannot follow chat scrolling or disappear with an aged-out message.</p>
 */
public final class InlinePatternChatRenderer {
    private static final String TOKEN_PREFIX = "\uE000hexcasting:pattern:";
    private static final String TOKEN_SUFFIX = "\uE001";
    private static final String LEGACY_TOKEN_PREFIX = "\\uE000hexcasting:pattern:";
    private static final String LEGACY_TOKEN_SUFFIX = "\\uE001";
    private static final Pattern TOKEN = Pattern.compile(
        "(?:" + Pattern.quote(TOKEN_PREFIX) + "|"
            + Pattern.quote(LEGACY_TOKEN_PREFIX) + ")"
            + "([A-Za-z_/,]+)(?:\\|([0-9A-Fa-f]{8}))?"
            + "(?:" + Pattern.quote(TOKEN_SUFFIX) + "|"
            + Pattern.quote(LEGACY_TOKEN_SUFFIX) + ")");
    private static final int MAX_TRACKED_MESSAGES = 100;

    private static final List<TrackedMessage> trackedMessages = new ArrayList<>();
    private static long nextSequence;
    private static int chatOriginX;
    private static int chatOriginY = -48;

    private InlinePatternChatRenderer() {
    }

    public static String token(String signature) {
        return TOKEN_PREFIX + signature + TOKEN_SUFFIX;
    }

    /**
     * Remember the formatted form before the token is replaced with spaces.
     * The update counter is the same counter GuiNewChat writes into ChatLine,
     * which lets us distinguish two identical messages in the chat history.
     */
    public static synchronized void capture(ITextComponent message) {
        String text = message == null ? null : message.getUnformattedText();
        if (text == null) {
            return;
        }

        Matcher matcher = TOKEN.matcher(text);
        if (!matcher.find()) {
            return;
        }
        matcher.reset();

        List<PatternToken> tokens = new ArrayList<>();
        int rawCursor = 0;
        int plainCursor = 0;
        while (matcher.find()) {
            plainCursor += matcher.start() - rawCursor;
            int plainStart = plainCursor;
            String replacement = tokenReplacement(matcher.group(1));
            plainCursor += replacement.length();
            tokens.add(new PatternToken(matcher.group(1), parseColor(matcher.group(2)),
                plainStart, plainCursor));
            rawCursor = matcher.end();
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        int updateCounter = -1;
        if (minecraft != null && minecraft.ingameGUI != null) {
            updateCounter = minecraft.ingameGUI.getUpdateCounter();
        }

        trackedMessages.add(0, new TrackedMessage(
            stripTokenText(text), tokens, updateCounter, nextSequence++));
        while (trackedMessages.size() > MAX_TRACKED_MESSAGES) {
            trackedMessages.remove(trackedMessages.size() - 1);
        }
    }

    /** Replace private tokens with a width-preserving blank in vanilla chat. */
    public static ITextComponent stripTokens(ITextComponent message) {
        if (message == null) {
            return null;
        }
        TextComponentString result = new TextComponentString(
            stripTokenText(message.getUnformattedText()));
        result.setStyle(message.getStyle());
        return result;
    }

    /** Return the text that vanilla 1.12.2 should measure and draw. */
    public static String stripTokenText(String text) {
        if (text == null) {
            return "";
        }
        Matcher matcher = TOKEN.matcher(text);
        StringBuffer result = new StringBuffer(text.length());
        while (matcher.find()) {
            matcher.appendReplacement(result,
                Matcher.quoteReplacement(tokenReplacement(matcher.group(1))));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String tokenReplacement(String signature) {
        int width = HexPatternChatGeometry.width(signature);
        // The default 1.12.2 font assigns four pixels to an ASCII space.
        int spaces = Math.max(1, (width + 3) / 4);
        StringBuilder replacement = new StringBuilder(spaces);
        for (int i = 0; i < spaces; i++) {
            replacement.append(' ');
        }
        return replacement.toString();
    }

    /** Return whether a string contains one or more Inline pattern tokens. */
    public static boolean containsTokens(String text) {
        return text != null && TOKEN.matcher(text).find();
    }

    /** Measure text after replacing private tokens with their reserved width. */
    public static int stringWidth(FontRenderer font, String text) {
        return font == null ? 0 : font.getStringWidth(stripTokenText(text));
    }

    /** Draw ordinary text and then place the real pattern glyphs over its blanks. */
    public static void drawInlineText(FontRenderer font, String text, int x, int y,
                                      int argb) {
        if (font == null || text == null) {
            return;
        }
        font.drawString(stripTokenText(text), x, y, argb);
        drawInlinePatterns(font, text, x, y, argb);
    }

    /** Draw every pattern token in a line at the positions measured by FontRenderer. */
    public static void drawInlinePatterns(FontRenderer font, String text, int x, int y,
                                          int argb) {
        if (font == null || text == null || !containsTokens(text)) {
            return;
        }
        Matcher matcher = TOKEN.matcher(text);
        while (matcher.find()) {
            String prefix = stripTokenText(text.substring(0, matcher.start()));
            drawInlinePattern(matcher.group(1),
                x + font.getStringWidth(prefix), y,
                parseColorOrDefault(matcher.group(2), argb));
        }
    }

    /**
     * Draw tokens from one source line that belong to a wrapped vanilla line.
     * Forge may split tooltip text before RenderTooltipEvent.PostText, so the
     * source line and the line actually painted by FontRenderer are not always
     * the same string.
     */
    public static boolean drawInlinePatterns(FontRenderer font, String sourceText,
                                             String renderedLine, int x, int y,
                                             int argb) {
        if (font == null || sourceText == null || renderedLine == null
            || renderedLine.isEmpty() || !containsTokens(sourceText)) {
            return false;
        }
        String plainSource = stripTokenText(sourceText);
        int lineStart = plainSource.indexOf(renderedLine);
        if (lineStart < 0) {
            return false;
        }

        boolean drawn = false;
        Matcher matcher = TOKEN.matcher(sourceText);
        while (matcher.find()) {
            // Translate the raw token position into the width-preserving
            // plain string before comparing it with the wrapped line.
            String beforeToken = stripTokenText(sourceText.substring(0, matcher.start()));
            int tokenStart = beforeToken.length();
            if (tokenStart >= lineStart
                && tokenStart < lineStart + renderedLine.length()) {
                drawInlinePattern(matcher.group(1),
                    x + font.getStringWidth(
                        plainSource.substring(lineStart, tokenStart)), y,
                    parseColorOrDefault(matcher.group(2), argb));
                drawn = true;
            }
        }
        return drawn;
    }

    /** Draw one decoded pattern without drawing its text placeholder. */
    public static void drawInlinePattern(String signature, int x, int y, int argb) {
        if (signature == null || signature.isEmpty()) {
            return;
        }
        int alpha = (argb >>> 24) & 0xFF;
        HexPatternChatGeometry.drawAt(signature, x, y, alpha, argb);
    }

    /** Render the 1.20.1-style scroll image and its full-size pattern. */
    public static void drawTooltipPattern(HexPattern pattern, int x, int y,
                                           boolean ancient) {
        drawTooltipPattern(pattern, x, y, ancient, false);
    }

    /** Render a tooltip preview, with the ancient Shift accent when requested. */
    public static void drawTooltipPattern(HexPattern pattern, int x, int y,
                                          boolean ancient, boolean shiftAccent) {
        if (pattern == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.getTextureManager() == null) {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        ResourceLocation background = new ResourceLocation(
            "hexcasting", ancient
                ? "textures/gui/scroll_ancient.png"
                : "textures/gui/scroll.png");
        minecraft.getTextureManager().bindTexture(background);
        Gui.drawScaledCustomSizeModalRect(
            x, y, 0.0F, 0.0F, 48, 48, 128, 128, 48.0F, 48.0F);
        GlStateManager.popMatrix();

        HexPatternChatGeometry.drawPreview(pattern, x, y, 128, 255,
            shiftAccent ? 0xFF763FA1 : 0xFFD2C8C8,
            shiftAccent ? 0xFFE7D0F9 : 0xFF554D54);
    }

    /** Draw the purple Shift overlay used by the original Hex item renderer. */
    public static void drawItemPatternOverlay(HexPattern pattern, int x, int y) {
        if (pattern == null) {
            return;
        }
        HexPatternChatGeometry.drawPreview(pattern, x + 1, y + 1, 16, 255,
            0xFF763FA1, 0xFFE7D0F9, false);
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(InlinePatternChatRenderer.class);
    }

    /** Capture the actual origin used by Forge's GuiIngameForge chat pass. */
    @SubscribeEvent
    public static void onChatOrigin(RenderGameOverlayEvent.Chat event) {
        if (event == null) {
            return;
        }
        chatOriginX = event.getPosX();
        chatOriginY = event.getPosY();
    }

    /** Draw after GuiNewChat so the glyph is above the chat background/text. */
    @SubscribeEvent
    public static void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event == null || event.getType() != ElementType.CHAT) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.fontRenderer == null
            || minecraft.ingameGUI == null) {
            return;
        }

        GuiNewChat chat = minecraft.ingameGUI.getChatGUI();
        List<ChatLine> drawnLines = getDrawnChatLines(chat);
        if (drawnLines == null || drawnLines.isEmpty()) {
            return;
        }

        List<TrackedMessage> messages;
        synchronized (InlinePatternChatRenderer.class) {
            if (trackedMessages.isEmpty()) {
                return;
            }
            messages = new ArrayList<>(trackedMessages);
        }

        int lineCount = chat.getLineCount();
        int scrollPos = getScrollPos(chat);
        boolean chatOpen = chat.getChatOpen();
        int updateCounter = minecraft.ingameGUI.getUpdateCounter();
        float opacity = minecraft.gameSettings.chatOpacity * 0.9F + 0.1F;
        // GuiIngameForge may let another mod move the chat widget. Use the
        // coordinates captured from RenderGameOverlayEvent.Chat instead of
        // reconstructing the vanilla default here.
        int chatX = chatOriginX;
        int chatY = chatOriginY;
        FontRenderer font = minecraft.fontRenderer;

        for (int visibleLine = 0;
             visibleLine < lineCount && visibleLine + scrollPos < drawnLines.size();
             visibleLine++) {
            ChatLine chatLine = drawnLines.get(visibleLine + scrollPos);
            if (chatLine == null) {
                continue;
            }

            int age = updateCounter - chatLine.getUpdatedCounter();
            if (age < 0) {
                age = 0;
            }
            if (!chatOpen && age >= 200) {
                continue;
            }

            int alpha = chatOpen ? 255 : fadeAlpha(age, opacity);
            if (alpha <= 3) {
                continue;
            }

            String lineText = chatLine.getChatComponent().getUnformattedText();
            for (TrackedMessage message : messages) {
                if (message.updateCounter >= 0
                    && message.updateCounter != chatLine.getUpdatedCounter()) {
                    continue;
                }
                for (PatternPlacement placement : message.placementsOn(lineText)) {
                    int x = font.getStringWidth(placement.prefix);
                    // Geometry's y correction is -16 px relative to the old
                    // pre-chat pass; subtracting 9 px here follows each
                    // wrapped/scrollable vanilla chat row.
                    HexPatternChatGeometry.draw(
                        placement.signature, chatX + x,
                        chatY - visibleLine * 9, alpha, placement.argb);
                }
            }
        }
    }

    private static int fadeAlpha(int age, float opacity) {
        double fade = 1.0D - (double) age / 200.0D;
        fade = Math.max(0.0D, Math.min(1.0D, fade * 10.0D));
        fade *= fade;
        return (int) (255.0D * fade * opacity);
    }

    @SuppressWarnings("unchecked")
    private static List<ChatLine> getDrawnChatLines(GuiNewChat chat) {
        try {
            return ObfuscationReflectionHelper.getPrivateValue(
                GuiNewChat.class, chat, "field_146253_i");
        } catch (RuntimeException ignored) {
            return Collections.emptyList();
        }
    }

    private static int getScrollPos(GuiNewChat chat) {
        try {
            Integer value = ObfuscationReflectionHelper.getPrivateValue(
                GuiNewChat.class, chat, "field_146250_j");
            return value == null ? 0 : Math.max(0, value);
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static final class TrackedMessage {
        private final String plainText;
        private final List<PatternToken> tokens;
        private final int updateCounter;
        @SuppressWarnings("unused")
        private final long sequence;

        private TrackedMessage(String plainText, List<PatternToken> tokens,
                               int updateCounter, long sequence) {
            this.plainText = plainText;
            this.tokens = tokens;
            this.updateCounter = updateCounter;
            this.sequence = sequence;
        }

        private List<PatternPlacement> placementsOn(String lineText) {
            if (lineText == null || lineText.isEmpty()) {
                return Collections.emptyList();
            }

            List<PatternPlacement> placements = new ArrayList<>();
            for (PatternToken token : tokens) {
                int lineStart = findLineContainingToken(lineText, token.plainStart);
                if (lineStart < 0) {
                    continue;
                }
                int prefixEnd = token.plainStart - lineStart;
                if (prefixEnd < 0 || prefixEnd > lineText.length()) {
                    continue;
                }
                placements.add(new PatternPlacement(
                    token.signature, lineText.substring(0, prefixEnd), token.argb));
            }
            return placements;
        }

        private int findLineContainingToken(String lineText, int tokenStart) {
            int searchFrom = 0;
            while (searchFrom <= plainText.length()) {
                int lineStart = plainText.indexOf(lineText, searchFrom);
                if (lineStart < 0) {
                    return -1;
                }
                if (tokenStart >= lineStart
                    && tokenStart < lineStart + lineText.length()) {
                    return lineStart;
                }
                searchFrom = lineStart + 1;
            }
            return -1;
        }
    }

    private static final class PatternToken {
        private final String signature;
        private final int argb;
        private final int plainStart;
        @SuppressWarnings("unused")
        private final int plainEnd;

        private PatternToken(String signature, int argb, int plainStart, int plainEnd) {
            this.signature = signature;
            this.argb = argb;
            this.plainStart = plainStart;
            this.plainEnd = plainEnd;
        }
    }

    private static final class PatternPlacement {
        private final String signature;
        private final String prefix;
        private final int argb;

        private PatternPlacement(String signature, String prefix, int argb) {
            this.signature = signature;
            this.prefix = prefix;
            this.argb = argb;
        }
    }

    private static int parseColor(String encoded) {
        if (encoded == null || encoded.length() != 8) {
            return 0xFFFFFFFF;
        }
        try {
            return (int) Long.parseLong(encoded, 16);
        } catch (NumberFormatException ignored) {
            return 0xFFFFFFFF;
        }
    }

    private static int parseColorOrDefault(String encoded, int fallback) {
        if (encoded == null || encoded.length() != 8) {
            return fallback;
        }
        return parseColor(encoded);
    }
}
