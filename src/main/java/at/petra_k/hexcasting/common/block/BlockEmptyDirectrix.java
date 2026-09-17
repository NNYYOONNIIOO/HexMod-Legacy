package at.petra_k.hexcasting.common.block;

import at.petra_k.hexcasting.api.casting.circles.ICircleComponent;
import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Collections;

/** Empty directrix: a pass-through control-flow component placeholder. */
public final class BlockEmptyDirectrix extends BlockDirectrixBase {
    public BlockEmptyDirectrix() {
        super();
        setDefaultState(blockState.getBaseState()
            .withProperty(FACING, EnumFacing.UP)
            .withProperty(ENERGIZED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, ENERGIZED);
    }

    @Override
    public ICircleComponent.ControlFlow acceptControlFlow(CastingVM image,
        EnumFacing enterDirection, BlockPos pos, IBlockState state, World world) {
        EnumFacing facing = state.getValue(FACING);
        EnumFacing output = world.rand.nextBoolean() ? facing : facing.getOpposite();
        return new ICircleComponent.Continue(image,
            Collections.singletonList(exitPositionFromDirection(pos, output)));
    }
}
