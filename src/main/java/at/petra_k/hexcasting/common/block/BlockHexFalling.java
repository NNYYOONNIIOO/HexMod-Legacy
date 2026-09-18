package at.petra_k.hexcasting.common.block;

import net.minecraft.block.BlockFalling;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Amethyst dust block with vanilla falling behavior. */
public final class BlockHexFalling extends BlockFalling {
    public BlockHexFalling() {
        super(Material.SAND);
        setHardness(0.5F);
        setResistance(0.5F);
        setSoundType(SoundType.SAND);
        setHarvestLevel("shovel", 0);
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess world,
                                BlockPos pos) {
        return MapColor.PURPLE;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getDustColor(IBlockState state) {
        return 0xFFB38EF3;
    }
}
