package at.petra_k.hexcasting.common.network;

import at.petra_k.hexcasting.common.item.ItemHexStaff;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.casting.StaffCastExecutor;
import at.petra_k.hexcasting.common.casting.StaffPatternValidator;
import at.petra_k.hexcasting.common.effect.HexCastingEffects;
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
    private String staffInstanceId;
    private NBTTagList patternsData;

    public MsgStaffPatternC2S(EnumHand hand, String staffInstanceId) {
        this.handOrdinal = hand == null ? EnumHand.MAIN_HAND.ordinal() : hand.ordinal();
        this.staffInstanceId = staffInstanceId == null ? "" : staffInstanceId;
        this.patternsData = null;
    }

    public MsgStaffPatternC2S() {
        this.handOrdinal = EnumHand.MAIN_HAND.ordinal();
        this.staffInstanceId = "";
        this.patternsData = null;
    }

    public MsgStaffPatternC2S(EnumHand hand, ResourceLocation action) {
        this(hand, "", action);
    }

    public MsgStaffPatternC2S(EnumHand hand, String staffInstanceId,
                              ResourceLocation action) {
        this.handOrdinal = hand == null ? EnumHand.MAIN_HAND.ordinal() : hand.ordinal();
        this.staffInstanceId = staffInstanceId == null ? "" : staffInstanceId;
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
        this(hand, "", pattern, originQ, originR);
    }

    public MsgStaffPatternC2S(EnumHand hand, String staffInstanceId,
                              HexPattern pattern, int originQ, int originR) {
        this.handOrdinal = hand == null ? EnumHand.MAIN_HAND.ordinal() : hand.ordinal();
        this.staffInstanceId = staffInstanceId == null ? "" : staffInstanceId;
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
        this(hand, "", patterns, originQ, originR);
    }

    public MsgStaffPatternC2S(EnumHand hand, String staffInstanceId,
                              List<HexPattern> patterns,
                              List<Integer> originQ, List<Integer> originR) {
        this.handOrdinal = hand == null ? EnumHand.MAIN_HAND.ordinal() : hand.ordinal();
        this.staffInstanceId = staffInstanceId == null ? "" : staffInstanceId;
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

    /**
     * Send the already normalized staff snapshot, including per-pattern
     * resolution state.  The GUI uses this overload so appending one new
     * pattern cannot turn all older, resolved paths back into UNRESOLVED.
     */
    public MsgStaffPatternC2S(EnumHand hand, String staffInstanceId,
                              NBTTagList snapshot) {
        this.handOrdinal = hand == null ? EnumHand.MAIN_HAND.ordinal() : hand.ordinal();
        this.staffInstanceId = staffInstanceId == null ? "" : staffInstanceId;
        this.patternsData = snapshot == null ? new NBTTagList() : snapshot;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        handOrdinal = buf.readByte();
        boolean hasPatterns = buf.readBoolean();
        NBTTagCompound payload = ByteBufUtils.readTag(buf);
        staffInstanceId = payload == null ? "" : payload.getString("staff_id");
        patternsData = !hasPatterns || payload == null
            ? null : payload.getTagList("patterns", 10);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(handOrdinal);
        buf.writeBoolean(patternsData != null);
        NBTTagCompound payload = new NBTTagCompound();
        payload.setString("staff_id", staffInstanceId == null ? "" : staffInstanceId);
        if (patternsData != null) {
            payload.setTag("patterns", patternsData);
        }
        // Send the identity even for a clear request. Otherwise a delayed
        // clear packet could affect whichever other staff occupies this hand.
        ByteBufUtils.writeTag(buf, payload);
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
        String serverInstanceId = ItemHexStaff.ensureInstanceId(staff);
        if (staffInstanceId != null && !staffInstanceId.isEmpty()
            && !staffInstanceId.equals(serverInstanceId)) {
            // The player changed the held stack while a previous GUI packet
            // was in flight.  Never overwrite the newly held staff.
            sendAuthoritativeSnapshot(player, hand);
            return;
        }
        if (patternsData == null) {
            ItemHexStaff.clearProgram(player, hand, staff);
            HexCastingEffects.clearOrbitPatterns(player);
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
            HexCastingEffects.clearOrbitPatterns(player);
        } else if (patternsData.tagCount() > previous.tagCount()) {
            for (int i = previous.tagCount(); i < patternsData.tagCount(); i++) {
                try {
                    HexPattern pattern = HexPattern.fromNBT(patternsData.getCompoundTagAt(i));
                    StaffCastExecutor.CastOutcome outcome = StaffCastExecutor.executeDetailed(
                        player, hand, staff,
                        pattern);
                    ItemHexStaff.setProgramResolution(
                        staff, i, outcome.getResolution());
                    PaucalAPI.sendTo(new MsgStaffCastResultS2C(
                        hand, i, outcome), player);
                    HexCastingEffects.onStaffPattern(player, pattern, outcome);
                    if (outcome.isSuccess() && outcome.isStackClear()) {
                        StaffCastExecutor.clear(staff);
                        ItemHexStaff.clearProgram(player, hand, staff);
                        break;
                    }
                } catch (RuntimeException ignored) {
                    // ItemHexStaff already filters malformed snapshot entries.
                    ItemHexStaff.setProgramResolution(
                        staff, i, StaffCastExecutor.Resolution.ERRORED);
                    PaucalAPI.sendTo(new MsgStaffCastResultS2C(
                        hand, false, StaffCastExecutor.getStackSize(staff)), player);
                    HexCastingEffects.onStaffPattern(player, null,
                        null);
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
        ItemStack staff = player.getHeldItem(hand);
        PaucalAPI.sendTo(new MsgStaffProgramS2C(
            hand, player.world, staff, ItemHexStaff.getProgramSnapshot(staff)), player);
    }

    public static void register() {
        at.petrak.paucal.api.PaucalAPI.registerMessage(MsgStaffPatternC2S.class, Side.SERVER);
        MsgStaffProgramS2C.register();
        MsgStaffCastResultS2C.register();
        MsgCastingPatternS2C.register();
        MsgClearCastingPatternsS2C.register();
        MsgCastParticlesS2C.register();
        MsgPerWorldPatternsS2C.register();
    }
}
