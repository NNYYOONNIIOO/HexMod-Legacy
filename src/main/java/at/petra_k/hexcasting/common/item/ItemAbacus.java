package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.iota.DoubleIota;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.PatternIota;
import at.petra_k.hexcasting.api.item.IotaHolderItem;
import at.petra_k.hexcasting.common.casting.IotaDataHolder;
import at.petra_k.hexcasting.common.lib.HexSounds;
import at.petra_k.hexcasting.interop.inline.HexInline;
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
public final class ItemAbacus extends Item implements IotaHolderItem {
    /** The canonical key used by Hex's abacus and by the scroll handler. */
    public static final String TAG_VALUE = "value";
    /** Keys written by the first 1.12.2 port, retained for old stacks. */
    private static final String LEGACY_VALUE = "hexcasting_value";
    private static final String LEGACY_TYPE = "hexcasting_value_type";

    public ItemAbacus() {
        setMaxStackSize(1);
    }

    @Override
    public NBTTagCompound readIotaTag(ItemStack stack) {
        return new DoubleIota(getValue(stack)).serialize();
    }

    @Override
    public boolean writeable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canWrite(ItemStack stack, Iota datum) {
        return false;
    }

    @Override
    public void writeDatum(ItemStack stack, Iota datum) {
        // The abacus is intentionally read-only to casting actions.
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack abacus = player.getHeldItem(hand);
        if (!world.isRemote) {
            if (player.isSneaking()) {
                clear(abacus);
                player.playSound(HexSounds.ABACUS_SHAKE, 1.0F, 1.0F);
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
        tag.setString(LEGACY_TYPE, type == null ? "unknown" : type);
        tag.setString(LEGACY_VALUE, value == null ? "" : value);
    }

    public static String getDisplayValue(ItemStack stack) {
        if (IotaDataHolder.canRead(stack)) {
            try {
                Iota value = IotaDataHolder.read(stack);
                String display = value instanceof PatternIota
                    ? HexInline.formatPattern(((PatternIota) value).getPattern(),
                        HexInline.DEFAULT_PATTERN_COLOR)
                    : value.display();
                return value.getType().getId() + ": " + display;
            } catch (CastingException ignored) {
                return I18n.translateToLocal("hexcasting.error.data_holder_invalid");
            }
        }
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag != null && tag.hasKey(TAG_VALUE, 99)) {
            return Double.toString(getValue(stack));
        }
        if (tag == null || !tag.hasKey(LEGACY_VALUE, 8)) {
            return Double.toString(getValue(stack));
        }
        String type = tag.hasKey(LEGACY_TYPE, 8) ? tag.getString(LEGACY_TYPE) : "iota";
        return type + ": " + tag.getString(LEGACY_VALUE);
    }

    public static void clear(ItemStack stack) {
        if (stack != null && stack.getTagCompound() != null) {
            stack.getTagCompound().removeTag(TAG_VALUE);
            stack.getTagCompound().removeTag(LEGACY_TYPE);
            stack.getTagCompound().removeTag(LEGACY_VALUE);
        }
    }

    /** Read the canonical numeric value, accepting the old integer/double NBT types. */
    public static double getValue(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_VALUE, 99)) {
            return 0.0D;
        }
        return tag.getDouble(TAG_VALUE);
    }

    /** Store the canonical numeric value used by the scroll-wheel handler. */
    public static void setValue(ItemStack stack, double value) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setDouble(TAG_VALUE, value);
        // Do not leave the old display-only representation behind after a
        // wheel edit; otherwise legacy readers could show a different value.
        tag.removeTag(LEGACY_TYPE);
        tag.removeTag(LEGACY_VALUE);
    }
}
