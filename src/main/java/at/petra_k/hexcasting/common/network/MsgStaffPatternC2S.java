package at.petra_k.hexcasting.common.network;

import at.petra_k.hexcasting.common.item.ItemHexStaff;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import at.petrak.paucal.api.PaucalMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;

/** Client-to-server snapshot of the spell layout in the staff GUI. */
public final class MsgStaffPatternC2S implements PaucalMessage {
    private int handOrdinal;
    private NBTTagList patternsData;

    public MsgStaffPatternC2S() {
        this.handOrdinal = EnumHand.MAIN_HAND.ordinal();
        this.patternsData = null;
    }

    public MsgStaffPatternC2S(EnumHand hand, ResourceLocation action) {
        this.handOrdinal = hand == null ? EnumHand.MAIN_HAND.ordinal() : hand.ordinal();
        this.patternsData = null;
        if (action != null) {
            HexActionRegistry.bootstrap();
            HexPattern pattern = HexActionRegistry.getPattern(action);
            if (pattern != null) {
                this.patternsData = new NBTTagList();
                this.patternsData.appendTag(pattern.serializeToNBT());
            }
        }
    }

    public MsgStaffPatternC2S(EnumHand hand, HexPattern pattern, int originQ, int originR) {
        this.handOrdinal = hand == null ? EnumHand.MAIN_HAND.ordinal() : hand.ordinal();
        this.patternsData = null;
        if (pattern != null) {
            this.patternsData = new NBTTagList();
            NBTTagCompound patternData = pattern.serializeToNBT();
            patternData.setInteger("origin_q", originQ);
            patternData.setInteger("origin_r", originR);
            this.patternsData.appendTag(patternData);
        }
    }

    public MsgStaffPatternC2S(EnumHand hand, List<HexPattern> patterns,
                              List<Integer> originQ, List<Integer> originR) {
        this.handOrdinal = hand == null ? EnumHand.MAIN_HAND.ordinal() : hand.ordinal();
        this.patternsData = new NBTTagList();
        if (patterns == null) {
            return;
        }
        int limit = Math.min(patterns.size(), ItemHexStaff.MAX_PROGRAM_SIZE);
        for (int i = 0; i < limit; i++) {
            HexPattern pattern = patterns.get(i);
            if (pattern == null) {
                continue;
            }
            NBTTagCompound patternData = pattern.serializeToNBT();
            patternData.setInteger("origin_q", originQ != null && i < originQ.size()
                ? originQ.get(i) : 0);
            patternData.setInteger("origin_r", originR != null && i < originR.size()
                ? originR.get(i) : 0);
            this.patternsData.appendTag(patternData);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        handOrdinal = buf.readByte();
        if (buf.readBoolean()) {
            NBTTagCompound payload = ByteBufUtils.readTag(buf);
            patternsData = payload == null ? null : payload.getTagList("patterns", 10);
        } else {
            patternsData = null;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(handOrdinal);
        buf.writeBoolean(patternsData != null);
        if (patternsData != null) {
            NBTTagCompound payload = new NBTTagCompound();
            payload.setTag("patterns", patternsData);
            ByteBufUtils.writeTag(buf, payload);
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
        if (patternsData == null) {
            ItemHexStaff.clearProgram(staff);
            return;
        }
        ItemHexStaff.replaceProgram(staff, patternsData);
    }

    public static void register() {
        at.petrak.paucal.api.PaucalAPI.registerMessage(MsgStaffPatternC2S.class, Side.SERVER);
    }
}
