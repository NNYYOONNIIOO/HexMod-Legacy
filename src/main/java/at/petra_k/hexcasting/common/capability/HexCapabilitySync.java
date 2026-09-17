package at.petra_k.hexcasting.common.capability;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.common.network.MsgCastingDataS2C;
import at.petrak.paucal.api.PaucalAPI;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

/** Server-side synchronization helpers for the persistent player capability. */
public final class HexCapabilitySync {
    private HexCapabilitySync() {
    }

    public static void send(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)
            || player.world == null || player.world.isRemote
            || HexCapabilities.CASTING_DATA == null) {
            return;
        }
        IHexCastingData data = player.getCapability(
            HexCapabilities.CASTING_DATA, null);
        if (data != null) {
            PaucalAPI.sendTo(new MsgCastingDataS2C(data), player);
        }
    }
}
