package at.petra_k.hexcasting.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;

/** Small non-solid light source used by Hex lantern and sconce blocks. */
public final class BlockHexLight extends Block {
    private static final AxisAlignedBB FULL_BOX =
        new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    private static final AxisAlignedBB LANTERN_BOX =
        new AxisAlignedBB(0.1875D, 0.0D, 0.1875D, 0.8125D, 0.875D, 0.8125D);
    private static final AxisAlignedBB SCONCE_BOX =
        new AxisAlignedBB(0.25D, 0.25D, 0.25D, 0.75D, 1.0D, 0.75D);

    private final boolean sconce;
    private final boolean paperLantern;

    public BlockHexLight(String id) {
        super(materialFor(id));
        this.sconce = id != null && id.endsWith("sconce");
        this.paperLantern = isPaperId(id);
        setHardness(paperLantern ? 0.0F : 0.8F);
        setResistance(paperLantern ? 0.0F : 2.0F);
        setSoundType(paperLantern ? SoundType.PLANT : SoundType.GLASS);
        setLightLevel(id != null && id.contains("ancient_scroll_paper_lantern")
            ? (12.0F / 15.0F) : 1.0F);
        setLightOpacity(0);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        return isPaperLantern() ? FULL_BOX : (sconce ? SCONCE_BOX : LANTERN_BOX);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        return isPaperLantern() ? FULL_BOX : (sconce ? SCONCE_BOX : LANTERN_BOX);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    private boolean isPaperLantern() {
        return paperLantern;
    }

    private static Material materialFor(String id) {
        return isPaperId(id) ? Material.CLOTH : Material.GLASS;
    }

    private static boolean isPaperId(String id) {
        return id != null && id.contains("paper");
    }

    @Override
    public boolean isFlammable(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return isPaperLantern();
    }

    @Override
    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public net.minecraft.util.BlockRenderLayer getBlockLayer() {
        return net.minecraft.util.BlockRenderLayer.CUTOUT;
    }

    @Override
    public int getFlammability(net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos, net.minecraft.util.EnumFacing face) {
        return isPaperLantern() ? 100 : 0;
    }

    @Override
    public int getFireSpreadSpeed(net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos, net.minecraft.util.EnumFacing face) {
        return isPaperLantern() ? 60 : 0;
    }

}
