package at.petra_k.hexcasting.common.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/** An invisible, non-colliding light source created by Hex Casting. */
public final class BlockConjuredLight extends BlockConjured {
    public BlockConjuredLight() {
        super(Material.AIR);
        setLightLevel(1.0F);
        setLightOpacity(0);
        setBlockUnbreakable();
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return null;
    }

    @Override
    public int quantityDropped(java.util.Random random) {
        return 0;
    }

    /** Conjured light follows the same temporary lifetime as conjured blocks. */
    @Override
    public boolean hasTileEntity(net.minecraft.block.state.IBlockState state) {
        return true;
    }

    @Override
    public net.minecraft.tileentity.TileEntity createTileEntity(
            net.minecraft.world.World world,
            net.minecraft.block.state.IBlockState state) {
        return new TileEntityConjured();
    }
}
