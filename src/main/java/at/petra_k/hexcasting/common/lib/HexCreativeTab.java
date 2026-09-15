package at.petra_k.hexcasting.common.lib;

import at.petra_k.hexcasting.api.HexAPI;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Creative tab for the migrated Hex Casting items. */
public final class HexCreativeTab {
    public static final CreativeTabs HEX = new CreativeTabs(HexAPI.MOD_ID) {
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(HexItems.FOCUS);
        }
    };

    /** Dedicated scroll tab, matching Hex 1.20.1's ancient-scroll tab. */
    public static final CreativeTabs SCROLLS = new CreativeTabs(HexAPI.MOD_ID + ".scrolls") {
        @Override
        public ItemStack getTabIconItem() {
            Item scroll = HexItems.EXTRA_ITEMS.get("scroll");
            return scroll == null ? new ItemStack(HexItems.FOCUS) : new ItemStack(scroll);
        }
    };

    private HexCreativeTab() {
    }
}
