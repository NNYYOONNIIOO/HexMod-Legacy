package at.petra_k.hexcasting.common.capability;

import at.petra_k.hexcasting.api.addldata.ADVariantItem;
import at.petra_k.hexcasting.api.item.VariantItem;
import net.minecraft.item.ItemStack;

/** Adapter exposing a stack-backed VariantItem as a Forge capability. */
public final class HexItemVariant implements ADVariantItem {
    private final VariantItem item;
    private final ItemStack stack;

    public HexItemVariant(VariantItem item, ItemStack stack) {
        this.item = item;
        this.stack = stack;
    }

    @Override
    public int numVariants() {
        return item.numVariants();
    }

    @Override
    public int getVariant() {
        return item.getVariantValue(stack);
    }

    @Override
    public void setVariant(int variant) {
        item.setVariantValue(stack, variant);
    }
}
