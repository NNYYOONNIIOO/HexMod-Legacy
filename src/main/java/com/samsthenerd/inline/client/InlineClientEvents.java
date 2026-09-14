package com.samsthenerd.inline.client;

import com.samsthenerd.inline.api.InlineAPI;
import com.samsthenerd.inline.api.MatchContext;
import at.petra_k.hexcasting.interop.inline.InlinePatternChatRenderer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Registers the 1.12.2 client-side bridge for Inline chat placeholders. */
public final class InlineClientEvents {
    private InlineClientEvents() {
    }

    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent event) {
        if (event == null || event.getMessage() == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        net.minecraft.util.text.ITextComponent formatted = InlineAPI.formatChat(
            event.getMessage(), new MatchContext(minecraft.player, minecraft.world));
        InlinePatternChatRenderer.capture(formatted);
        event.setMessage(InlinePatternChatRenderer.stripTokens(formatted));
    }

    /**
     * Kept as a public rendering seam for 1.12.2 GUI integrations. Text-only
     * chat cannot carry custom glyphs in this version, so callers that have a
     * concrete Inline payload may use these helpers during their own overlay
     * pass without depending on modern text mixins.
     */
    public static void renderItem(com.samsthenerd.inline.api.data.ItemInlineData data, int x, int y, float scale) {
        InlineBuiltins.renderItemGui(data, x, y, scale);
    }

    public static void renderEntity(com.samsthenerd.inline.api.data.EntityInlineData data, int x, int y, float scale) {
        InlineBuiltins.renderEntityGui(data, x, y, scale);
    }

    public static void renderPlayerHead(com.samsthenerd.inline.api.data.PlayerHeadData data, int x, int y, int size) {
        InlineBuiltins.renderPlayerHeadGui(data, x, y, size);
    }

    public static void renderModIcon(com.samsthenerd.inline.api.data.ModIconData data, int x, int y, int size) {
        InlineBuiltins.renderModIconGui(data, x, y, size);
    }
}
