package at.petra_k.hexcasting.common.block;

import net.minecraft.block.BlockFence;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MapColor;

/** Fence with vanilla connection properties and collision behavior. */
public final class BlockHexFence extends BlockFence {
    public BlockHexFence() {
        super(Material.WOOD, MapColor.WOOD);
        setHardness(2.0F);
        setResistance(3.0F);
        setSoundType(SoundType.WOOD);
        setHarvestLevel("axe", 0);
    }
}
