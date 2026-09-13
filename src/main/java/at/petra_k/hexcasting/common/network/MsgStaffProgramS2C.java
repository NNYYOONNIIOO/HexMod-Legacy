package at.petra_k.hexcasting.common.network;

import at.petra_k.hexcasting.common.item.ItemHexStaff;
import at.petrak.paucal.api.PaucalAPI;
import at.petrak.paucal.api.PaucalMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;

/** Server snapshot used to refresh the client staff GUI after it opens. */
public final class MsgStaffProgramS2C implements PaucalMessage {
    private int handOrdinal;
    private String staffInstanceId;
    private NBTTagList patternsData;

    public MsgStaffProgramS2C() {
        this.handOrdinal = EnumHand.MAIN_HAND.ordinal();
        this.staffInstanceId = "";
        this.patternsData = new NBTTagList();
    }

    public MsgStaffProgramS2C(EnumHand hand, NBTTagList patterns) {
        this(hand, null, patterns);
    }

    public MsgStaffProgramS2C(EnumHand hand, ItemStack staff, NBTTagList patterns) {
        this.handOrdinal = hand == null ? EnumHand.MAIN_HAND.ordinal() : hand.ordinal();
        this.staffInstanceId = ItemHexStaff.getInstanceId(staff);
        this.patternsData = patterns == null ? new NBTTagList() : patterns;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        handOrdinal = buf.readByte();
        NBTTagCompound payload = ByteBufUtils.readTag(buf);
        staffInstanceId = payload == null ? "" : payload.getString("staff_id");
        patternsData = payload == null ? new NBTTagList() : payload.getTagList("patterns", 10);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(handOrdinal);
        NBTTagCompound payload = new NBTTagCompound();
        payload.setString("staff_id", staffInstanceId == null ? "" : staffInstanceId);
        payload.setTag("patterns", patternsData == null ? new NBTTagList() : patternsData);
        ByteBufUtils.writeTag(buf, payload);
    }

    @Override
    public void handleMessage(EntityPlayer player) {
        handleMessage(player, Side.CLIENT);
    }

    @Override
    public void handleMessage(EntityPlayer player, Side side) {
        if (side != Side.CLIENT || player == null
            || handOrdinal < 0 || handOrdinal >= EnumHand.values().length) {
            return;
        }
        EnumHand hand = EnumHand.values()[handOrdinal];
        ItemStack staff = player.getHeldItem(hand);
        if (!ItemHexStaff.isStaff(staff)) {
            return;
        }
        if (staffInstanceId != null && !staffInstanceId.isEmpty()) {
            String localInstanceId = ItemHexStaff.getInstanceId(staff);
            if (!localInstanceId.isEmpty() && !localInstanceId.equals(staffInstanceId)) {
                // This is a delayed response for a different physical staff.
                return;
            }
            ItemHexStaff.setInstanceId(staff, staffInstanceId);
        }
        ItemHexStaff.replaceProgram(staff, patternsData);
        try {
            Class<?> bridge = Class.forName(
                "at.petra_k.hexcasting.client.HexStaffClientSync");
            bridge.getMethod("refresh").invoke(null);
        } catch (ReflectiveOperationException ignored) {
            // The client-only bridge is intentionally absent on a dedicated server.
        }
    }

    public static void register() {
        PaucalAPI.registerMessage(MsgStaffProgramS2C.class, Side.CLIENT);
    }
}
