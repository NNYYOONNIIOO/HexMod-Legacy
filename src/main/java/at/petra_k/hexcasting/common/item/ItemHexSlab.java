package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.common.block.BlockHexSlab;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockSlab.EnumBlockHalf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Item form of the edified slab; merging keeps the same registered block. */
public final class ItemHexSlab extends ItemBlock {
    private final BlockHexSlab slab;

    public ItemHexSlab(BlockHexSlab block) {
        super(block);
        this.slab = block;
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    @Override
    public int getMetadata(int damage) {
        return 0;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
                                      EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == slab
                && !state.getValue(BlockHexSlab.DOUBLE)
                && canMerge(state, facing, hitY)
                && player.canPlayerEdit(pos, facing, stack)) {
            IBlockState merged = state
                .withProperty(BlockHexSlab.DOUBLE, true)
                .withProperty(BlockSlab.HALF, EnumBlockHalf.BOTTOM);
            if (world.isRemote) {
                return EnumActionResult.SUCCESS;
            }
            if (world.setBlockState(pos, merged, 11)) {
                if (!player.capabilities.isCreativeMode) {
                    stack.shrink(1);
                }
                return EnumActionResult.SUCCESS;
            }
        }
        return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
    }

    private static boolean canMerge(IBlockState state, EnumFacing facing, float hitY) {
        EnumBlockHalf half = state.getValue(BlockSlab.HALF);
        if (half == EnumBlockHalf.BOTTOM) {
            return facing == EnumFacing.UP
                || (facing.getAxis() != EnumFacing.Axis.Y && hitY > 0.5F);
        }
        return facing == EnumFacing.DOWN
            || (facing.getAxis() != EnumFacing.Axis.Y && hitY <= 0.5F);
    }
}
