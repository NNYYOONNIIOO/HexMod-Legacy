package at.petra_k.hexcasting.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;

/** Thin, non-opaque spell-circle surface. */
public final class BlockSpellCircle extends Block {
    public BlockSpellCircle() {
        super(Material.CLOTH);
        setHardness(0.2F);
        setResistance(0.2F);
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

