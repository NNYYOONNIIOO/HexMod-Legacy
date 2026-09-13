package at.petra_k.hexcasting.common.block;

import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

/** Edified log/wood blocks with the vanilla axis state and log behavior. */
public final class BlockHexLog extends BlockRotatedPillar {
    public BlockHexLog() {
        super(Material.WOOD);
        setHardness(2.0F);
        setResistance(3.0F);
        setSoundType(SoundType.WOOD);
        setHarvestLevel("axe", 0);
    }
}
