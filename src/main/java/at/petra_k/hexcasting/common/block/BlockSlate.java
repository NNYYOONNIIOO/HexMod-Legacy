package at.petra_k.hexcasting.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/** Carved slate used by Hex Casting circle structures. */
public final class BlockSlate extends Block {
    public BlockSlate() {
        super(Material.ROCK);
        setHardness(1.5F);
        setResistance(6.0F);
    }
}

