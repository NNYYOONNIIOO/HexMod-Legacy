package at.petra_k.hexcasting.common.block;

import at.petra_k.hexcasting.api.casting.circles.ICircleComponent;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Shared 1.12.2 block behavior for components of a spell circle. */
public abstract class BlockCircleComponent extends Block
    implements ICircleComponent {
    public static final PropertyBool ENERGIZED = PropertyBool.create("energized");

    protected BlockCircleComponent(Material material) {
        super(material);
    }

    @Override
    public IBlockState startEnergized(BlockPos pos, IBlockState state, World world) {
        IBlockState newState = state.withProperty(ENERGIZED, true);
        if (!newState.equals(state)) {
            world.setBlockState(pos, newState, 3);
        }
        return newState;
    }

    @Override
    public boolean isEnergized(BlockPos pos, IBlockState state, World world) {
        return state.getValue(ENERGIZED);
    }

    @Override
    public IBlockState endEnergized(BlockPos pos, IBlockState state, World world) {
        IBlockState newState = state.withProperty(ENERGIZED, false);
        if (!newState.equals(state)) {
            world.setBlockState(pos, newState, 3);
        }
        return newState;
    }

    /** Safe normal used by particle and diagnostic callers. */
    public EnumFacing normalDir(BlockPos pos, IBlockState state, World world) {
        return EnumFacing.UP;
    }

    public float particleHeight(BlockPos pos, IBlockState state, World world) {
        return 0.5F;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world,
                                          BlockPos pos) {
        return state.getValue(ENERGIZED) ? 15 : 0;
    }
}
