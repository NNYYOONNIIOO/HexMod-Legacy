package at.petrak.paucal.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Message contract shared by the embedded 1.12.2 Paucal network facade. */
public interface PaucalMessage extends IMessage {
    void handleMessage(EntityPlayer player);
}
