package at.petra_k.hexcasting.common.block;

import net.minecraft.block.BlockSlab;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.properties.IProperty;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Single wood slab with vanilla half-state, collision, and placement behavior. */
public final class BlockHexSlab extends BlockSlab {
    public BlockHexSlab() {
        super(Material.WOOD);
        setHardness(2.0F);
        setResistance(3.0F);
        setSoundType(SoundType.WOOD);
        setDefaultState(blockState.getBaseState().withProperty(HALF, EnumBlockHalf.BOTTOM));
    }

    /**
     * Keep the half property present while BlockSlab's superclass constructor
     * creates the state container.  The 1.12.2 Cleanroom/Forge combination can
     * otherwise resolve the container as an empty vanilla block state during
     * static registration.
     */
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, HALF);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(HALF,
            (meta & 8) == 0 ? EnumBlockHalf.BOTTOM : EnumBlockHalf.TOP);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(HALF) == EnumBlockHalf.TOP ? 8 : 0;
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
        IBlockState state = getDefaultState();
        if (this.isDouble()) {
            return state;
        }
        if (facing == EnumFacing.DOWN || (facing != EnumFacing.UP && hitY > 0.5F)) {
            return state.withProperty(HALF, EnumBlockHalf.TOP);
        }
        return state.withProperty(HALF, EnumBlockHalf.BOTTOM);
    }

}
