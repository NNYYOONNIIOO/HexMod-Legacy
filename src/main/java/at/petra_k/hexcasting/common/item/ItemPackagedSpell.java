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
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;

/** A single-use or reusable packaged spell container for the 1.12.2 port. */
public final class ItemPackagedSpell extends Item {
    private static final String KEY_PACKAGED_ACTION = "packaged_action";

    /** Returns the action stored in this packaged spell, or null for an empty item. */
    public static ResourceLocation getPackagedAction(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag == null || !tag.hasKey(KEY_PACKAGED_ACTION, 8)) {
            return null;
        }
        try {
            ResourceLocation id = new ResourceLocation(tag.getString(KEY_PACKAGED_ACTION));
            return HexActionRegistry.getPattern(id) == null ? null : id;
        } catch (RuntimeException ignored) {
            return null;
        }
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
            ResourceLocation action = ItemPatternScroll.getActionId(offhand);
            setPackagedAction(stack, action);
            player.sendMessage(new TextComponentString(
                I18n.translateToLocalFormatted("hexcasting.message.program_added",
                    localizeAction(action), 1, 1)));
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        ResourceLocation action = getPackagedAction(stack);
        if (action == null) {
            player.sendMessage(new TextComponentString(
                I18n.translateToLocal("hexcasting.message.program_empty")));
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        HexPattern pattern = HexActionRegistry.getPattern(action);
        if (pattern == null) {
            clearPackagedAction(stack);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        try {
            IHexCastingData data = HexCapabilities.CASTING_DATA == null
                ? null : player.getCapability(HexCapabilities.CASTING_DATA, null);
            CastingStack result;
            if (data == null) {
                result = HexEvaluator.evaluate(Collections.singletonList(pattern));
            } else {
                HexEvaluator.evaluate(Collections.singletonList(pattern), data.getCastingStack(), data, player);
                result = data.getCastingStack();
            }
            String resultText = result.isEmpty()
                ? I18n.translateToLocal("hexcasting.message.empty_stack")
                : result.peek().display();
            player.sendMessage(new TextComponentString(
                I18n.translateToLocalFormatted("hexcasting.message.program_result", resultText)));
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
        ResourceLocation action = getPackagedAction(stack);
        if (action == null) {
            tooltip.add(I18n.translateToLocal("hexcasting.tooltip.none"));
            return;
        }
        tooltip.add(I18n.translateToLocal("hexcasting.tooltip.action") + ": " + localizeAction(action));
        HexPattern pattern = HexActionRegistry.getPattern(action);
        if (pattern != null) {
            tooltip.add(I18n.translateToLocal("hexcasting.tooltip.pattern") + ": " + HexInline.formatPattern(pattern));
        }
    }

    public static void setPackagedAction(ItemStack stack, ResourceLocation action) {
        if (stack == null || stack.isEmpty() || action == null
            || HexActionRegistry.getPattern(action) == null) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setString(KEY_PACKAGED_ACTION, action.toString());
    }

    public static void clearPackagedAction(ItemStack stack) {
        if (stack != null && !stack.isEmpty() && stack.getTagCompound() != null) {
            stack.getTagCompound().removeTag(KEY_PACKAGED_ACTION);
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
