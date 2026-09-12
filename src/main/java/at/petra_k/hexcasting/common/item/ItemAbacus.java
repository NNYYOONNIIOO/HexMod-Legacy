package at.petra_k.hexcasting.common.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import java.util.List;

/** Read-only iota storage item corresponding to Hex Casting's abacus. */
public final class ItemAbacus extends Item {
    private static final String KEY_VALUE = "hexcasting_value";
    private static final String KEY_TYPE = "hexcasting_value_type";

    public ItemAbacus() {
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack abacus = player.getHeldItem(hand);
        if (!world.isRemote) {
            if (player.isSneaking()) {
                clear(abacus);
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocal("hexcasting.message.abacus_cleared")));
            } else {
                String value = getDisplayValue(abacus);
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocalFormatted("hexcasting.message.abacus_read", value)));
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, abacus);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        tooltip.add(I18n.translateToLocalFormatted(
            "hexcasting.tooltip.abacus", getDisplayValue(stack)));
    }

    /** Stores a simple scalar iota without embedding user-facing text in code. */
    public static void writeValue(ItemStack stack, String type, String value) {
        if (stack == null || stack.isEmpty()) return;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setString(KEY_TYPE, type == null ? "unknown" : type);
        tag.setString(KEY_VALUE, value == null ? "" : value);
    }

    public static String getDisplayValue(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag == null || !tag.hasKey(KEY_VALUE, 8)) {
            return I18n.translateToLocal("hexcasting.tooltip.none");
        }
        String type = tag.hasKey(KEY_TYPE, 8) ? tag.getString(KEY_TYPE) : "iota";
        return type + ": " + tag.getString(KEY_VALUE);
    }

    public static void clear(ItemStack stack) {
        if (stack != null && stack.getTagCompound() != null) {
            stack.getTagCompound().removeTag(KEY_TYPE);
            stack.getTagCompound().removeTag(KEY_VALUE);
        }
    }
}
