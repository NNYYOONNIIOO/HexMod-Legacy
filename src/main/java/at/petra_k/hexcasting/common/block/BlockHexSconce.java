package at.petra_k.hexcasting.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Directional amethyst sconce matching Hex's six-faced fixture. */
public final class BlockHexSconce extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    private static final AxisAlignedBB UP_BOX = new AxisAlignedBB(
        4.0D / 16.0D, 0.0D, 4.0D / 16.0D,
        12.0D / 16.0D, 1.0D / 16.0D, 12.0D / 16.0D);
    private static final AxisAlignedBB DOWN_BOX = new AxisAlignedBB(
        4.0D / 16.0D, 15.0D / 16.0D, 4.0D / 16.0D,
        12.0D / 16.0D, 1.0D, 12.0D / 16.0D);
    private static final AxisAlignedBB NORTH_BOX = new AxisAlignedBB(
        4.0D / 16.0D, 4.0D / 16.0D, 15.0D / 16.0D,
        12.0D / 16.0D, 12.0D / 16.0D, 1.0D);
    private static final AxisAlignedBB SOUTH_BOX = new AxisAlignedBB(
        4.0D / 16.0D, 4.0D / 16.0D, 0.0D,
        12.0D / 16.0D, 12.0D / 16.0D, 1.0D / 16.0D);
    private static final AxisAlignedBB WEST_BOX = new AxisAlignedBB(
        15.0D / 16.0D, 4.0D / 16.0D, 4.0D / 16.0D,
        1.0D, 12.0D / 16.0D, 12.0D / 16.0D);
    private static final AxisAlignedBB EAST_BOX = new AxisAlignedBB(
        0.0D, 4.0D / 16.0D, 4.0D / 16.0D,
        1.0D / 16.0D, 12.0D / 16.0D, 12.0D / 16.0D);

    public BlockHexSconce() {
        super(Material.ROCK);
        setHardness(1.0F);
        setResistance(2.0F);
        setSoundType(SoundType.STONE);
        setHarvestLevel("pickaxe", 0);
        setLightLevel(1.0F);
        setLightOpacity(0);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.getFront(meta & 7);
        return getDefaultState().withProperty(FACING, facing == null ? EnumFacing.UP : facing);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, facing);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        switch (state.getValue(FACING)) {
            case DOWN: return DOWN_BOX;
            case NORTH: return NORTH_BOX;
            case SOUTH: return SOUTH_BOX;
            case WEST: return WEST_BOX;
            case EAST: return EAST_BOX;
            case UP:
            default: return UP_BOX;
        }
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return getBoundingBox(state, world, pos);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }
}
