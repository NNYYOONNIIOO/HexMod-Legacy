package at.petra_k.hexcasting.common.block;

import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.SoundType;

/** Fence gate with the vanilla facing/open/powered/in-wall state. */
public final class BlockHexFenceGate extends BlockFenceGate {
    public BlockHexFenceGate() {
        super(BlockPlanks.EnumType.OAK);
        setHardness(2.0F);
        setResistance(3.0F);
        setSoundType(SoundType.WOOD);
        setHarvestLevel("axe", 0);
    }
}
