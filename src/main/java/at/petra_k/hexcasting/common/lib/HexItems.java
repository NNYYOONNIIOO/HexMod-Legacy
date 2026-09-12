package at.petra_k.hexcasting.common.lib;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.common.item.ItemHexFocus;
import at.petra_k.hexcasting.common.item.ItemHexStaff;
import at.petra_k.hexcasting.common.item.ItemAbacus;
import at.petra_k.hexcasting.common.item.ItemMediaBattery;
import at.petra_k.hexcasting.common.item.ItemPackagedSpell;
import at.petra_k.hexcasting.common.item.ItemPatternScroll;
import at.petra_k.hexcasting.common.item.ItemSpellbook;
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

    public static final ItemHexStaff STAFF = (ItemHexStaff) new ItemHexStaff()
        .setRegistryName(HexAPI.MOD_ID, "staff")
        .setUnlocalizedName(HexAPI.MOD_ID + ".staff")
        .setCreativeTab(HexCreativeTab.HEX);

    public static final ItemScryingLens SCRYING_LENS = (ItemScryingLens) new ItemScryingLens()
        .setRegistryName(HexAPI.MOD_ID, "scrying_lens")
        .setUnlocalizedName(HexAPI.MOD_ID + ".scrying_lens")
        .setCreativeTab(HexCreativeTab.HEX);

    public static final ItemMediaBattery BATTERY = (ItemMediaBattery) new ItemMediaBattery()
        .setRegistryName(HexAPI.MOD_ID, "battery")
        .setUnlocalizedName(HexAPI.MOD_ID + ".battery")
        .setCreativeTab(HexCreativeTab.HEX);

    public static final ItemPackagedSpell CYPHER = (ItemPackagedSpell) new ItemPackagedSpell()
        .setRegistryName(HexAPI.MOD_ID, "cypher")
        .setUnlocalizedName(HexAPI.MOD_ID + ".cypher")
        .setCreativeTab(HexCreativeTab.HEX);

    public static final ItemPackagedSpell TRINKET = (ItemPackagedSpell) new ItemPackagedSpell()
        .setRegistryName(HexAPI.MOD_ID, "trinket")
        .setUnlocalizedName(HexAPI.MOD_ID + ".trinket")
        .setCreativeTab(HexCreativeTab.HEX);

    public static final ItemPackagedSpell ARTIFACT = (ItemPackagedSpell) new ItemPackagedSpell()
        .setRegistryName(HexAPI.MOD_ID, "artifact")
        .setUnlocalizedName(HexAPI.MOD_ID + ".artifact")
        .setCreativeTab(HexCreativeTab.HEX);

    public static final ItemPatternScroll PATTERN_SCROLL = (ItemPatternScroll) new ItemPatternScroll()
        .setRegistryName(HexAPI.MOD_ID, "pattern_scroll")
        .setUnlocalizedName(HexAPI.MOD_ID + ".pattern_scroll")
        .setCreativeTab(HexCreativeTab.HEX);


    /** Generic item registrations for content whose specialized behavior is ported later. */
    private static final String[] EXTRA_ITEM_IDS = new String[] {
        "amethyst_dust",
        "charged_amethyst",
        "quenched_allay_shard",
        "staff/oak",
        "staff/spruce",
        "staff/birch",
        "staff/jungle",
        "staff/acacia",
        "staff/dark_oak",
        "staff/crimson",
        "staff/warped",
        "staff/mangrove",
        "staff/cherry",
        "staff/bamboo",
        "staff/edified",
        "staff/quenched",
        "staff/mindsplice",
        "lens",
        "abacus",
        "thought_knot",
        "spellbook",
        "ancient_cypher",
        "jeweler_hammer",
        "scroll_small",
        "scroll_medium",
        "scroll",
        "uuid_colorizer",
        "default_colorizer",
        "ancient_colorizer",
        "sub_sandwich",
        "lore_fragment",
        "creative_unlocker",
        "dye_colorizer_white",
        "dye_colorizer_orange",
        "dye_colorizer_magenta",
        "dye_colorizer_light_blue",
        "dye_colorizer_yellow",
        "dye_colorizer_lime",
        "dye_colorizer_pink",
        "dye_colorizer_gray",
        "dye_colorizer_light_gray",
        "dye_colorizer_cyan",
        "dye_colorizer_purple",
        "dye_colorizer_blue",
        "dye_colorizer_brown",
        "dye_colorizer_green",
        "dye_colorizer_red",
        "dye_colorizer_black"
    };
    public static final java.util.Map<String, Item> EXTRA_ITEMS = new java.util.LinkedHashMap<>();

    static {
        for (String id : EXTRA_ITEM_IDS) {
            Item item;
            if (id.equals("spellbook")) {
                item = new ItemSpellbook();
            } else if (id.equals("ancient_cypher")) {
                item = new ItemPackagedSpell();
            } else if (id.equals("abacus")) {
                item = new ItemAbacus();
            } else if (id.equals("scroll_small") || id.equals("scroll_medium") || id.equals("scroll")) {
                item = new ItemPatternScroll();
            } else if (id.startsWith("staff/")) {
                item = new ItemHexStaff();
            } else {
                item = new Item();
            }
            EXTRA_ITEMS.put(id, item.setRegistryName(HexAPI.MOD_ID, id)
                .setUnlocalizedName(HexAPI.MOD_ID + "." + id)
                .setCreativeTab(HexCreativeTab.HEX));
        }
    }

    public static java.util.List<Item> allItems() {
        java.util.List<Item> items = new java.util.ArrayList<>();
        items.add(FOCUS);
        items.add(STAFF);
        items.add(SCRYING_LENS);
        items.add(BATTERY);
        items.add(CYPHER);
        items.add(TRINKET);
        items.add(ARTIFACT);
        items.add(PATTERN_SCROLL);
        items.addAll(EXTRA_ITEMS.values());
        return items;
    }

    private HexItems() {
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(FOCUS);
        event.getRegistry().register(STAFF);
        event.getRegistry().register(SCRYING_LENS);
        event.getRegistry().register(BATTERY);
        event.getRegistry().register(CYPHER);
        event.getRegistry().register(TRINKET);
        event.getRegistry().register(ARTIFACT);
        event.getRegistry().register(PATTERN_SCROLL);
        EXTRA_ITEMS.values().forEach(event.getRegistry()::register);
    }
}
