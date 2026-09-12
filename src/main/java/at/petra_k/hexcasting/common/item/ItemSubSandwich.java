package at.petra_k.hexcasting.common.item;

import net.minecraft.item.ItemFood;

/** The edible Sub Sandwich item from the migrated item set. */
public final class ItemSubSandwich extends ItemFood {
    public ItemSubSandwich() {
        super(8, 0.8F, false);
        setAlwaysEdible();
        setMaxStackSize(16);
    }
}
