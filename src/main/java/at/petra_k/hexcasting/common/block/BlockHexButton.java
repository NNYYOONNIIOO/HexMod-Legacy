package at.petra_k.hexcasting.common.block;

import net.minecraft.block.BlockButtonWood;
import net.minecraft.block.SoundType;

/** Wood button using vanilla facing, press duration, and redstone behavior. */
public final class BlockHexButton extends BlockButtonWood {
    public BlockHexButton() {
        super();
        setHardness(0.5F);
        setResistance(1.0F);
        setSoundType(SoundType.WOOD);
        setHarvestLevel("axe", 0);
    }
}
