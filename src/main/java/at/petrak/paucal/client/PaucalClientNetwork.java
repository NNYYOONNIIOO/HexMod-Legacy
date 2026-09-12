package at.petrak.paucal.client;

import at.petrak.paucal.api.PaucalMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;

/** Client-only main-thread bridge for messages received through Paucal. */
public final class PaucalClientNetwork {
    private PaucalClientNetwork() {
    }

    public static void dispatch(PaucalMessage message) {
        if (message == null) {
            return;
        }
        Minecraft.getMinecraft().addScheduledTask(() -> {
            EntityPlayer player = Minecraft.getMinecraft().player;
            message.handleMessage(player, Side.CLIENT);
        });
    }
}
