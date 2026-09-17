package at.petra_k.hexcasting.common.block;

import at.petra_k.hexcasting.api.casting.circles.ICircleComponent;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;
import at.petra_k.hexcasting.api.casting.iota.BooleanIota;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.Locale;

/** Boolean directrix: its typed branch behavior is supplied by circle execution. */
public final class BlockBooleanDirectrix extends BlockDirectrixBase {
    public static final PropertyEnum<State> STATE = PropertyEnum.create("state", State.class);

    public BlockBooleanDirectrix() {
        super();
        setDefaultState(blockState.getBaseState()
            .withProperty(FACING, EnumFacing.UP)
            .withProperty(STATE, State.NEITHER)
            .withProperty(ENERGIZED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, STATE, ENERGIZED);
    }

    @Override
    public ICircleComponent.ControlFlow acceptControlFlow(CastingVM image,
        EnumFacing enterDirection, BlockPos pos, IBlockState state, World world) {
        BooleanIota value;
        try {
            value = image.getStack().pop(BooleanIota.class);
        } catch (CastingException ignored) {
            return new ICircleComponent.Stop();
        }
        boolean truth = value.getValue();
        world.setBlockState(pos, state.withProperty(STATE,
            truth ? State.TRUE : State.FALSE), 3);
        EnumFacing output = truth ? state.getValue(FACING).getOpposite()
            : state.getValue(FACING);
        return new ICircleComponent.Continue(image,
            Collections.singletonList(exitPositionFromDirection(pos, output)));
    }

    @Override
    public IBlockState endEnergized(BlockPos pos, IBlockState state, World world) {
        return super.endEnergized(pos, state.withProperty(STATE, State.NEITHER), world);
    }

    public enum State implements net.minecraft.util.IStringSerializable {
        NEITHER, TRUE, FALSE;

        @Override
        public String getName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
