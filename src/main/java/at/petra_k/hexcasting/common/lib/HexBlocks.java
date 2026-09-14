package at.petra_k.hexcasting.common.lib;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.common.block.BlockConjured;
import at.petra_k.hexcasting.common.block.BlockHexPillar;
import at.petra_k.hexcasting.common.block.BlockConjuredLight;
import at.petra_k.hexcasting.common.block.BlockAkashicRecord;
import at.petra_k.hexcasting.common.block.BlockAkashicBookshelf;
import at.petra_k.hexcasting.common.block.BlockAkashicConnector;
import at.petra_k.hexcasting.common.block.BlockHexDecorative;
import at.petra_k.hexcasting.common.block.BlockHexFalling;
import at.petra_k.hexcasting.common.block.BlockHexRotated;
import at.petra_k.hexcasting.common.block.BlockHexSlab;
import at.petra_k.hexcasting.common.block.BlockHexButton;
import at.petra_k.hexcasting.common.block.BlockHexDoor;
import at.petra_k.hexcasting.common.block.BlockHexFence;
import at.petra_k.hexcasting.common.block.BlockHexFenceGate;
import at.petra_k.hexcasting.common.block.BlockHexLeaves;
import at.petra_k.hexcasting.common.block.BlockHexLight;
import at.petra_k.hexcasting.common.block.BlockHexSconce;
import at.petra_k.hexcasting.common.block.BlockHexLog;
import at.petra_k.hexcasting.common.block.BlockHexPressurePlate;
import at.petra_k.hexcasting.common.block.BlockHexStairs;
import at.petra_k.hexcasting.common.block.BlockHexTrapdoor;
import at.petra_k.hexcasting.common.block.BlockBooleanDirectrix;
import at.petra_k.hexcasting.common.block.BlockEmptyDirectrix;
import at.petra_k.hexcasting.common.block.BlockGreatImpetus;
import at.petra_k.hexcasting.common.block.BlockImpetus;
import at.petra_k.hexcasting.common.block.BlockRedstoneDirectrix;
import at.petra_k.hexcasting.common.block.BlockSlate;
import at.petra_k.hexcasting.common.block.BlockSpellCircle;
import at.petra_k.hexcasting.common.item.ItemSlate;
import at.petra_k.hexcasting.common.item.ItemImpetus;
import at.petra_k.hexcasting.common.item.ItemAkashicBookshelf;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemDoor;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Generic 1.12.2 registry for Hex blocks pending specialized behavior ports. */
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID)
public final class HexBlocks {
    private static final String[] BLOCK_IDS = new String[] {
        "slate",
        "akashic_record",
        "conjured_block",
        "conjured_light",
        "akashic_bookshelf",
        "akashic_connector",
        "amethyst_bricks",
        "amethyst_bricks_small",
        "amethyst_dust_block",
        "amethyst_edified_leaves",
        "amethyst_pillar",
        "amethyst_sconce",
        "amethyst_tiles",
        "ancient_scroll_paper",
        "ancient_scroll_paper_lantern",
        "aventurine_edified_leaves",
        "citrine_edified_leaves",
        "directrix/boolean",
        "directrix/empty",
        "directrix/redstone",
        "great_impetus",
        "edified_button",
        "edified_door",
        "edified_fence",
        "edified_fence_gate",
        "edified_log",
        "edified_log_amethyst",
        "edified_log_aventurine",
        "edified_log_citrine",
        "edified_log_purple",
        "edified_panel",
        "edified_planks",
        "edified_pressure_plate",
        "edified_slab",
        "edified_stairs",
        "edified_tile",
        "edified_trapdoor",
        "edified_wood",
        "impetus/empty",
        "impetus/look",
        "impetus/redstone",
        "impetus/rightclick",
        "spell_circle",
        "quenched_allay",
        "quenched_allay_bricks",
        "quenched_allay_bricks_small",
        "quenched_allay_tiles",
        "scroll_paper",
        "scroll_paper_lantern",
        "slate_amethyst_bricks",
        "slate_amethyst_bricks_small",
        "slate_amethyst_pillar",
        "slate_amethyst_tiles",
        "slate_block",
        "slate_bricks",
        "slate_bricks_small",
        "slate_pillar",
        "slate_tiles",
        "stripped_edified_log",
        "stripped_edified_wood",
    };
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    private static final Map<String, Item> BLOCK_ITEMS = new LinkedHashMap<>();

