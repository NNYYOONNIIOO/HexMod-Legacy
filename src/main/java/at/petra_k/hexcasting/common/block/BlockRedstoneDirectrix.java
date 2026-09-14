package at.petra_k.hexcasting.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Redstone directrix with a synchronized powered input state. */
public final class BlockRedstoneDirectrix extends BlockDirectrixBase {
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public BlockRedstoneDirectrix() {
        super();
        setDefaultState(blockState.getBaseState()
            .withProperty(FACING, EnumFacing.UP)
            .withProperty(POWERED, false)
            .withProperty(ENERGIZED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, POWERED, ENERGIZED);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState()
            .withProperty(FACING, EnumFacing.getFront(meta & 7))
            .withProperty(POWERED, false)
            .withProperty(ENERGIZED, (meta & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex()
            | (state.getValue(ENERGIZED) ? 8 : 0);
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        super.onBlockAdded(world, pos, state);
        updatePowered(world, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos,
                                Block blockIn, BlockPos fromPos) {
        super.neighborChanged(state, world, pos, blockIn, fromPos);
        updatePowered(world, pos);
    }

    private static void updatePowered(World world, BlockPos pos) {
        IBlockState current = world.getBlockState(pos);
        if (!(current.getBlock() instanceof BlockRedstoneDirectrix)) {
            return;
        }
        boolean powered = world.isBlockPowered(pos);
        if (powered != current.getValue(POWERED)) {
            world.setBlockState(pos, current.withProperty(POWERED, powered), 3);
        }
    }
}
