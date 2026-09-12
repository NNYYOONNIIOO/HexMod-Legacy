package at.petra_k.hexcasting.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;

/** The translucent temporary block created by Hex Casting. */
public final class BlockConjured extends Block {
    public BlockConjured() {
        super(Material.GLASS);
        setHardness(0.3F);
        setResistance(0.3F);
        setLightOpacity(0);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }
}

