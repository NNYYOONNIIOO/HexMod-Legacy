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
            .withProperty(POWERED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, POWERED);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState()
            .withProperty(FACING, EnumFacing.getFront(meta & 7))
            .withProperty(POWERED, (meta & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex()
            | (state.getValue(POWERED) ? 8 : 0);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos,
                                Block blockIn, BlockPos fromPos) {
        super.neighborChanged(state, world, pos, blockIn, fromPos);
        if (world.isAirBlock(pos)) {
            return;
        }
        boolean powered = world.isBlockPowered(pos);
        if (powered != state.getValue(POWERED)) {
            world.setBlockState(pos, state.withProperty(POWERED, powered), 3);
        }
    }
}
