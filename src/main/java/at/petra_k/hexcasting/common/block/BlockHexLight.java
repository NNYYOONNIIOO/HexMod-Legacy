package at.petra_k.hexcasting.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;

/** Small non-solid light source used by Hex lantern and sconce blocks. */
public final class BlockHexLight extends Block {
    private static final AxisAlignedBB LANTERN_BOX =
        new AxisAlignedBB(0.1875D, 0.0D, 0.1875D, 0.8125D, 0.875D, 0.8125D);
    private static final AxisAlignedBB SCONCE_BOX =
        new AxisAlignedBB(0.25D, 0.25D, 0.25D, 0.75D, 1.0D, 0.75D);

    private final boolean sconce;

    public BlockHexLight(String id) {
        super(Material.GLASS);
        this.sconce = id != null && id.endsWith("sconce");
        setHardness(0.8F);
        setResistance(2.0F);
        setSoundType(SoundType.GLASS);
        setLightLevel(1.0F);
        setLightOpacity(0);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        return sconce ? SCONCE_BOX : LANTERN_BOX;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }
}
