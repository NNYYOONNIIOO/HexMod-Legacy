package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.interop.inline.HexInline;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import at.petra_k.hexcasting.common.casting.HexEvaluator;
import at.petra_k.hexcasting.common.casting.SpecialPatternResolver;
import at.petra_k.hexcasting.common.casting.StaffCastExecutor;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import at.petra_k.hexcasting.common.network.MsgStaffProgramS2C;
import at.petra_k.hexcasting.common.network.MsgPerWorldPatternsS2C;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** A programmable Hex Casting staff for the first 1.12.2 casting slice. */
public final class ItemHexStaff extends Item {
    public static final int MAX_PROGRAM_SIZE = 64;
    private final String variant;
    private static final String KEY_PROGRAM = "program";
    private static final String KEY_PATTERN_PROGRAM = "patterns";
    private static final String KEY_ORIGIN_Q = "origin_q";
    private static final String KEY_ORIGIN_R = "origin_r";
    /** Per-pattern client rendering state, matching Hex's ResolvedPattern.Valid field. */
    public static final String KEY_RESOLUTION = "resolution";
    private static final String KEY_CASTING_STATE = "casting_state";
    private static final String KEY_INSTANCE_ID = "staff_instance_id";

    public ItemHexStaff() {
        this("");
    }

    public ItemHexStaff(String variant) {
        this.variant = variant == null ? "" : variant;
        setMaxStackSize(1);
    }

    public String getVariant() {
        return variant;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack staff = player.getHeldItem(hand);
        if (world.isRemote) {
            // The server clears the authoritative program below. Clear the
            // client copy first so the GUI is opened only after the old
            // program has disappeared from the visible hand stack.
            if (player.isSneaking()) {
                clearProgram(player, hand, staff);
            }
            openStaffGui(hand);
            return new ActionResult<>(EnumActionResult.SUCCESS, staff);
        }

        // A packet must be tied to this physical stack, not just to the hand
        // that currently holds it.  The id is deliberately kept when the
        // program is cleared, so clearing one staff can never alias another.
        ensureInstanceId(staff);
        if (player.isSneaking()) {
            clearProgram(player, hand, staff);
            player.sendMessage(new TextComponentString(
                I18n.translateToLocal("hexcasting.message.program_cleared")));
        }

        if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            at.petrak.paucal.api.PaucalAPI.sendTo(
                new MsgPerWorldPatternsS2C(player.world),
                (net.minecraft.entity.player.EntityPlayerMP) player);
            at.petrak.paucal.api.PaucalAPI.sendTo(
                new MsgStaffProgramS2C(hand, player.world, staff,
                    getProgramSnapshot(staff)), player);
        }

