package at.petra_k.hexcasting.common.lib;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.api.misc.MediaConstants;
import at.petra_k.hexcasting.common.item.ItemHexFocus;
import at.petra_k.hexcasting.common.item.ItemHexStaff;
import at.petra_k.hexcasting.common.item.ItemAbacus;
import at.petra_k.hexcasting.common.item.ItemColorizer;
import at.petra_k.hexcasting.common.item.ItemKnowledgeFragment;
import at.petra_k.hexcasting.common.item.ItemHexKnowledge;
import at.petra_k.hexcasting.common.item.ItemMediaBattery;
import at.petra_k.hexcasting.common.item.ItemMediaMaterial;
import at.petra_k.hexcasting.common.item.ItemPackagedSpell;
import at.petra_k.hexcasting.common.item.ItemCypher;
import at.petra_k.hexcasting.common.item.ItemTrinket;
import at.petra_k.hexcasting.common.item.ItemArtifact;
import at.petra_k.hexcasting.common.item.ItemPatternScroll;
import at.petra_k.hexcasting.common.item.ItemJewelerHammer;
import at.petra_k.hexcasting.common.item.ItemSpellbook;
import at.petra_k.hexcasting.common.item.ItemSubSandwich;
import at.petra_k.hexcasting.common.item.ItemThoughtKnot;
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
        .setRegistryName(HexAPI.MOD_ID, "lens")
        .setUnlocalizedName(HexAPI.MOD_ID + ".lens")
        .setCreativeTab(HexCreativeTab.HEX);

    public static final ItemMediaBattery BATTERY = (ItemMediaBattery) new ItemMediaBattery()
        .setRegistryName(HexAPI.MOD_ID, "battery")
        .setUnlocalizedName(HexAPI.MOD_ID + ".battery")
        .setCreativeTab(HexCreativeTab.HEX);

    public static final ItemCypher CYPHER = (ItemCypher) new ItemCypher()
        .setRegistryName(HexAPI.MOD_ID, "cypher")
        .setUnlocalizedName(HexAPI.MOD_ID + ".cypher")
        .setCreativeTab(HexCreativeTab.HEX);

    public static final ItemTrinket TRINKET = (ItemTrinket) new ItemTrinket()
        .setRegistryName(HexAPI.MOD_ID, "trinket")
        .setUnlocalizedName(HexAPI.MOD_ID + ".trinket")
        .setCreativeTab(HexCreativeTab.HEX);

    public static final ItemArtifact ARTIFACT = (ItemArtifact) new ItemArtifact()
        .setRegistryName(HexAPI.MOD_ID, "artifact")
        .setUnlocalizedName(HexAPI.MOD_ID + ".artifact")
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
        "dye_colorizer_black",
        "pride_colorizer_agender",
        "pride_colorizer_aroace",
        "pride_colorizer_aromantic",
        "pride_colorizer_asexual",
        "pride_colorizer_bisexual",
        "pride_colorizer_demiboy",
        "pride_colorizer_demigirl",
        "pride_colorizer_gay",
        "pride_colorizer_genderfluid",
        "pride_colorizer_genderqueer",
        "pride_colorizer_intersex",
        "pride_colorizer_lesbian",
        "pride_colorizer_nonbinary",
        "pride_colorizer_pansexual",
        "pride_colorizer_plural",
        "pride_colorizer_transgender"
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
            } else if (id.equals("thought_knot")) {
                item = new ItemThoughtKnot();
            } else if (id.equals("jeweler_hammer")) {
                item = new ItemJewelerHammer();
            } else if (id.equals("sub_sandwich")) {
                item = new ItemSubSandwich();
            } else if (id.equals("amethyst_dust")) {
                item = new ItemMediaMaterial(MediaConstants.DUST_UNIT, "amethyst_dust", 3000);
            } else if (id.equals("charged_amethyst")) {
                item = new ItemMediaMaterial(MediaConstants.CRYSTAL_UNIT, "charged_amethyst", 1000);
            } else if (id.equals("quenched_allay_shard")) {
                item = new ItemMediaMaterial(MediaConstants.SHARD_UNIT * 3L, "quenched_allay_shard", 800);
            } else if (id.equals("lore_fragment")) {
                item = new ItemKnowledgeFragment("lore_fragment");
            } else if (id.equals("creative_unlocker")) {
                item = new ItemHexKnowledge("creative_unlocker");
            } else if (id.equals("lore_fragment")) {
                item = new ItemHexKnowledge("lore_fragment");
            } else if (id.equals("scroll_small") || id.equals("scroll_medium") || id.equals("scroll")) {
                item = new ItemPatternScroll(scrollSize(id));
            } else if (id.contains("colorizer")) {
                item = new ItemColorizer(id);
            } else if (id.startsWith("staff/")) {
                item = new ItemHexStaff(id.substring("staff/".length()));
            } else {
                item = new ItemHexKnowledge(id);
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
        items.addAll(EXTRA_ITEMS.values());
        return items;
    }

    private static int scrollSize(String id) {
        if ("scroll_small".equals(id)) {
            return 1;
        }
        if ("scroll_medium".equals(id)) {
            return 2;
        }
        return 3;
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
        EXTRA_ITEMS.values().forEach(event.getRegistry()::register);
    }
}
