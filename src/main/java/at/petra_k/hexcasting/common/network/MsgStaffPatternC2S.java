package at.petra_k.hexcasting.common.network;

import at.petra_k.hexcasting.common.item.ItemHexStaff;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.casting.StaffCastExecutor;
import at.petra_k.hexcasting.common.casting.StaffPatternValidator;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import at.petrak.paucal.api.PaucalAPI;
import at.petrak.paucal.api.PaucalMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
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
            ItemHexStaff.clearProgram(player, hand, staff);
            sendAuthoritativeSnapshot(player, hand);
            return;
        }
        String validationError = StaffPatternValidator.validate(patternsData);
        if (validationError != null) {
            player.sendMessage(new TextComponentString(I18n.translateToLocal(validationError)));
            sendAuthoritativeSnapshot(player, hand);
            return;
        }
        NBTTagList previous = ItemHexStaff.getProgramSnapshot(staff);
        boolean appendOnly = isPrefix(previous, patternsData);
        ItemHexStaff.replaceProgram(player, hand, staff, patternsData);
        if (!appendOnly) {
            StaffCastExecutor.clear(staff);
        } else if (patternsData.tagCount() > previous.tagCount()) {
            for (int i = previous.tagCount(); i < patternsData.tagCount(); i++) {
                try {
                    StaffCastExecutor.execute(
                        player, hand, staff,
                        HexPattern.fromNBT(patternsData.getCompoundTagAt(i)));
                } catch (RuntimeException ignored) {
                    // ItemHexStaff already filters malformed snapshot entries.
                }
            }
        }
        sendAuthoritativeSnapshot(player, hand);
    }

    private static boolean isPrefix(NBTTagList previous, NBTTagList incoming) {
        if (previous == null || incoming == null || previous.tagCount() > incoming.tagCount()) {
            return false;
        }
        for (int i = 0; i < previous.tagCount(); i++) {
            try {
                HexPattern oldPattern = HexPattern.fromNBT(previous.getCompoundTagAt(i));
                HexPattern newPattern = HexPattern.fromNBT(incoming.getCompoundTagAt(i));
                if (!oldPattern.signature().equals(newPattern.signature())) {
                    return false;
                }
            } catch (RuntimeException ignored) {
                return false;
            }
        }
        return true;
    }

    private static void sendAuthoritativeSnapshot(EntityPlayer player, EnumHand hand) {
        PaucalAPI.sendTo(new MsgStaffProgramS2C(
            hand, ItemHexStaff.getProgramSnapshot(player.getHeldItem(hand))), player);
    }

    public static void register() {
        at.petrak.paucal.api.PaucalAPI.registerMessage(MsgStaffPatternC2S.class, Side.SERVER);
        MsgStaffProgramS2C.register();
    }
}
