package at.petra_k.hexcasting.common.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** A non-stackable knowledge item with an explicit 1.12.2 variant. */
public final class ItemHexKnowledge extends Item {
    private final String variant;

    public ItemHexKnowledge(String variant) {
        this.variant = variant == null ? "" : variant;
        setMaxStackSize(1);
    }

    public String getVariant() {
        return variant;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
    }
}
