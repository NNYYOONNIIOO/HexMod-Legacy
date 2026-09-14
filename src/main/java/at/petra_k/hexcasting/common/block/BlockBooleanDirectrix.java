package at.petra_k.hexcasting.common.block;

import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.util.EnumFacing;

/** Boolean directrix: its typed branch behavior is supplied by circle execution. */
public final class BlockBooleanDirectrix extends BlockDirectrixBase {
    public BlockBooleanDirectrix() {
        super();
        setDefaultState(blockState.getBaseState()
            .withProperty(FACING, EnumFacing.UP)
            .withProperty(ENERGIZED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, ENERGIZED);
    }
}
