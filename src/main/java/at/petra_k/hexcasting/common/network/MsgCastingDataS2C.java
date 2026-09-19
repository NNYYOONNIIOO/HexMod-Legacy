package at.petra_k.hexcasting.common.network;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import at.petrak.paucal.api.PaucalAPI;
import at.petrak.paucal.api.PaucalMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;

/** Authoritative player capability snapshot sent to the logical client. */
public final class MsgCastingDataS2C implements PaucalMessage {
    private NBTTagCompound data;

    public MsgCastingDataS2C() {
        data = new NBTTagCompound();
    }

    public MsgCastingDataS2C(IHexCastingData source) {
        data = source == null ? new NBTTagCompound() : source.serializeNBT();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        NBTTagCompound decoded = ByteBufUtils.readTag(buf);
        data = decoded == null ? new NBTTagCompound() : decoded;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, data == null ? new NBTTagCompound() : data);
    }

    @Override
    public void handleMessage(EntityPlayer player) {
        handleMessage(player, Side.CLIENT);
    }

    @Override
    public void handleMessage(EntityPlayer player, Side side) {
        if (side != Side.CLIENT || player == null
            || HexCapabilities.CASTING_DATA == null) {
            return;
        }
        IHexCastingData target = player.getCapability(
            HexCapabilities.CASTING_DATA, null);
        if (target != null) {
            target.deserializeNBT(data == null ? new NBTTagCompound() : data);
            // The orbit renderer may already have restored staff patterns
            // before this authoritative capability snapshot arrives.  Push
            // the same source into its client cache so existing orbits are
            // recolored immediately instead of waiting for a new cast.
            try {
                Class<?> bridge = Class.forName(
                    "at.petra_k.hexcasting.client.HexClientEffects");
                bridge.getMethod("updatePlayerPigment",
                    EntityPlayer.class, IHexCastingData.class)
                    .invoke(null, player, target);
            } catch (ReflectiveOperationException ignored) {
                // Client-only rendering is optional on a dedicated server.
            }
        }
    }

    public static void register() {
        PaucalAPI.registerMessage(MsgCastingDataS2C.class, Side.CLIENT);
    }
}
