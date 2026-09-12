package at.petrak.paucal.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;

/** Message contract shared by the embedded 1.12.2 Paucal network facade. */
public interface PaucalMessage extends IMessage {
    void handleMessage(EntityPlayer player);

    /** Handle a message on the game thread for either network side. */
    default void handleMessage(EntityPlayer player, Side side) {
        handleMessage(player);
    }
}
