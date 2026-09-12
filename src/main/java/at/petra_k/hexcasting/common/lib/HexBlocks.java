package at.petra_k.hexcasting.common.lib;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.common.block.BlockConjured;
import at.petra_k.hexcasting.common.block.BlockConjuredLight;
import at.petra_k.hexcasting.common.block.BlockAkashicRecord;
import at.petra_k.hexcasting.common.block.BlockGreatImpetus;
import at.petra_k.hexcasting.common.block.BlockImpetus;
import at.petra_k.hexcasting.common.block.BlockSlate;
import at.petra_k.hexcasting.common.block.BlockSpellCircle;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
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
        "impetus",
        "great_impetus",
        "spell_circle",
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
            } else if ("impetus".equals(id)) {
                block = new BlockImpetus();
            } else if ("great_impetus".equals(id)) {
                block = new BlockGreatImpetus();
            } else if ("spell_circle".equals(id)) {
                block = new BlockSpellCircle();
            } else if ("conjured_light".equals(id)) {
                block = new BlockConjuredLight();
            } else if ("conjured_block".equals(id)) {
                block = new BlockConjured();
            } else {
                block = new Block(Material.ROCK)
                    .setHardness(2.0F)
                    .setResistance(6.0F);
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
            ItemBlock item = new ItemBlock(block);
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
}
