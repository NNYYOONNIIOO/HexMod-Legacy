package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.common.block.TileEntityAkashicBookshelf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Restores a stored Akashic mapping when a bookshelf is placed. */
public final class ItemAkashicBookshelf extends ItemBlock {
    private static final String TAG_BLOCK_ENTITY = "BlockEntityTag";

    public ItemAkashicBookshelf(Block block) {
        super(block);
        setMaxStackSize(64);
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world,
                                BlockPos pos, EnumFacing side, float hitX,
                                float hitY, float hitZ, IBlockState newState) {
        if (!super.placeBlockAt(stack, player, world, pos, side,
                                hitX, hitY, hitZ, newState)) {
            return false;
        }
        if (world.isRemote) {
            return true;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_BLOCK_ENTITY, 10)) {
            return true;
        }
        NBTTagCompound blockEntityTag = tag.getCompoundTag(TAG_BLOCK_ENTITY);
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof TileEntityAkashicBookshelf) {
            ((TileEntityAkashicBookshelf) tileEntity).readFromNBT(blockEntityTag);
            tileEntity.markDirty();
        }
        return true;
    }
}
