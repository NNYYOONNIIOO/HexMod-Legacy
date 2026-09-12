package com.samsthenerd.inline.client;

import com.samsthenerd.inline.api.InlineAPI;
import com.samsthenerd.inline.api.MatchContext;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Applies the common Inline matcher pipeline to 1.12.2 item tooltips. */
public final class InlineTooltipEvents {
    private InlineTooltipEvents() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (event == null || event.getToolTip() == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        MatchContext context = new MatchContext(minecraft.player, minecraft.world);
        for (int index = 0; index < event.getToolTip().size(); index++) {
            String line = event.getToolTip().get(index);
            event.getToolTip().set(index, InlineAPI.formatPlainText(line, context));
        }
    }
}

