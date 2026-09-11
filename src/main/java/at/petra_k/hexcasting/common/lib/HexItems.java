package at.petra_k.hexcasting.common.lib;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.common.item.ItemHexFocus;
import at.petra_k.hexcasting.common.item.ItemHexGuideBook;
import at.petra_k.hexcasting.common.item.ItemPatternScroll;
import at.petra_k.hexcasting.common.item.ItemScryingLens;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** 1.12.2 Forge item registry. */
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID)
public final class HexItems {
    public static final ItemHexFocus FOCUS = (ItemHexFocus) new ItemHexFocus()
        .setRegistryName(HexAPI.MOD_ID, "focus")
        .setUnlocalizedName(HexAPI.MOD_ID + ".focus")
        .setCreativeTab(HexCreativeTab.HEX);

    public static final ItemScryingLens SCRYING_LENS = (ItemScryingLens) new ItemScryingLens()
        .setRegistryName(HexAPI.MOD_ID, "scrying_lens")
        .setUnlocalizedName(HexAPI.MOD_ID + ".scrying_lens")
        .setCreativeTab(HexCreativeTab.HEX);

    public static final ItemPatternScroll PATTERN_SCROLL = (ItemPatternScroll) new ItemPatternScroll()
        .setRegistryName(HexAPI.MOD_ID, "pattern_scroll")
        .setUnlocalizedName(HexAPI.MOD_ID + ".pattern_scroll")
        .setCreativeTab(HexCreativeTab.HEX);

    public static final ItemHexGuideBook GUIDE_BOOK = (ItemHexGuideBook) new ItemHexGuideBook()
        .setCreativeTab(HexCreativeTab.HEX);

    private HexItems() {
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(FOCUS);
        event.getRegistry().register(SCRYING_LENS);
        event.getRegistry().register(PATTERN_SCROLL);
        event.getRegistry().register(GUIDE_BOOK);
    }
}
