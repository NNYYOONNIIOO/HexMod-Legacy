package at.petra_k.hexcasting.common.lib;

import at.petra_k.hexcasting.api.HexAPI;
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
        "spell_circle"
    };
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    private static final Map<String, Item> BLOCK_ITEMS = new LinkedHashMap<>();

    static {
        for (String id : BLOCK_IDS) {
            Block block = new Block(Material.ROCK)
                .setRegistryName(HexAPI.MOD_ID, id)
                .setUnlocalizedName(HexAPI.MOD_ID + "." + id)
                .setHardness(2.0F)
                .setResistance(6.0F)
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
