package at.petra_k.hexcasting.common.block;

import net.minecraft.block.BlockDoor;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

/** Door with vanilla two-block placement and open/hinge/power behavior. */
public final class BlockHexDoor extends BlockDoor {
    public BlockHexDoor() {
        super(Material.WOOD);
        setHardness(3.0F);
        setResistance(3.0F);
        setSoundType(SoundType.WOOD);
        setHarvestLevel("axe", 0);
    }
}