        // Modern Hex opens the spellcasting screen here. The saved pattern
        // list is edited and evaluated through the spellcasting state as each
        // pattern is drawn; right-clicking must not replay the whole list as a
        // fresh cast (which loses the VM stack and causes false failures).
        return new ActionResult<>(EnumActionResult.SUCCESS, staff);
    }

    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    private static void openStaffGui(EnumHand hand) {
        net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(
            new at.petra_k.hexcasting.client.GuiHexStaff(hand));
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        HexActionRegistry.bootstrap();
        List<ProgramEntry> entries = getProgramEntries(stack);
        tooltip.add(I18n.translateToLocalFormatted(
            "hexcasting.tooltip.staff_program", entries.size(), MAX_PROGRAM_SIZE));
        int shown = Math.min(entries.size(), 8);
        for (int i = 0; i < shown; i++) {
            tooltip.add(I18n.translateToLocalFormatted(
                "hexcasting.tooltip.staff_entry", i + 1,
                describeProgramEntry(entries.get(i))));
        }
        if (entries.size() > shown) {
            tooltip.add(I18n.translateToLocalFormatted(
                "hexcasting.tooltip.staff_more", entries.size() - shown));
        }
    }

    private static String describeProgramEntry(ProgramEntry entry) {
        if (entry == null || entry.getPattern() == null) {
            return I18n.translateToLocal("hexcasting.tooltip.pattern");
        }
        // A staff tooltip is a compact view of the same drawing state as the
        // grid. Keep the action's glyph instead of replacing it with its
        // localized name, and carry the persisted resolution colour through
        // the inline token so the tooltip does not turn every old path white.
        return HexInline.formatPattern(entry.getPattern(),
            resolutionColor(entry.getResolutionOrdinal()));
    }

    private static int resolutionColor(int ordinal) {
        StaffCastExecutor.Resolution[] values = StaffCastExecutor.Resolution.values();
        if (ordinal < 0 || ordinal >= values.length) {
            ordinal = StaffCastExecutor.Resolution.UNRESOLVED.ordinal();
        }
        return values[ordinal].getColor();
    }

    public static boolean isStaff(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemHexStaff;
    }

    /**
     * Returns the persistent identity of this physical staff stack.
     *
     * <p>The program itself is still stored in the stack's NBT.  This second
     * value exists so delayed client/server packets cannot apply that NBT to
     * whichever other staff happens to occupy the same hand later.</p>
     */
    public static String getInstanceId(ItemStack staff) {
        if (!isStaff(staff) || staff.getTagCompound() == null) {
            return "";
        }
        String instanceId = staff.getTagCompound().getString(KEY_INSTANCE_ID);
        return isValidInstanceId(instanceId) ? instanceId : "";
    }

    /** Assign an id once, without touching the staff's program. */
    public static String ensureInstanceId(ItemStack staff) {
        if (!isStaff(staff)) {
            return "";
        }
        String existing = getInstanceId(staff);
        if (!existing.isEmpty()) {
            return existing;
        }
        String generated = UUID.randomUUID().toString();
        getOrCreateTag(staff).setString(KEY_INSTANCE_ID, generated);
        return generated;
    }

    /** Apply a server-authoritative id to a client stack during synchronization. */
    public static void setInstanceId(ItemStack staff, String instanceId) {
        if (!isStaff(staff) || !isValidInstanceId(instanceId)) {
            return;
        }
        getOrCreateTag(staff).setString(KEY_INSTANCE_ID, instanceId);
    }

    public static boolean appendAction(ItemStack staff, ResourceLocation action) {
        if (!isStaff(staff) || action == null) {
            return false;
        }
        HexActionRegistry.bootstrap();
        return appendPattern(staff, HexActionRegistry.getPattern(action), 0, 0);
    }

    /** Stores the drawable pattern and its screen-space origin, as Hex does. */
    public static boolean appendPattern(ItemStack staff, HexPattern pattern,
                                        int originQ, int originR) {
        if (!isStaff(staff) || pattern == null) {
            return false;
        }
        HexActionRegistry.bootstrap();
        if (getProgramSize(staff) >= MAX_PROGRAM_SIZE) {
            return false;
        }
        NBTTagCompound tag = getOrCreateTag(staff);
        NBTTagList patterns = tag.getTagList(KEY_PATTERN_PROGRAM, 10);
        NBTTagCompound entry = pattern.serializeToNBT();
        entry.setInteger(KEY_ORIGIN_Q, originQ);
        entry.setInteger(KEY_ORIGIN_R, originR);
        entry.setInteger(KEY_RESOLUTION,
            StaffCastExecutor.Resolution.UNRESOLVED.ordinal());
        patterns.appendTag(entry);
        tag.setTag(KEY_PATTERN_PROGRAM, patterns);
        return true;
    }

    /** Replace the complete spell layout sent by the staff GUI. */
    public static void replaceProgram(ItemStack staff, NBTTagList incoming) {
        if (!isStaff(staff)) {
            return;
        }
        // Older clients and a few legacy callers submit only the pattern and
        // origin fields.  Preserve the old resolution for an unchanged
        // prefix in that case; otherwise a harmless append would repaint all
        // previous paths as gray on the next authoritative sync.
        NBTTagList previous = null;
        if (staff.getTagCompound() != null
            && staff.getTagCompound().hasKey(KEY_PATTERN_PROGRAM, 9)) {
            previous = staff.getTagCompound().getTagList(KEY_PATTERN_PROGRAM, 10);
        }
        NBTTagList normalized = new NBTTagList();
        if (incoming != null) {
            int limit = Math.min(incoming.tagCount(), MAX_PROGRAM_SIZE);
            for (int i = 0; i < limit; i++) {
                try {
                    NBTTagCompound raw = incoming.getCompoundTagAt(i);
                    HexPattern pattern = HexPattern.fromNBT(raw);
                    NBTTagCompound entry = pattern.serializeToNBT();
                    entry.setInteger(KEY_ORIGIN_Q, raw.getInteger(KEY_ORIGIN_Q));
                    entry.setInteger(KEY_ORIGIN_R, raw.getInteger(KEY_ORIGIN_R));
                    int resolution = StaffCastExecutor.Resolution.UNRESOLVED.ordinal();
                    if (raw.hasKey(KEY_RESOLUTION, 3)) {
                        resolution = normalizeResolution(raw.getInteger(KEY_RESOLUTION));
                    } else if (previous != null && i < previous.tagCount()) {
                        try {
                            NBTTagCompound old = previous.getCompoundTagAt(i);
                            HexPattern oldPattern = HexPattern.fromNBT(old);
                            if (oldPattern.signature().equals(pattern.signature())) {
                                resolution = normalizeResolution(old.getInteger(KEY_RESOLUTION));
                            }
                        } catch (RuntimeException ignored) {
                            // A malformed old entry must not reject the new snapshot.
                        }
                    }
                    entry.setInteger(KEY_RESOLUTION, resolution);
                    normalized.appendTag(entry);
                } catch (RuntimeException ignored) {
                    // Ignore only malformed entries in the submitted snapshot.
                }
            }
        }
        NBTTagCompound tag = getOrCreateTag(staff);
        tag.removeTag(KEY_PROGRAM);
        tag.setTag(KEY_PATTERN_PROGRAM, normalized);
    }

    /** Update both the player-scoped program and the client compatibility cache. */
    public static void replaceProgram(EntityPlayer player, ItemStack staff,
                                      NBTTagList incoming) {
        replaceProgram(staff, incoming);
    }

    public static void replaceProgram(EntityPlayer player, EnumHand hand,
                                      ItemStack staff, NBTTagList incoming) {
        replaceProgram(staff, incoming);
    }

    public static void clearProgram(ItemStack staff) {
        if (!isStaff(staff)) {
            return;
        }
        NBTTagCompound tag = staff.getTagCompound();
        if (tag != null) {
            tag.removeTag(KEY_PROGRAM);
            tag.removeTag(KEY_PATTERN_PROGRAM);
            tag.removeTag(KEY_CASTING_STATE);
        }
    }

    /** Persist the last server-side resolution state for one drawn pattern. */
    public static void setProgramResolution(ItemStack staff, int index,
                                             StaffCastExecutor.Resolution resolution) {
        if (!isStaff(staff) || index < 0 || resolution == null
            || staff.getTagCompound() == null) {
            return;
        }
        NBTTagCompound tag = staff.getTagCompound();
        if (!tag.hasKey(KEY_PATTERN_PROGRAM, 9)) {
            return;
        }
        NBTTagList patterns = tag.getTagList(KEY_PATTERN_PROGRAM, 10);
        if (index >= patterns.tagCount()) {
            return;
        }
        NBTTagCompound entry = patterns.getCompoundTagAt(index);
        entry.setInteger(KEY_RESOLUTION, normalizeResolution(resolution.ordinal()));
    }

    public static void clearProgram(EntityPlayer player, ItemStack staff) {
        clearProgram(player, EnumHand.MAIN_HAND, staff);
    }

    public static void clearProgram(EntityPlayer player, EnumHand hand, ItemStack staff) {
        clearProgram(staff);
    }

    public static int getProgramSize(ItemStack staff) {
        return getProgramEntries(staff).size();
    }

    public static List<ResourceLocation> getProgramIds(ItemStack staff) {
        List<ResourceLocation> result = new ArrayList<>();
        for (ProgramEntry entry : getProgramEntries(staff)) {
            if (entry.getActionId() != null) {
                result.add(entry.getActionId());
            }
        }
        return result;
    }

    public static List<HexPattern> getProgramPatterns(ItemStack staff) {
        List<HexPattern> result = new ArrayList<>();
        for (ProgramEntry entry : getProgramEntries(staff)) {
            result.add(entry.getPattern());
        }
        return result;
    }

    public static List<HexPattern> getProgramPatterns(EntityPlayer player, ItemStack staff) {
        return getProgramPatterns(player, EnumHand.MAIN_HAND, staff);
    }

    public static List<HexPattern> getProgramPatterns(EntityPlayer player, EnumHand hand,
                                                      ItemStack staff) {
        return getProgramPatterns(staff);
    }

    /** Read the player-side program first, falling back to legacy item NBT. */
    public static List<ProgramEntry> getProgramEntries(EntityPlayer player, ItemStack staff) {
        return getProgramEntries(player, EnumHand.MAIN_HAND, staff);
    }

    public static List<ProgramEntry> getProgramEntries(EntityPlayer player, EnumHand hand,
                                                       ItemStack staff) {
        if (!isStaff(staff) || staff.getTagCompound() == null) {
            return Collections.emptyList();
        }
        HexActionRegistry.bootstrap();
        NBTTagCompound tag = staff.getTagCompound();
        if (tag.hasKey(KEY_PATTERN_PROGRAM)) {
            return getPatternEntries(tag.getTagList(KEY_PATTERN_PROGRAM, 10),
                player == null ? null : player.world);
        }
        return getProgramEntries(staff);
    }

    /** Returns a detached, normalized snapshot for network synchronization. */
    public static NBTTagList getProgramSnapshot(ItemStack staff) {
        NBTTagList snapshot = new NBTTagList();
        for (ProgramEntry entry : getProgramEntries(staff)) {
            NBTTagCompound data = entry.getPattern().serializeToNBT();
            data.setInteger(KEY_ORIGIN_Q, entry.getOriginQ());
            data.setInteger(KEY_ORIGIN_R, entry.getOriginR());
            data.setInteger(KEY_RESOLUTION, entry.getResolutionOrdinal());
            snapshot.appendTag(data);
        }
        return snapshot;
    }

    /** Returns the pattern, action id, and origin needed to reconstruct the GUI. */
    public static List<ProgramEntry> getProgramEntries(ItemStack staff) {
        if (!isStaff(staff) || staff.getTagCompound() == null) {
            return Collections.emptyList();
        }
        HexActionRegistry.bootstrap();
        NBTTagCompound tag = staff.getTagCompound();
        List<ProgramEntry> result = new ArrayList<>();
        if (tag.hasKey(KEY_PATTERN_PROGRAM)) {
            return getPatternEntries(tag.getTagList(KEY_PATTERN_PROGRAM, 10));
        }

        // Read the pre-pattern-format string list created by older builds.
        NBTTagList legacy = tag.getTagList(KEY_PROGRAM, 8);
        int legacyOriginQ = 0;
        for (int i = 0; i < legacy.tagCount(); i++) {
            try {
                ResourceLocation actionId = new ResourceLocation(legacy.getStringTagAt(i));
                HexPattern pattern = HexActionRegistry.getPattern(actionId);
                if (pattern != null) {
                    result.add(new ProgramEntry(pattern, actionId, legacyOriginQ, 0,
                        StaffCastExecutor.Resolution.UNRESOLVED.ordinal()));
                    legacyOriginQ += 4;
                }
            } catch (RuntimeException ignored) {
                // Invalid old data is ignored while the remaining program is preserved.
            }
        }
        return result;
    }

    private static List<ProgramEntry> getPatternEntries(NBTTagList patterns) {
        return getPatternEntries(patterns, null);
    }

    private static List<ProgramEntry> getPatternEntries(
        NBTTagList patterns, net.minecraft.world.World world) {
        HexActionRegistry.bootstrap();
        List<ProgramEntry> result = new ArrayList<>();
        for (int i = 0; i < patterns.tagCount(); i++) {
            try {
                NBTTagCompound entry = patterns.getCompoundTagAt(i);
                HexPattern pattern = HexPattern.fromNBT(entry);
                HexAction action = HexActionRegistry.get(pattern, world);
                ResourceLocation actionId = action == null
                    ? null : HexActionRegistry.idFor(action);
                result.add(new ProgramEntry(pattern, actionId,
                    entry.getInteger(KEY_ORIGIN_Q), entry.getInteger(KEY_ORIGIN_R),
                    normalizeResolution(entry.getInteger(KEY_RESOLUTION))));
            } catch (RuntimeException ignored) {
                // Ignore malformed entries without discarding the rest of the program.
            }
        }
        return result;
    }

    public static final class ProgramEntry {
        private final HexPattern pattern;
        private final ResourceLocation actionId;
        private final int originQ;
        private final int originR;
        private final int resolutionOrdinal;

        private ProgramEntry(HexPattern pattern, ResourceLocation actionId,
                             int originQ, int originR, int resolutionOrdinal) {
            this.pattern = pattern;
            this.actionId = actionId;
            this.originQ = originQ;
            this.originR = originR;
            this.resolutionOrdinal = normalizeResolution(resolutionOrdinal);
        }

        public HexPattern getPattern() {
            return pattern;
        }

        public ResourceLocation getActionId() {
            return actionId;
        }

        public int getOriginQ() {
            return originQ;
        }

        public int getOriginR() {
            return originR;
        }

        public int getResolutionOrdinal() {
            return resolutionOrdinal;
        }
    }

    private static int normalizeResolution(int ordinal) {
        StaffCastExecutor.Resolution[] values = StaffCastExecutor.Resolution.values();
        return ordinal < 0 || ordinal >= values.length
            ? StaffCastExecutor.Resolution.UNRESOLVED.ordinal() : ordinal;
    }

    private static boolean isPatternScroll(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemPatternScroll;
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        return tag;
    }

    private static boolean isValidInstanceId(String instanceId) {
        if (instanceId == null || instanceId.length() > 64) {
            return false;
        }
        try {
            UUID.fromString(instanceId);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static String localizeError(String message) {
        if (message == null || message.isEmpty()) {
            return I18n.translateToLocal("hexcasting.message.staff_error");
        }
        String lower = message.toLowerCase();
        String marker = "no action is registered for pattern";
        int markerIndex = lower.indexOf(marker);
        if (markerIndex >= 0) {
            String signature = message.substring(markerIndex + marker.length()).trim();
            try {
                signature = HexInline.formatPattern(HexPattern.fromSignature(signature));
            } catch (IllegalArgumentException ignored) {
                // Keep compatibility with older saved/error messages.
            }
            return I18n.translateToLocalFormatted(
                "hexcasting.message.pattern_unregistered", signature);
        }
        String translated = I18n.translateToLocal(message);
        return message.equals(translated) ? message : translated;
    }

    private static String localizeAction(ResourceLocation id) {
        String key = "hexcasting.action." + id.getResourcePath();
        String translated = I18n.translateToLocal(key);
        return key.equals(translated) ? id.getResourcePath() : translated;
    }
}
