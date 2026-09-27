package at.petra_k.hexcasting.api.item;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Item contract for multiple visual variants stored in stack NBT. */
public interface VariantItem {
    String TAG_VARIANT = "variant";

    int numVariants();

    int getVariantValue(ItemStack stack);

    void setVariantValue(ItemStack stack, int variant);

    default int clampVariant(int variant) {
        if (variant < 0) {
            return 0;
        }
        return Math.min(Math.max(0, numVariants() - 1), variant);
    }

    static int readVariant(ItemStack stack, int fallback, int count) {
        if (stack == null || stack.isEmpty() || stack.getTagCompound() == null) {
            return fallback;
        }
        NBTTagCompound tag = stack.getTagCompound();
        int value = tag.hasKey(TAG_VARIANT, 3) ? tag.getInteger(TAG_VARIANT) : fallback;
        return Math.max(0, Math.min(Math.max(0, count - 1), value));
    }
}
