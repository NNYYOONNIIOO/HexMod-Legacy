package at.petra_k.hexcasting.common.block;

import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

/** Rock pillar with the vanilla X/Y/Z axis state. */
public final class BlockHexRotated extends BlockRotatedPillar {
    public BlockHexRotated() {
        super(Material.ROCK);
        setHardness(3.0F);
        setResistance(12.0F);
        setSoundType(SoundType.STONE);
        setHarvestLevel("pickaxe", 0);
    }
}
