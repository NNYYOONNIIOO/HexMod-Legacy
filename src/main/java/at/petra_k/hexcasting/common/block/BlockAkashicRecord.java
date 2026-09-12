package at.petra_k.hexcasting.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/** Wooden storage block for the Akashic record system. */
public final class BlockAkashicRecord extends Block {
    public BlockAkashicRecord() {
        super(Material.WOOD);
        setHardness(2.0F);
        setResistance(5.0F);
    }
}

