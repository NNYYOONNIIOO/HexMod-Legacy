package at.petra_k.hexcasting.common.block;

import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

/** Trapdoor with vanilla half/open/facing state and collision behavior. */
public final class BlockHexTrapdoor extends BlockTrapDoor {
    public BlockHexTrapdoor() {
        super(Material.WOOD);
        setHardness(3.0F);
        setResistance(3.0F);
        setSoundType(SoundType.WOOD);
        setHarvestLevel("axe", 0);
    }
}
