package com.samsthenerd.inline.client;

import com.samsthenerd.inline.api.InlineAPI;
import com.samsthenerd.inline.api.MatchContext;
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
        event.setMessage(InlineAPI.formatChat(event.getMessage(),
            new MatchContext(minecraft.player, minecraft.world)));
    }
}
