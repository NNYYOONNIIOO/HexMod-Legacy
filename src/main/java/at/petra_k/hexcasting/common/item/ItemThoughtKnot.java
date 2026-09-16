package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.PatternIota;
import at.petra_k.hexcasting.api.item.IotaHolderItem;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import at.petra_k.hexcasting.common.casting.HexEvaluator;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import at.petra_k.hexcasting.common.lib.hex.HexActions;
import at.petra_k.hexcasting.interop.inline.HexInline;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import java.util.List;

/** A single-slot portable pattern holder for the 1.12.2 item set. */
public final class ItemThoughtKnot extends Item implements IotaHolderItem {
    private static final String KEY_ACTION = "action";
    private static final String KEY_PATTERN = "pattern";

    public ItemThoughtKnot() {
        setMaxStackSize(1);
    }

    @Override
    public NBTTagCompound readIotaTag(ItemStack stack) {
        HexPattern pattern = getPattern(stack);
        return pattern == null ? null : new PatternIota(pattern).serialize();
    }

    @Override
    public boolean writeable(ItemStack stack) {
        return readIotaTag(stack) == null;
    }

    @Override
    public boolean canWrite(ItemStack stack, Iota iota) {
        return iota != null && writeable(stack);
    }

    @Override
    public void writeDatum(ItemStack stack, Iota datum) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (datum == null) {
            setPattern(stack, null);
            return;
        }
        if (datum instanceof PatternIota) {
            setPattern(stack, ((PatternIota) datum).getPattern());
        }
    }

    public static void clearDatum(ItemStack stack) {
        if (stack != null && !stack.isEmpty() && stack.getTagCompound() != null) {
            stack.getTagCompound().removeTag(TAG_DATA);
            stack.getTagCompound().removeTag(KEY_ACTION);
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack knot = player.getHeldItem(hand);
        if (!world.isRemote) {
            HexActionRegistry.bootstrap();
            ItemStack offhand = player.getHeldItemOffhand();
            if (player.isSneaking()) {
                clear(knot);
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocal("hexcasting.message.thought_knot_cleared")));
            } else if (!offhand.isEmpty() && offhand.getItem() instanceof ItemPatternScroll) {
                HexPattern pattern = ItemPatternScroll.getPattern(offhand);
                if (pattern == null) {
                    player.sendMessage(new TextComponentString(
                        I18n.translateToLocal("hexcasting.tooltip.scroll.empty")));
                    return new ActionResult<>(EnumActionResult.SUCCESS, knot);
                }
                setPattern(knot, pattern);
                ItemPatternScroll.consumeForWrite(offhand, player);
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocalFormatted("hexcasting.message.thought_knot_written",
                        HexInline.formatPattern(pattern))));
            } else {
                execute(knot, player, hand);
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, knot);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        HexActionRegistry.bootstrap();
        ResourceLocation action = getActionId(stack);
        HexPattern pattern = getPattern(stack);
        tooltip.add(I18n.translateToLocalFormatted("hexcasting.tooltip.thought_knot",
            action == null ? I18n.translateToLocal("hexcasting.tooltip.none") : localizeAction(action)));
        if (pattern != null) {
            tooltip.add(I18n.translateToLocalFormatted("hexcasting.tooltip.pattern",
                HexInline.formatPattern(pattern, HexInline.DEFAULT_PATTERN_COLOR)));
        }
    }

    /** Return the exact pattern, with legacy action NBT as a compatibility fallback. */
    public static HexPattern getPattern(ItemStack stack) {
        if (stack != null && !stack.isEmpty() && stack.getTagCompound() != null) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag.hasKey(KEY_PATTERN, 10)) {
                try {
                    return HexPattern.fromNBT(tag.getCompoundTag(KEY_PATTERN));
                } catch (RuntimeException ignored) {
                    // Fall through to the legacy action representation.
                }
            }
        }
        ResourceLocation action = getActionId(stack);
        return action == null ? null : HexActionRegistry.getPattern(action);
    }

    /** Store or clear the exact pattern and invalidate legacy action NBT. */
    public static void setPattern(ItemStack stack, HexPattern pattern) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (pattern == null) {
            if (tag != null) {
                tag.removeTag(KEY_PATTERN);
                tag.removeTag(KEY_ACTION);
            }
            return;
        }
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setTag(KEY_PATTERN, pattern.serializeToNBT());
        tag.removeTag(KEY_ACTION);
    }

    public static ResourceLocation getActionId(ItemStack stack) {
        HexActionRegistry.bootstrap();
        NBTTagCompound data = stack == null ? null : stack.getTagCompound();
        if (data != null && data.hasKey(TAG_DATA, 10)) {
            try {
                Iota iota = stack.getItem() instanceof ItemThoughtKnot
                    ? ((ItemThoughtKnot) stack.getItem()).readIota(stack) : null;
                if (iota instanceof PatternIota) {
                    at.petra_k.hexcasting.api.casting.action.HexAction action =
                        HexActionRegistry.get(((PatternIota) iota).getPattern());
                    ResourceLocation id = HexActionRegistry.idFor(action);
                    if (id != null) {
                        return id;
                    }
                }
            } catch (Exception ignored) {
                // Malformed Iota data falls back to legacy/default behavior.
            }
        }
        ResourceLocation legacy = getLegacyAction(stack);
        if (legacy != null) {
            return legacy;
        }
        return HexActionRegistry.get(HexActions.PUSH_ONE_ID) == null
            ? HexActionRegistry.firstId() : HexActions.PUSH_ONE_ID;
    }

    private static ResourceLocation getLegacyAction(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey(KEY_ACTION, 8)) {
            try {
                ResourceLocation stored = new ResourceLocation(tag.getString(KEY_ACTION));
                if (HexActionRegistry.get(stored) != null) return stored;
            } catch (RuntimeException ignored) {
                // Old or malformed NBT falls back to the default action.
            }
        }
        return null;
    }

    private static void execute(ItemStack knot, EntityPlayer player, EnumHand hand) {
        ResourceLocation action = getActionId(knot);
        HexPattern pattern = getPattern(knot);
        
        if (pattern == null) {
            player.sendMessage(new TextComponentString(
                I18n.translateToLocal("hexcasting.message.thought_knot_empty")));
            return;
        }
        try {
            IHexCastingData data = HexCapabilities.CASTING_DATA == null
                ? null : player.getCapability(HexCapabilities.CASTING_DATA, null);
            CastingStack result;
            if (data == null) {
                result = HexEvaluator.evaluate(java.util.Collections.singletonList(pattern));
            } else {
                HexEvaluator.evaluate(java.util.Collections.singletonList(pattern),
                    data.getCastingStack(), data, player, hand);
                result = data.getCastingStack();
            }
            String value = result.isEmpty()
                ? I18n.translateToLocal("hexcasting.message.empty_stack") : result.peek().display();
            player.sendMessage(new TextComponentString(
                I18n.translateToLocalFormatted("hexcasting.message.thought_knot_result", value)));
        } catch (CastingException exception) {
            player.sendMessage(new TextComponentString(
                I18n.translateToLocalFormatted("hexcasting.message.thought_knot_error",
                    exception.getMessage())));
        }
    }

    private static void setAction(ItemStack stack, ResourceLocation action) {
        HexPattern pattern = HexActionRegistry.getPattern(action);
        if (pattern != null && stack.getItem() instanceof ItemThoughtKnot) {
            ((ItemThoughtKnot) stack.getItem()).writeDatum(stack, new PatternIota(pattern));
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setString(KEY_ACTION, action.toString());
    }

    private static void clear(ItemStack stack) {
        clearDatum(stack);
    }

    private static String localizeAction(ResourceLocation id) {
        String key = "hexcasting.action." + id.getResourcePath();
        String translated = I18n.translateToLocal(key);
        return key.equals(translated) ? id.getResourcePath() : translated;
    }
}
