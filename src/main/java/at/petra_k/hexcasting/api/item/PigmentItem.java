package at.petra_k.hexcasting.api.item;

import net.minecraft.item.ItemStack;

import java.util.UUID;

/** Item contract for a pigment with optional animated colour output. */
public interface PigmentItem {
    int provideColor(ItemStack stack, UUID owner, float time,
                     double x, double y, double z);
}
