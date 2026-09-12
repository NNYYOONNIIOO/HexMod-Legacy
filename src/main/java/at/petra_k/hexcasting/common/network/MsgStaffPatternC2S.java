package at.petra_k.hexcasting.common.network;

import at.petra_k.hexcasting.common.item.ItemHexStaff;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import at.petrak.paucal.api.PaucalMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;

/** Client-to-server result of drawing one registered pattern in the staff GUI. */
public final class MsgStaffPatternC2S implements PaucalMessage {
    private int handOrdinal;
    private NBTTagCompound patternData;

    public MsgStaffPatternC2S() {
        this.handOrdinal = EnumHand.MAIN_HAND.ordinal();
        this.patternData = null;
    }

    public MsgStaffPatternC2S(EnumHand hand, ResourceLocation action) {
        this.handOrdinal = hand == null ? EnumHand.MAIN_HAND.ordinal() : hand.ordinal();
        this.patternData = null;
        if (action != null) {
            HexActionRegistry.bootstrap();
            HexPattern pattern = HexActionRegistry.getPattern(action);
            if (pattern != null) {
                this.patternData = pattern.serializeToNBT();
            }
        }
    }

    public MsgStaffPatternC2S(EnumHand hand, HexPattern pattern, int originQ, int originR) {
        this.handOrdinal = hand == null ? EnumHand.MAIN_HAND.ordinal() : hand.ordinal();
        this.patternData = pattern == null ? null : pattern.serializeToNBT();
        if (this.patternData != null) {
            this.patternData.setInteger("origin_q", originQ);
            this.patternData.setInteger("origin_r", originR);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        handOrdinal = buf.readByte();
        patternData = buf.readBoolean() ? ByteBufUtils.readTag(buf) : null;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(handOrdinal);
        buf.writeBoolean(patternData != null);
        if (patternData != null) {
            ByteBufUtils.writeTag(buf, patternData);
        }
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
        if (patternData == null) {
            ItemHexStaff.clearProgram(staff);
            return;
        }
        try {
            HexPattern pattern = HexPattern.fromNBT(patternData);
            ItemHexStaff.appendPattern(staff, pattern,
                patternData.getInteger("origin_q"), patternData.getInteger("origin_r"));
        } catch (RuntimeException ignored) {
            // Invalid client data is rejected without changing the held item.
        }
    }

    public static void register() {
        at.petrak.paucal.api.PaucalAPI.registerMessage(MsgStaffPatternC2S.class, Side.SERVER);
    }
}
