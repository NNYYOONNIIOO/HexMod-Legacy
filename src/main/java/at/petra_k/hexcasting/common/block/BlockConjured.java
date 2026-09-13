package at.petra_k.hexcasting.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** The translucent temporary block created by Hex Casting. */
public class BlockConjured extends Block {
    public BlockConjured() {
        this(Material.GLASS);
    }

    protected BlockConjured(Material material) {
        super(material);
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

    /** The visual effect is supplied by the block entity, as in Hex's source. */
    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityConjured();
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile != null) {
            tile.invalidate();
        }
        super.breakBlock(world, pos, state);
    }
}
