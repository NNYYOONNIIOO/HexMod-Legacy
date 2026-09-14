package at.petra_k.hexcasting.common.block;

import net.minecraft.block.BlockSlab;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** Single wood slab with vanilla half-state, collision, and placement behavior. */
public final class BlockHexSlab extends BlockSlab {
    public static final PropertyBool DOUBLE = PropertyBool.create("double");

    public BlockHexSlab() {
        super(Material.WOOD);
        setHardness(2.0F);
        setResistance(3.0F);
        setSoundType(SoundType.WOOD);
        setDefaultState(blockState.getBaseState()
            .withProperty(HALF, EnumBlockHalf.BOTTOM)
            .withProperty(DOUBLE, false));
    }

    /**
     * Keep the half property present while BlockSlab's superclass constructor
     * creates the state container.  The 1.12.2 Cleanroom/Forge combination can
     * otherwise resolve the container as an empty vanilla block state during
     * static registration.
     */
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, HALF, DOUBLE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(HALF,
            (meta & 8) == 0 ? EnumBlockHalf.BOTTOM : EnumBlockHalf.TOP)
            .withProperty(DOUBLE, (meta & 4) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(HALF) == EnumBlockHalf.TOP ? 8 : 0;
        return state.getValue(DOUBLE) ? meta | 4 : meta;
    }

    @Override
    public String getUnlocalizedName(int meta) {
        return getUnlocalizedName();
    }

    @Override
    public boolean isDouble() {
        return false;
    }

    @Override
    public IProperty<?> getVariantProperty() {
        return HALF;
    }

    @Override
    public Comparable<?> getTypeForItem(ItemStack stack) {
        return EnumBlockHalf.BOTTOM;
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
            float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        IBlockState state = getDefaultState().withProperty(DOUBLE, false);
        if (this.isDouble()) {
            return state;
        }
        if (facing == EnumFacing.DOWN || (facing != EnumFacing.UP && hitY > 0.5F)) {
            return state.withProperty(HALF, EnumBlockHalf.TOP);
        }
        return state.withProperty(HALF, EnumBlockHalf.BOTTOM);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (state.getValue(DOUBLE)) {
            return new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        }
        return state.getValue(HALF) == EnumBlockHalf.TOP
            ? new AxisAlignedBB(0.0D, 0.5D, 0.0D, 1.0D, 1.0D, 1.0D)
            : new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return getBoundingBox(state, world, pos);
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return state.getValue(DOUBLE);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return state.getValue(DOUBLE);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return 0;
    }

}
