package at.petra_k.hexcasting.common.block;

import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

/** Wood pressure plate using vanilla entity activation and redstone output. */
public final class BlockHexPressurePlate extends BlockPressurePlate {
    public BlockHexPressurePlate() {
        super(Material.WOOD, Sensitivity.EVERYTHING);
        setHardness(0.5F);
        setResistance(1.0F);
        setSoundType(SoundType.WOOD);
        setHarvestLevel("axe", 0);
    }
}
