package at.petra_k.hexcasting.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

/**
 * A passive Akashic network bridge. It has no tile entity or interaction;
 * Akashic records use it as a traversable link between shelves, matching the
 * upstream ligature's deliberately inert behavior.
 */
public final class BlockAkashicConnector extends Block {
    public BlockAkashicConnector() {
        super(Material.WOOD);
        setHardness(2.0F);
        setResistance(5.0F);
        setSoundType(SoundType.WOOD);
        setHarvestLevel("axe", 0);
        setLightLevel(4.0F / 15.0F);
    }
}
