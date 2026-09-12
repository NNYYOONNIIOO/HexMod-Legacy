package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import at.petra_k.hexcasting.common.casting.HexEvaluator;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
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

/** A programmable Hex Casting staff for the first 1.12.2 casting slice. */
public final class ItemHexStaff extends Item {
    public static final int MAX_PROGRAM_SIZE = 64;
    private static final String KEY_PROGRAM = "program";
    private static final String KEY_PATTERN_PROGRAM = "patterns";
    private static final String KEY_ORIGIN_Q = "origin_q";
    private static final String KEY_ORIGIN_R = "origin_r";

    public ItemHexStaff() {
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack staff = player.getHeldItem(hand);
        if (world.isRemote) {
            // The server clears the authoritative program below. Clear the
            // client copy first so the GUI is opened only after the old
            // program has disappeared from the visible hand stack.
            if (player.isSneaking()) {
                clearProgram(staff);
            }
            openStaffGui(hand);
            return new ActionResult<>(EnumActionResult.SUCCESS, staff);
        }

        HexActionRegistry.bootstrap();
        if (player.isSneaking()) {
            clearProgram(staff);
            player.sendMessage(new TextComponentString(
                I18n.translateToLocal("hexcasting.message.program_cleared")));
            return new ActionResult<>(EnumActionResult.SUCCESS, staff);
        }

        List<HexPattern> program = getProgramPatterns(staff);
        if (program.isEmpty()) {
            player.sendMessage(new TextComponentString(
                I18n.translateToLocal("hexcasting.message.program_empty")));
            return new ActionResult<>(EnumActionResult.SUCCESS, staff);
        }

        try {
            CastingStack result = new CastingStack();
            IHexCastingData data = HexCapabilities.CASTING_DATA == null
                ? null
                : player.getCapability(HexCapabilities.CASTING_DATA, null);
            HexEvaluator.evaluate(program, result, data, player);
            String resultText = result.isEmpty()
                ? I18n.translateToLocal("hexcasting.message.empty_stack")
                : result.peek().display();
            player.sendMessage(new TextComponentString(
                I18n.translateToLocalFormatted(
                    "hexcasting.message.program_result", resultText)));
        } catch (CastingException exception) {
            player.sendMessage(new TextComponentString(
                I18n.translateToLocalFormatted(
                    "hexcasting.message.staff_error", localizeError(exception.getMessage()))));
        }
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
        List<ResourceLocation> actions = getProgramIds(stack);
        tooltip.add(I18n.translateToLocalFormatted(
            "hexcasting.tooltip.staff_program", actions.size(), MAX_PROGRAM_SIZE));
        int shown = Math.min(actions.size(), 8);
        for (int i = 0; i < shown; i++) {
            tooltip.add(I18n.translateToLocalFormatted(
                "hexcasting.tooltip.staff_entry", i + 1, localizeAction(actions.get(i))));
        }
        if (actions.size() > shown) {
            tooltip.add(I18n.translateToLocalFormatted(
                "hexcasting.tooltip.staff_more", actions.size() - shown));
        }
    }

    public static boolean isStaff(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemHexStaff;
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
        patterns.appendTag(entry);
        tag.setTag(KEY_PATTERN_PROGRAM, patterns);
        return true;
    }

    /** Replace the complete spell layout sent by the staff GUI. */
    public static void replaceProgram(ItemStack staff, NBTTagList incoming) {
        if (!isStaff(staff)) {
            return;
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

    public static void clearProgram(ItemStack staff) {
        if (!isStaff(staff)) {
            return;
        }
        NBTTagCompound tag = staff.getTagCompound();
        if (tag != null) {
            tag.removeTag(KEY_PROGRAM);
            tag.removeTag(KEY_PATTERN_PROGRAM);
        }
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

    /** Returns the pattern, action id, and origin needed to reconstruct the GUI. */
    public static List<ProgramEntry> getProgramEntries(ItemStack staff) {
        if (!isStaff(staff) || staff.getTagCompound() == null) {
            return Collections.emptyList();
        }
        HexActionRegistry.bootstrap();
        NBTTagCompound tag = staff.getTagCompound();
        List<ProgramEntry> result = new ArrayList<>();
        if (tag.hasKey(KEY_PATTERN_PROGRAM)) {
            NBTTagList patterns = tag.getTagList(KEY_PATTERN_PROGRAM, 10);
            for (int i = 0; i < patterns.tagCount(); i++) {
                try {
                    NBTTagCompound entry = patterns.getCompoundTagAt(i);
                    HexPattern pattern = HexPattern.fromNBT(entry);
                    HexAction action = HexActionRegistry.get(pattern);
                    ResourceLocation actionId = action == null
                        ? null : HexActionRegistry.idFor(action);
                    result.add(new ProgramEntry(pattern, actionId,
                        entry.getInteger(KEY_ORIGIN_Q), entry.getInteger(KEY_ORIGIN_R)));
                } catch (RuntimeException ignored) {
                    // Ignore malformed entries without discarding the rest of the program.
                }
            }
            return result;
        }

        // Read the pre-pattern-format string list created by older builds.
        NBTTagList legacy = tag.getTagList(KEY_PROGRAM, 8);
        int legacyOriginQ = 0;
        for (int i = 0; i < legacy.tagCount(); i++) {
            try {
                ResourceLocation actionId = new ResourceLocation(legacy.getStringTagAt(i));
                HexPattern pattern = HexActionRegistry.getPattern(actionId);
                if (pattern != null) {
                    result.add(new ProgramEntry(pattern, actionId, legacyOriginQ, 0));
                    legacyOriginQ += 4;
                }
            } catch (RuntimeException ignored) {
                // Invalid old data is ignored while the remaining program is preserved.
            }
        }
        return result;
    }

    public static final class ProgramEntry {
        private final HexPattern pattern;
        private final ResourceLocation actionId;
        private final int originQ;
        private final int originR;

        private ProgramEntry(HexPattern pattern, ResourceLocation actionId,
                             int originQ, int originR) {
            this.pattern = pattern;
            this.actionId = actionId;
            this.originQ = originQ;
            this.originR = originR;
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

    private static String localizeError(String message) {
        if (message == null || message.isEmpty()) {
            return I18n.translateToLocal("hexcasting.message.staff_error");
        }
        String lower = message.toLowerCase();
        String marker = "no action is registered for pattern";
        int markerIndex = lower.indexOf(marker);
        if (markerIndex >= 0) {
            String signature = message.substring(markerIndex + marker.length()).trim();
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