    static {
        for (String id : BLOCK_IDS) {
            Block block;
            if ("slate".equals(id)) {
                block = new BlockSlate();
            } else if ("akashic_record".equals(id)) {
                block = new BlockAkashicRecord();
            } else if ("akashic_bookshelf".equals(id)) {
                block = new BlockAkashicBookshelf();
            } else if ("akashic_connector".equals(id)) {
                block = new BlockAkashicConnector();
            } else if ("amethyst_edified_leaves".equals(id)
                || "aventurine_edified_leaves".equals(id)
                || "citrine_edified_leaves".equals(id)) {
                block = new BlockHexLeaves();
            } else if ("amethyst_sconce".equals(id)) {
                block = new BlockHexSconce();
            } else if (id.endsWith("_lantern")) {
                block = new BlockHexLight(id);
            } else if (id.startsWith("impetus/")) {
                BlockImpetus.TriggerMode mode = id.endsWith("/redstone")
                    ? BlockImpetus.TriggerMode.REDSTONE
                    : id.endsWith("/look")
                        ? BlockImpetus.TriggerMode.LOOK
                        : id.endsWith("/empty")
                            ? BlockImpetus.TriggerMode.EMPTY
                            : BlockImpetus.TriggerMode.RIGHT_CLICK;
                block = new BlockImpetus(mode);
            } else if ("directrix/empty".equals(id)) {
                block = new BlockEmptyDirectrix();
            } else if ("directrix/boolean".equals(id)) {
                block = new BlockBooleanDirectrix();
            } else if ("directrix/redstone".equals(id)) {
                block = new BlockRedstoneDirectrix();
            } else if ("great_impetus".equals(id)) {
                block = new BlockGreatImpetus();
            } else if ("spell_circle".equals(id)) {
                block = new BlockSpellCircle();
            } else if ("conjured_light".equals(id)) {
                block = new BlockConjuredLight();
            } else if ("conjured_block".equals(id)) {
                block = new BlockConjured();
            } else if ("amethyst_dust_block".equals(id)) {
                block = new BlockHexFalling();
            } else if ("slate_pillar".equals(id)
                || "slate_amethyst_pillar".equals(id)) {
                block = new BlockHexRotated();
            } else if ("edified_door".equals(id)) {
                block = new BlockHexDoor();
            } else if ("edified_trapdoor".equals(id)) {
                block = new BlockHexTrapdoor();
            } else if ("edified_fence".equals(id)) {
                block = new BlockHexFence();
            } else if ("edified_fence_gate".equals(id)) {
                block = new BlockHexFenceGate();
            } else if ("edified_button".equals(id)) {
                block = new BlockHexButton();
            } else if ("edified_pressure_plate".equals(id)) {
                block = new BlockHexPressurePlate();
            } else if ("edified_slab".equals(id)) {
                block = new BlockHexSlab();
            } else if ("edified_stairs".equals(id)) {
                Block edifiedPlanks = BLOCKS.get("edified_planks");
                block = new BlockHexStairs(edifiedPlanks == null
                    ? Blocks.PLANKS.getDefaultState() : edifiedPlanks.getDefaultState());
            } else if ("amethyst_pillar".equals(id)) {
                block = new BlockHexPillar();
            } else if ("edified_log".equals(id)
                || "edified_log_amethyst".equals(id)
                || "edified_log_aventurine".equals(id)
                || "edified_log_citrine".equals(id)
                || "edified_log_purple".equals(id)
                || "stripped_edified_log".equals(id)
                || "edified_wood".equals(id)
                || "stripped_edified_wood".equals(id)) {
                block = new BlockHexLog();
            } else {
                block = new BlockHexDecorative(id);
            }
            block.setRegistryName(HexAPI.MOD_ID, id)
                .setUnlocalizedName(HexAPI.MOD_ID + "." + id)
                .setCreativeTab(HexCreativeTab.HEX);
            BLOCKS.put(id, block);
        }
    }

    private HexBlocks() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        BLOCKS.values().forEach(event.getRegistry()::register);
    }

    @SubscribeEvent
    public static void registerBlockItems(RegistryEvent.Register<Item> event) {
        for (Map.Entry<String, Block> entry : BLOCKS.entrySet()) {
            Block block = entry.getValue();
            Item item;
            if ("slate".equals(entry.getKey())) {
                item = new ItemSlate(block);
            } else if ("edified_door".equals(entry.getKey())) {
                item = new ItemDoor(block);
            } else if (entry.getKey().startsWith("impetus/") || "great_impetus".equals(entry.getKey())) {
                item = new ItemImpetus(block);
            } else if ("akashic_bookshelf".equals(entry.getKey())) {
                item = new ItemAkashicBookshelf(block);
            } else {
                item = new ItemBlock(block);
            }
            item.setRegistryName(block.getRegistryName());
            item.setUnlocalizedName(block.getUnlocalizedName());
            item.setCreativeTab(HexCreativeTab.HEX);
            event.getRegistry().register(item);
            BLOCK_ITEMS.put(entry.getKey(), item);
        }
    }

    public static Collection<Item> blockItems() {
        return Collections.unmodifiableCollection(BLOCK_ITEMS.values());
    }

    /** Return the registered item form of a Hex block for client model predicates. */
    public static Item getBlockItem(String id) {
        return BLOCK_ITEMS.get(id);
    }
}
