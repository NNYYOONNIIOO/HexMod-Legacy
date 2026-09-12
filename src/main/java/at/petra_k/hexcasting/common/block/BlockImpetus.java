package at.petra_k.hexcasting.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/** Ordinary impetus casing; tile-entity activation is ported separately. */
public final class BlockImpetus extends Block {
    public BlockImpetus() {
        super(Material.IRON);
        setHardness(3.0F);
        setResistance(10.0F);
    }
}

