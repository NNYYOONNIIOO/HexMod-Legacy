package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.Mishap;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.item.IotaHolderItem;
import at.petra_k.hexcasting.common.item.ItemAbacus;
import at.petra_k.hexcasting.common.item.ItemHexStaff;
import at.petra_k.hexcasting.common.item.ItemPackagedSpell;
import at.petra_k.hexcasting.common.item.ItemThoughtKnot;
import at.petra_k.hexcasting.common.lib.hex.HexIotaTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Namespaced 1.12.2 item bridge for the read/write Hex actions. */
public final class IotaDataHolder {
    private static final String TAG_IOTA = "hexcasting_iota";

    private IotaDataHolder() {
    }

    public static boolean canRead(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof IotaHolderItem) {
            try {
                return ((IotaHolderItem) stack.getItem()).readIotaTag(stack) != null;
            } catch (RuntimeException ignored) {
                return false;
            }
        }
        return stack.hasTagCompound()
            && stack.getTagCompound().hasKey(TAG_IOTA, 10);
    }

    public static boolean canWrite(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof IotaHolderItem) {
            return ((IotaHolderItem) stack.getItem()).writeable(stack);
        }
        return !(stack.getItem() instanceof ItemAbacus);
    }

    /** Whether the stack contains data that the erase action can remove. */
    public static boolean canClear(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (ItemHexStaff.isStaff(stack)) {
            NBTTagCompound tag = stack.getTagCompound();
            return ItemHexStaff.getProgramSize(stack) > 0
                || tag != null && tag.hasKey("casting_state", 10);
        }
        if (stack.getItem() instanceof ItemPackagedSpell) {
            NBTTagCompound tag = stack.getTagCompound();
            return !ItemPackagedSpell.getPackagedIotas(stack).isEmpty()
                || tag != null && (tag.hasKey(ItemPackagedSpell.TAG_MEDIA, 4)
                    || tag.hasKey(ItemPackagedSpell.TAG_MAX_MEDIA, 4));
        }
        if (stack.getItem() instanceof ItemAbacus) {
            // The abacus is intentionally read-only and is not an erase
            // target in modern Hex.
            return false;
        }
        if (stack.getItem() instanceof ItemThoughtKnot) {
            return ((ItemThoughtKnot) stack.getItem()).readIotaTag(stack) != null;
        }
        if (stack.getItem() instanceof IotaHolderItem) {
            IotaHolderItem holder = (IotaHolderItem) stack.getItem();
            try {
                return holder.readIotaTag(stack) != null
                    && holder.canWrite(stack, null);
            } catch (RuntimeException ignored) {
                return false;
            }
        }
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey(TAG_IOTA, 10);
    }

    /** Clear the data owned by a supported item without changing its item id. */
    public static void clear(ItemStack stack) throws CastingException {
        if (!canClear(stack)) {
            throw Mishap.error("hexcasting.error.data_holder_not_writable");
        }
        if (ItemHexStaff.isStaff(stack)) {
            ItemHexStaff.clearProgram(stack);
            return;
        }
        if (stack.getItem() instanceof ItemPackagedSpell) {
            ItemPackagedSpell.clearPackagedAction(stack);
            return;
        }
        if (stack.getItem() instanceof ItemThoughtKnot) {
            ItemThoughtKnot.clearDatum(stack);
            return;
        }
        if (stack.getItem() instanceof IotaHolderItem) {
            ((IotaHolderItem) stack.getItem()).writeDatum(stack, null);
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            tag.removeTag(TAG_IOTA);
        }
    }

    public static Iota read(ItemStack stack) throws CastingException {
        if (!canRead(stack)) {
            throw Mishap.error("hexcasting.error.data_holder_missing");
        }
        try {
            if (stack.getItem() instanceof IotaHolderItem) {
                Iota value = ((IotaHolderItem) stack.getItem()).readIota(stack);
                if (value == null) {
                    throw Mishap.error("hexcasting.error.data_holder_missing");
                }
                return value;
            }
            return HexIotaTypes.deserialize(stack.getTagCompound().getCompoundTag(TAG_IOTA));
        } catch (CastingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw Mishap.error("hexcasting.error.data_holder_invalid");
        }
    }

    public static void write(ItemStack stack, Iota value) throws CastingException {
        if (!canWrite(stack) || value == null) {
            throw Mishap.error("hexcasting.error.data_holder_not_writable");
        }
        if (stack.getItem() instanceof IotaHolderItem) {
            IotaHolderItem holder = (IotaHolderItem) stack.getItem();
            if (!holder.canWrite(stack, value)) {
                throw Mishap.error("hexcasting.error.data_holder_not_writable");
            }
            holder.writeDatum(stack, value);
            return;
        }
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setTag(TAG_IOTA, value.serialize());
        stack.setTagCompound(tag);
    }
}
