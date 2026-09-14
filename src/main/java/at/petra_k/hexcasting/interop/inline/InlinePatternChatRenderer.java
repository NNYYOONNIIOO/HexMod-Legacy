package at.petra_k.hexcasting.interop.inline;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client-side renderer for pattern tokens embedded in chat text.
 *
 * <p>1.12.2 has no modern font provider/component API.  We therefore keep
 * the message text as an invisible-width token and draw the decoded Hex
 * geometry at the token's measured chat position during the chat overlay.
 * This produces a real line/dot diagram rather than Unicode box characters.</p>
 */
public final class InlinePatternChatRenderer {
    private static final String TOKEN_PREFIX = "\uE000hexcasting:pattern:";
    private static final String TOKEN_SUFFIX = "\uE001";
    private static final Pattern TOKEN = Pattern.compile(
        Pattern.quote(TOKEN_PREFIX) + "([a-z]+)" + Pattern.quote(TOKEN_SUFFIX));
    private static volatile String lastMessage;

    private InlinePatternChatRenderer() {
    }

    public static String token(String signature) {
        return TOKEN_PREFIX + signature + TOKEN_SUFFIX;
    }

    public static void capture(ITextComponent message) {
        String text = message == null ? null : message.getUnformattedText();
        lastMessage = text != null && text.indexOf(TOKEN_PREFIX) >= 0 ? text : null;
    }

    /** Replace the private token with a fixed-width blank in vanilla chat. */
    public static ITextComponent stripTokens(ITextComponent message) {
        if (message == null) {
            return null;
        }
        TextComponentString result = new TextComponentString(
            stripTokenText(message.getUnformattedText()));
        result.setStyle(message.getStyle());
        return result;
    }

    private static String stripTokenText(String text) {
        return text == null ? "" : TOKEN.matcher(text).replaceAll("    ");
    }

    public static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(InlinePatternChatRenderer.class);
    }

    @SubscribeEvent
    public static void onOverlay(RenderGameOverlayEvent.Chat event) {
        String message = lastMessage;
        if (message == null || message.indexOf(TOKEN_PREFIX) < 0) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.fontRenderer == null) {
            return;
        }
        Matcher matcher = TOKEN.matcher(message);
        while (matcher.find()) {
            String prefix = stripTokenText(message.substring(0, matcher.start()));
            int x = event.getPosX() + mc.fontRenderer.getStringWidth(prefix);
            drawPattern(mc.fontRenderer, matcher.group(1), x, event.getPosY());
        }
    }

    private static void drawPattern(FontRenderer font, String signature, int x, int y) {
        // Pattern rendering is deliberately delegated to the same compact
        // geometry helper used by the staff screen.  This method is a client
        // seam; the chat event supplies the translated screen origin.
        HexPatternChatGeometry.draw(signature, x, y);
    }
}
