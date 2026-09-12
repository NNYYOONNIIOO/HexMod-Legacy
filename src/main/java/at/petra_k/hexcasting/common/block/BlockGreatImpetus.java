package at.petra_k.hexcasting.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/** Reinforced impetus casing. */
public final class BlockGreatImpetus extends Block {
    public BlockGreatImpetus() {
        super(Material.IRON);
        setHardness(5.0F);
        setResistance(12.0F);
    }
}

