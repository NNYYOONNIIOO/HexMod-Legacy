package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.casting.HexEvaluator;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import at.petra_k.hexcasting.interop.inline.HexInline;
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

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

/** A single-use or reusable packaged spell container for the 1.12.2 port. */
public class ItemPackagedSpell extends Item {
    private static final String KEY_PACKAGED_ACTION = "packaged_action";
    private static final String KEY_PATTERNS = "patterns";
    private static final String KEY_PATTERN_PROGRAM = "pattern_program";
    private static final String KEY_VARIANT = "variant";
    private static final int VARIANT_COUNT = 5;

    /** Returns the action stored in this packaged spell, or null for an empty item. */
    public static ResourceLocation getPackagedAction(ItemStack stack) {
        List<ResourceLocation> actions = getPackagedActions(stack);
        return actions.isEmpty() ? null : actions.get(0);
    }

    /** Reads the current program, while accepting the old single-action NBT format. */
    public static List<ResourceLocation> getPackagedActions(ItemStack stack) {
        List<ResourceLocation> actions = new ArrayList<>();
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag == null) {
            return actions;
        }

        if (tag.hasKey(KEY_PATTERNS, 9)) {
            NBTTagList list = tag.getTagList(KEY_PATTERNS, 8);
            for (int i = 0; i < list.tagCount(); i++) {
                addValidAction(actions, list.getStringTagAt(i));
            }
        }
        if (actions.isEmpty() && tag.hasKey(KEY_PACKAGED_ACTION, 8)) {
            addValidAction(actions, tag.getString(KEY_PACKAGED_ACTION));
        }
        return actions;
    }

    /** Reads exact drawable patterns, falling back to legacy registered actions. */
    public static List<HexPattern> getPackagedPatterns(ItemStack stack) {
        List<HexPattern> patterns = new ArrayList<>();
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag != null && tag.hasKey(KEY_PATTERN_PROGRAM, 9)) {
            NBTTagList list = tag.getTagList(KEY_PATTERN_PROGRAM, 10);
            for (int i = 0; i < list.tagCount(); i++) {
                try {
                    patterns.add(HexPattern.fromNBT(list.getCompoundTagAt(i)));
                } catch (RuntimeException ignored) {
                    // Preserve valid entries even when one old entry is malformed.
                }
            }
            return patterns;
        }
        for (ResourceLocation action : getPackagedActions(stack)) {
            HexPattern pattern = HexActionRegistry.getPattern(action);
            if (pattern != null) {
                patterns.add(pattern);
            }
        }
        return patterns;
    }

    private static void addValidAction(List<ResourceLocation> actions, String rawId) {
        try {
            ResourceLocation id = new ResourceLocation(rawId);
            if (HexActionRegistry.getPattern(id) != null) {
                actions.add(id);
            }
        } catch (RuntimeException ignored) {
            // Ignore malformed entries so one damaged stack does not crash item rendering.
        }
    }

    public static int getVariant(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag == null) {
            return 0;
        }
        return Math.max(0, Math.min(VARIANT_COUNT - 1, tag.getInteger(KEY_VARIANT)));
    }

    public static void setVariant(ItemStack stack, int variant) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        int normalized = Math.max(0, Math.min(VARIANT_COUNT - 1, variant));
        NBTTagCompound tag = stack.getTagCompound();
        if (normalized == 0) {
            if (tag != null) {
                tag.removeTag(KEY_VARIANT);
            }
            return;
        }
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setInteger(KEY_VARIANT, normalized);
    }

    public static void cycleVariant(ItemStack stack) {
        setVariant(stack, (getVariant(stack) + 1) % VARIANT_COUNT);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        HexActionRegistry.bootstrap();
        if (player.isSneaking()) {
            clearPackagedAction(stack);
            player.sendMessage(new TextComponentString(
                I18n.translateToLocal("hexcasting.message.program_cleared")));
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        ItemStack offhand = player.getHeldItemOffhand();
        if (!offhand.isEmpty() && offhand.getItem() instanceof ItemPatternScroll) {
            HexPattern pattern = ItemPatternScroll.getPattern(offhand);
            if (pattern == null) {
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocal("hexcasting.tooltip.scroll.empty")));
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }
            appendPackagedPattern(stack, pattern);
            ItemPatternScroll.consumeForWrite(offhand, player);
            int count = getPackagedPatterns(stack).size();
            player.sendMessage(new TextComponentString(
                I18n.translateToLocalFormatted("hexcasting.message.program_added",
                    HexInline.formatPattern(pattern), count, count)));
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        List<ResourceLocation> actions = getPackagedActions(stack);
        List<HexPattern> patterns = getPackagedPatterns(stack);
        if (actions.isEmpty() && patterns.isEmpty()) {
            player.sendMessage(new TextComponentString(
                I18n.translateToLocal("hexcasting.message.program_empty")));
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        if (patterns.isEmpty()) {
            clearPackagedAction(stack);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        try {
            IHexCastingData data = HexCapabilities.CASTING_DATA == null
                ? null : player.getCapability(HexCapabilities.CASTING_DATA, null);
            CastingStack result;
            if (data == null) {
                result = HexEvaluator.evaluate(patterns);
            } else {
                HexEvaluator.evaluate(patterns, data.getCastingStack(), data, player, hand);
                result = data.getCastingStack();
            }
            String resultText = result.isEmpty()
                ? I18n.translateToLocal("hexcasting.message.empty_stack")
                : result.peek().display();
            player.sendMessage(new TextComponentString(
                I18n.translateToLocalFormatted("hexcasting.message.program_result", resultText)));
            if (consumeOnUse() && !player.capabilities.isCreativeMode) {
                stack.shrink(1);
            }
        } catch (CastingException exception) {
            player.sendMessage(new TextComponentString(
                I18n.translateToLocalFormatted("hexcasting.message.staff_error", exception.getMessage())));
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        HexActionRegistry.bootstrap();
        List<ResourceLocation> actions = getPackagedActions(stack);
        List<HexPattern> patterns = getPackagedPatterns(stack);
        if (patterns.isEmpty()) {
            tooltip.add(I18n.translateToLocal("hexcasting.tooltip.none"));
            return;
        }
        if (actions.isEmpty()) {
            for (HexPattern pattern : patterns) {
                tooltip.add(I18n.translateToLocal("hexcasting.tooltip.pattern") + ": "
                    + HexInline.formatPattern(pattern, HexInline.DEFAULT_PATTERN_COLOR));
            }
            return;
        }
        for (ResourceLocation action : actions) {
            tooltip.add(I18n.translateToLocal("hexcasting.tooltip.action") + ": " + localizeAction(action));
            HexPattern pattern = HexActionRegistry.getPattern(action);
            if (pattern != null) {
                tooltip.add(I18n.translateToLocal("hexcasting.tooltip.pattern") + ": "
                    + HexInline.formatPattern(pattern, HexInline.DEFAULT_PATTERN_COLOR));
            }
        }
    }

    /** Cyphers override this because they are consumed after a successful cast. */
    protected boolean consumeOnUse() {
        return false;
    }

    public static void setPackagedAction(ItemStack stack, ResourceLocation action) {
        if (stack == null || stack.isEmpty() || action == null
            || HexActionRegistry.getPattern(action) == null) {
            return;
        }
        clearPackagedAction(stack);
        appendPackagedAction(stack, action);
    }

    public static void appendPackagedAction(ItemStack stack, ResourceLocation action) {
        if (stack == null || stack.isEmpty() || action == null
            || HexActionRegistry.getPattern(action) == null) {
            return;
        }
        if (stack.getTagCompound() != null
            && stack.getTagCompound().hasKey(KEY_PATTERN_PROGRAM, 9)) {
            appendPackagedPattern(stack, HexActionRegistry.getPattern(action));
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        NBTTagList patterns = tag.hasKey(KEY_PATTERNS, 9)
            ? tag.getTagList(KEY_PATTERNS, 8) : new NBTTagList();
        patterns.appendTag(new NBTTagString(action.toString()));
        tag.setTag(KEY_PATTERNS, patterns);
        tag.removeTag(KEY_PACKAGED_ACTION);
    }

    /** Replace the packaged program with one exact drawable pattern. */
    public static void setPackagedPattern(ItemStack stack, HexPattern pattern) {
        clearPackagedAction(stack);
        appendPackagedPattern(stack, pattern);
    }

    /** Append an exact pattern, converting any legacy action list first. */
    public static void appendPackagedPattern(ItemStack stack, HexPattern pattern) {
        if (stack == null || stack.isEmpty() || pattern == null) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        NBTTagList patterns;
        if (tag.hasKey(KEY_PATTERN_PROGRAM, 9)) {
            patterns = tag.getTagList(KEY_PATTERN_PROGRAM, 10);
        } else {
            patterns = new NBTTagList();
            for (ResourceLocation action : getPackagedActions(stack)) {
                HexPattern oldPattern = HexActionRegistry.getPattern(action);
                if (oldPattern != null) {
                    patterns.appendTag(oldPattern.serializeToNBT());
                }
            }
        }
        patterns.appendTag(pattern.serializeToNBT());
        tag.setTag(KEY_PATTERN_PROGRAM, patterns);
        tag.removeTag(KEY_PATTERNS);
        tag.removeTag(KEY_PACKAGED_ACTION);
    }

    public static void clearPackagedAction(ItemStack stack) {
        if (stack != null && !stack.isEmpty() && stack.getTagCompound() != null) {
            stack.getTagCompound().removeTag(KEY_PACKAGED_ACTION);
            stack.getTagCompound().removeTag(KEY_PATTERNS);
            stack.getTagCompound().removeTag(KEY_PATTERN_PROGRAM);
        }
    }

    private static String localizeAction(ResourceLocation id) {
        String key = "hexcasting.action." + id.getResourcePath();
        String translated = I18n.translateToLocal(key);
        return key.equals(translated) ? id.getResourcePath() : translated;
    }

    public ItemPackagedSpell() {
        setMaxStackSize(1);
    }
}
