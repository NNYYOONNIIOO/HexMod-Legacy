package at.petra_k.hexcasting.common.lib;

import at.petra_k.hexcasting.api.HexAPI;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

/** Creative tab for the migrated Hex Casting items. */
public final class HexCreativeTab {
    public static final CreativeTabs HEX = new CreativeTabs(HexAPI.MOD_ID) {
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(HexItems.FOCUS);
        }
    };

    private HexCreativeTab() {
    }
}
