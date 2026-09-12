package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.common.item.ItemAbacus;
import at.petra_k.hexcasting.common.lib.hex.HexIotaTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Namespaced 1.12.2 item bridge for the read/write Hex actions. */
public final class IotaDataHolder {
    private static final String TAG_IOTA = "hexcasting_iota";

    private IotaDataHolder() {
    }

    public static boolean canRead(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.hasTagCompound()
            && stack.getTagCompound().hasKey(TAG_IOTA, 10);
    }

    public static boolean canWrite(ItemStack stack) {
        return stack != null && !stack.isEmpty()
            && !(stack.getItem() instanceof ItemAbacus);
    }

    public static Iota read(ItemStack stack) throws CastingException {
        if (!canRead(stack)) {
            throw new CastingException("hexcasting.error.data_holder_missing");
        }
        try {
            return HexIotaTypes.deserialize(stack.getTagCompound().getCompoundTag(TAG_IOTA));
        } catch (RuntimeException exception) {
            throw new CastingException("hexcasting.error.data_holder_invalid");
        }
    }

    public static void write(ItemStack stack, Iota value) throws CastingException {
        if (!canWrite(stack) || value == null) {
            throw new CastingException("hexcasting.error.data_holder_not_writable");
        }
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setTag(TAG_IOTA, value.serialize());
        stack.setTagCompound(tag);
    }
}
