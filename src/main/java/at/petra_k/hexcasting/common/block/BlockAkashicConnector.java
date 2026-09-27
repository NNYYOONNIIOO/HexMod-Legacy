package at.petra_k.hexcasting.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A passive Akashic network bridge. It has no tile entity or interaction;
 * Akashic records use it as a traversable link between shelves, matching the
 * upstream ligature's deliberately inert behavior.
 */
public final class BlockAkashicConnector extends Block {
    public BlockAkashicConnector() {
        super(Material.WOOD);
        setHardness(3.0F);
        setResistance(4.0F);
        setSoundType(SoundType.WOOD);
        setHarvestLevel("axe", 0);
        setLightLevel(4.0F / 15.0F);
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
        return 5;
    }
}
