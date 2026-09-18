package at.petra_k.hexcasting.common.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Directional amethyst sconce matching Hex's six-faced fixture. */
public final class BlockHexSconce extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");
    /** 1.12 has no native waterlogged interface, so retain the fluid bit in the blockstate. */
    public static final PropertyBool WATERLOGGED = PropertyBool.create("waterlogged");

    private static final int[] PARTICLE_COLORS = {
        0x6F4FAB, 0xB38EF3, 0xCFA0F3, 0xCFA0F3, 0xFFFDD5
    };

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
        setDefaultState(blockState.getBaseState()
            .withProperty(FACING, EnumFacing.UP)
            .withProperty(WATERLOGGED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, WATERLOGGED);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.getFront(meta & 7);
        return getDefaultState()
            .withProperty(FACING, facing == null ? EnumFacing.UP : facing)
            .withProperty(WATERLOGGED, (meta & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex()
            | (state.getValue(WATERLOGGED) ? 8 : 0);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, facing)
            .withProperty(WATERLOGGED,
                world.getBlockState(pos).getMaterial() == Material.WATER);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        EnumFacing facing = state.getValue(FACING);
        if (facing == EnumFacing.UP) {
            return UP_BOX;
        }
        if (facing == EnumFacing.DOWN) {
            return DOWN_BOX;
        }
        if (facing == EnumFacing.NORTH) {
            return NORTH_BOX;
        }
        if (facing == EnumFacing.SOUTH) {
            return SOUTH_BOX;
        }
        if (facing == EnumFacing.WEST) {
            return WEST_BOX;
        }
        if (facing == EnumFacing.EAST) {
            return EAST_BOX;
        }
        return UP_BOX;
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
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos,
                                  Random random) {
        if (random.nextFloat() >= 0.8F) {
            return;
        }

        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        EnumFacing facing = state.getValue(FACING);
        double motionX;
        double motionY;
        double motionZ;
        switch (facing) {
            case EAST:
                motionX = triangle(random, 0.01D, 0.05D);
                break;
            case WEST:
                motionX = triangle(random, -0.01D, -0.05D);
                break;
            default:
                motionX = triangle(random, -0.01D, 0.01D);
                break;
        }
        switch (facing) {
            case UP:
                motionY = triangle(random, 0.01D, 0.05D);
                break;
            case DOWN:
                motionY = triangle(random, -0.01D, -0.05D);
                break;
            default:
                motionY = triangle(random, -0.01D, 0.01D);
                break;
        }
        switch (facing) {
            case SOUTH:
                motionZ = triangle(random, 0.01D, 0.05D);
                break;
            case NORTH:
                motionZ = triangle(random, -0.01D, -0.05D);
                break;
            default:
                motionZ = triangle(random, -0.01D, 0.01D);
                break;
        }

        TileEntityConjured.spawnClientParticle(world, centerX, centerY, centerZ,
            motionX, motionY, motionZ,
            PARTICLE_COLORS[random.nextInt(PARTICLE_COLORS.length)]);
        if (random.nextFloat() < 0.08F) {
            world.playSound(centerX, centerY, centerZ,
                SoundEvents.BLOCK_NOTE_CHIME, SoundCategory.BLOCKS, 1.0F,
                0.5F + random.nextFloat() * 1.2F, false);
        }
    }

    private static double triangle(Random random, double mean, double deviation) {
        return mean + (random.nextDouble() + random.nextDouble() - 1.0D)
            * deviation;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }
}
