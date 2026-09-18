package at.petra_k.hexcasting.common.block;

import net.minecraft.block.BlockButtonWood;
import net.minecraft.block.SoundType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/** Wood button using vanilla facing, press duration, and redstone behavior. */
public final class BlockHexButton extends BlockButtonWood {
    public BlockHexButton() {
        super();
        setHardness(0.5F);
        setResistance(1.0F);
        setSoundType(SoundType.WOOD);
        setHarvestLevel("axe", 0);
    }

    @Override
    public boolean isFlammable(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return true;
    }

    @Override
    public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return 5;
    }

    @Override
    public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return 20;
    }
}
