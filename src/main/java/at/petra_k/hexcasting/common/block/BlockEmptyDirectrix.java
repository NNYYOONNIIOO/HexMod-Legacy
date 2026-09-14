package at.petra_k.hexcasting.common.block;

import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.util.EnumFacing;

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
}
