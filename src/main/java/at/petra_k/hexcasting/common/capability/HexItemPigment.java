package at.petra_k.hexcasting.common.capability;

import at.petra_k.hexcasting.api.addldata.ADPigment;
import at.petra_k.hexcasting.api.item.PigmentItem;
import net.minecraft.item.ItemStack;

import java.util.UUID;

/** Adapter exposing a stack-backed PigmentItem as a Forge capability. */
public final class HexItemPigment implements ADPigment {
    private final PigmentItem item;
    private final ItemStack stack;

    public HexItemPigment(PigmentItem item, ItemStack stack) {
        this.item = item;
        this.stack = stack;
    }

    @Override
    public int getColor(UUID owner, float time, double x, double y, double z) {
        return item.provideColor(stack, owner, time, x, y, z);
    }
}
