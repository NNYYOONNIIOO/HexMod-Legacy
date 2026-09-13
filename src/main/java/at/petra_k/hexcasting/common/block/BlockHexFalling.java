package at.petra_k.hexcasting.common.block;

import net.minecraft.block.BlockFalling;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

/** Amethyst dust block with vanilla falling behavior. */
public final class BlockHexFalling extends BlockFalling {
    public BlockHexFalling() {
        super(Material.SAND);
        setHardness(0.5F);
        setResistance(0.5F);
        setSoundType(SoundType.SAND);
        setHarvestLevel("shovel", 0);
    }
}
