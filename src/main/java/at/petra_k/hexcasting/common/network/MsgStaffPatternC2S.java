package at.petra_k.hexcasting.common.network;

import at.petra_k.hexcasting.common.item.ItemHexStaff;
import at.petrak.paucal.api.PaucalMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;

/** Client-to-server result of drawing one registered pattern in the staff GUI. */
public final class MsgStaffPatternC2S implements PaucalMessage {
    private int handOrdinal;
    private String actionId;

    public MsgStaffPatternC2S() {
        this.handOrdinal = EnumHand.MAIN_HAND.ordinal();
        this.actionId = "";
    }

    public MsgStaffPatternC2S(EnumHand hand, ResourceLocation action) {
        this.handOrdinal = hand == null ? EnumHand.MAIN_HAND.ordinal() : hand.ordinal();
        this.actionId = action == null ? "" : action.toString();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        handOrdinal = buf.readByte();
        actionId = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(handOrdinal);
        ByteBufUtils.writeUTF8String(buf, actionId == null ? "" : actionId);
    }

    @Override
    public void handleMessage(EntityPlayer player) {
        handleMessage(player, Side.SERVER);
    }

    @Override
    public void handleMessage(EntityPlayer player, Side side) {
        if (side != Side.SERVER || player == null
            || handOrdinal < 0 || handOrdinal >= EnumHand.values().length) {
            return;
        }
        EnumHand hand = EnumHand.values()[handOrdinal];
        ItemStack staff = player.getHeldItem(hand);
        if (!ItemHexStaff.isStaff(staff)) {
            return;
        }
        if (actionId == null || actionId.isEmpty()) {
            ItemHexStaff.clearProgram(staff);
            return;
        }
        try {
            ItemHexStaff.appendAction(staff, new ResourceLocation(actionId));
        } catch (RuntimeException ignored) {
            // Invalid client data is rejected without changing the held item.
        }
    }

    public static void register() {
        at.petrak.paucal.api.PaucalAPI.registerMessage(MsgStaffPatternC2S.class, Side.SERVER);
    }
}
