package at.petra_k.hexcasting.common.block;

import net.minecraft.block.BlockSlab;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.properties.IProperty;
import net.minecraft.item.ItemStack;

/** Single wood slab with vanilla half-state, collision, and placement behavior. */
public final class BlockHexSlab extends BlockSlab {
    public BlockHexSlab() {
        super(Material.WOOD);
        setHardness(2.0F);
        setResistance(3.0F);
        setSoundType(SoundType.WOOD);
        setDefaultState(blockState.getBaseState().withProperty(HALF, EnumBlockHalf.BOTTOM));
        setHarvestLevel("axe", 0);
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
}
