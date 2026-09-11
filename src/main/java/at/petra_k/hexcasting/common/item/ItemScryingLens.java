package at.petra_k.hexcasting.common.item;

import baubles.api.IBauble;
import baubles.api.BaubleType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** BaublesEX-backed wearable equivalent of Hex Casting's scrying lens. */
public final class ItemScryingLens extends Item implements IBauble {
    public ItemScryingLens() {
        setMaxStackSize(1);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.HEAD;
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase player) {
        // Pattern overlays and entity inspection will be connected here after
        // the 1.12.2 client rendering layer is migrated.
    }
}
