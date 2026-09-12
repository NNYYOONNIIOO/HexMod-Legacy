package at.petra_k.hexcasting.common.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import java.util.List;

/** Read-only iota storage item corresponding to Hex Casting's abacus. */
public final class ItemAbacus extends Item {
    public ItemAbacus() {
        setMaxStackSize(1);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        tooltip.add(I18n.translateToLocal("hexcasting.tooltip.abacus"));
    }
}

