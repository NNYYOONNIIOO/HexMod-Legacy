package at.petra_k.hexcasting.common.block;

import net.minecraft.block.BlockStairs;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;

/** Stair block backed by the edified wood properties. */
public final class BlockHexStairs extends BlockStairs {
    public BlockHexStairs(IBlockState modelState) {
        super(modelState);
        setHardness(2.0F);
        setResistance(3.0F);
        setSoundType(SoundType.WOOD);
        setHarvestLevel("axe", 0);
    }
}
