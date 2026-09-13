package at.petra_k.hexcasting.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** Shared face-attached geometry for the three Hex directrix variants. */
abstract class BlockDirectrixBase extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");
    private static final double THICKNESS = 1.0D / 16.0D;

    protected BlockDirectrixBase() {
        super(Material.CLOTH);
        setHardness(0.2F);
        setResistance(0.2F);
        setLightOpacity(0);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING,
            EnumFacing.getFront(meta % EnumFacing.values().length));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, facing);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        for (EnumFacing facing : EnumFacing.values()) {
            if (hasSupport(world, pos, facing)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos,
                                Block blockIn, BlockPos fromPos) {
        if (!hasSupport(world, pos, state.getValue(FACING))) {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
        super.neighborChanged(state, world, pos, blockIn, fromPos);
    }

    private static boolean hasSupport(World world, BlockPos pos, EnumFacing normal) {
        BlockPos supportPos = pos.offset(normal.getOpposite());
        return world.getBlockState(supportPos).isSideSolid(world, supportPos, normal);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        switch (state.getValue(FACING)) {
            case DOWN:
                return new AxisAlignedBB(0.0D, 1.0D - THICKNESS, 0.0D, 1.0D, 1.0D, 1.0D);
            case NORTH:
                return new AxisAlignedBB(0.0D, 0.0D, 1.0D - THICKNESS, 1.0D, 1.0D, 1.0D);
            case SOUTH:
                return new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, THICKNESS);
            case WEST:
                return new AxisAlignedBB(1.0D - THICKNESS, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
            case EAST:
                return new AxisAlignedBB(0.0D, 0.0D, 0.0D, THICKNESS, 1.0D, 1.0D);
            case UP:
            default:
                return new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, THICKNESS, 1.0D);
        }
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
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }
}
