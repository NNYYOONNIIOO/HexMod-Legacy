package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
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
public final class ItemThoughtKnot extends Item {
    private static final String KEY_ACTION = "action";

    public ItemThoughtKnot() {
        setMaxStackSize(1);
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
                ResourceLocation action = ItemPatternScroll.getActionId(offhand);
                setAction(knot, action);
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocalFormatted("hexcasting.message.thought_knot_written",
                        localizeAction(action))));
            } else {
                execute(knot, player);
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, knot);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        HexActionRegistry.bootstrap();
        ResourceLocation action = getActionId(stack);
        HexPattern pattern = HexActionRegistry.getPattern(action);
        tooltip.add(I18n.translateToLocalFormatted("hexcasting.tooltip.thought_knot",
            localizeAction(action)));
        if (pattern != null) {
            tooltip.add(I18n.translateToLocalFormatted("hexcasting.tooltip.pattern",
                HexInline.formatPattern(pattern)));
        }
    }

    public static ResourceLocation getActionId(ItemStack stack) {
        HexActionRegistry.bootstrap();
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey(KEY_ACTION, 8)) {
            try {
                ResourceLocation stored = new ResourceLocation(tag.getString(KEY_ACTION));
                if (HexActionRegistry.get(stored) != null) return stored;
            } catch (RuntimeException ignored) {
                // Old or malformed NBT falls back to the default action.
            }
        }
        return HexActionRegistry.get(HexActions.PUSH_ONE_ID) == null
            ? HexActionRegistry.firstId() : HexActions.PUSH_ONE_ID;
    }

    private static void execute(ItemStack knot, EntityPlayer player) {
        ResourceLocation action = getActionId(knot);
        HexPattern pattern = HexActionRegistry.getPattern(action);
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
                    data.getCastingStack(), data, player);
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
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setString(KEY_ACTION, action.toString());
    }

    private static void clear(ItemStack stack) {
        if (stack.getTagCompound() != null) stack.getTagCompound().removeTag(KEY_ACTION);
    }

    private static String localizeAction(ResourceLocation id) {
        String key = "hexcasting.action." + id.getResourcePath();
        String translated = I18n.translateToLocal(key);
        return key.equals(translated) ? id.getResourcePath() : translated;
    }
}
