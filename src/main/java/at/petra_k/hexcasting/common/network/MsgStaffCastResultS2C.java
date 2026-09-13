package at.petra_k.hexcasting.common.network;

import at.petrak.paucal.api.PaucalAPI;
import at.petrak.paucal.api.PaucalMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.relauncher.Side;

/** Client feedback for one server-side staff VM step. */
public final class MsgStaffCastResultS2C implements PaucalMessage {
    private int handOrdinal;
    private boolean success;
    private int stackSize;

    public MsgStaffCastResultS2C() {
        handOrdinal = EnumHand.MAIN_HAND.ordinal();
    }

    public MsgStaffCastResultS2C(EnumHand hand, boolean success, int stackSize) {
        handOrdinal = hand == null ? EnumHand.MAIN_HAND.ordinal() : hand.ordinal();
        this.success = success;
        this.stackSize = Math.max(0, stackSize);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        handOrdinal = buf.readByte();
        success = buf.readBoolean();
        stackSize = Math.max(0, buf.readInt());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(handOrdinal);
        buf.writeBoolean(success);
        buf.writeInt(stackSize);
    }

    @Override
    public void handleMessage(EntityPlayer player) {
        handleMessage(player, Side.CLIENT);
    }

    @Override
    public void handleMessage(EntityPlayer player, Side side) {
        if (side != Side.CLIENT || player == null) {
            return;
        }
        String message = success
            ? I18n.translateToLocalFormatted(
                "hexcasting.message.program_result", stackSize)
            : I18n.translateToLocal("hexcasting.message.cast_failed");
        try {
            Class<?> bridge = Class.forName(
                "at.petra_k.hexcasting.client.HexStaffClientSync");
            bridge.getMethod("showCastResult", String.class).invoke(null, message);
        } catch (ReflectiveOperationException ignored) {
            // The client-only bridge is intentionally absent on a dedicated server.
        }
    }

    public static void register() {
        PaucalAPI.registerMessage(MsgStaffCastResultS2C.class, Side.CLIENT);
    }
}
