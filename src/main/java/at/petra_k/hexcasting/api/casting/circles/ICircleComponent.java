package at.petra_k.hexcasting.api.casting.circles;

import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.List;

/**
 * A block that can carry the control flow of a spell circle.
 *
 * <p>The modern implementation calls this an image, while the 1.12.2 port
 * uses the already persistent {@link CastingVM} as the image.  Keeping the
 * control-flow contract independent from a particular block superclass lets
 * slates and the face-attached circle blocks share the same traversal code.</p>
 */
public interface ICircleComponent {
    /**
     * Execute this component and return the possible exits for the current
     * step.  A Stop result means that the component has already reported the
     * failure or intentionally terminates the circle.
     */
    ControlFlow acceptControlFlow(CastingVM image, EnumFacing enterDirection,
                                  BlockPos pos, IBlockState state, World world);

    /** Whether control flow may enter this block from the supplied direction. */
    boolean canEnterFromDirection(EnumFacing enterDirection, BlockPos pos,
                                  IBlockState state, World world);

    /**
     * All exits that are structurally possible.  This is used while finding
     * a closed circle, before a Boolean or redstone value chooses one branch.
     */
    EnumSet<EnumFacing> possibleExitDirections(BlockPos pos, IBlockState state,
                                               World world);

    /** Start the visual energized state for this component. */
    IBlockState startEnergized(BlockPos pos, IBlockState state, World world);

    /** Return whether the component is currently energized. */
    boolean isEnergized(BlockPos pos, IBlockState state, World world);

    /** Stop the visual energized state for this component. */
    IBlockState endEnergized(BlockPos pos, IBlockState state, World world);

    default Exit exitPositionFromDirection(BlockPos pos, EnumFacing direction) {
        return new Exit(pos.offset(direction), direction);
    }

    /** One candidate next block and the direction used to enter it. */
    final class Exit {
        private final BlockPos position;
        private final EnumFacing enterDirection;

        public Exit(BlockPos position, EnumFacing enterDirection) {
            this.position = position;
            this.enterDirection = enterDirection;
        }

        public BlockPos getPosition() {
            return position;
        }

        public EnumFacing getEnterDirection() {
            return enterDirection;
        }
    }

    /** Continue with a VM image and one or more candidate exits. */
    final class Continue extends ControlFlow {
        private final CastingVM image;
        private final List<Exit> exits;

        public Continue(CastingVM image, List<Exit> exits) {
            this.image = image;
            this.exits = exits;
        }

        public CastingVM getImage() {
            return image;
        }

        public List<Exit> getExits() {
            return exits;
        }
    }

    /** Stop the current execution. */
    final class Stop extends ControlFlow {
    }

    /** Marker superclass for the two control-flow outcomes. */
    abstract class ControlFlow {
    }
}
