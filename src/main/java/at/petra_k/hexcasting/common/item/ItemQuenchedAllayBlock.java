package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.item.MediaHolderItem;
import at.petra_k.hexcasting.api.misc.MediaConstants;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.List;

/**
 * The item form of a quenched Allay block is a fixed media source.
 *
 * <p>Unlike a battery, the block cannot be partially emptied.  Withdrawing
 * any positive amount consumes enough whole blocks to cover that amount,
 * matching the static-media behavior of modern Hex.</p>
 */
public final class ItemQuenchedAllayBlock extends ItemBlock
    implements MediaHolderItem {
    private static final long MEDIA_PER_BLOCK = MediaConstants.QUENCHED_BLOCK_UNIT;
    private static final int CONSUMPTION_PRIORITY = 800;

    public ItemQuenchedAllayBlock(Block block) {
        super(block);
        setMaxStackSize(64);
    }

    @Override
    public long getMaxMedia(ItemStack stack) {
        return mediaForCount(stack);
    }

    @Override
    public long getMedia(ItemStack stack) {
        return mediaForCount(stack);
    }

    /** Fixed-media blocks do not retain a partially drained NBT value. */
    @Override
    public void setMedia(ItemStack stack, long media) {
        // Intentionally empty: the media belongs to each whole block.
    }

    @Override
    public boolean canRecharge(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canProvide(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canConstructBattery(ItemStack stack) {
        return true;
    }

    @Override
    public int getConsumptionPriority(ItemStack stack) {
        return CONSUMPTION_PRIORITY;
    }

    @Override
    public long withdrawMedia(ItemStack stack, long amount, boolean simulate) {
        if (stack == null || stack.isEmpty()) {
            return 0L;
        }

        long available = getMedia(stack);
        long requested = amount < 0L ? available : Math.max(0L, amount);
        if (available <= 0L || requested <= 0L) {
            return 0L;
        }

        long wanted = Math.min(available, requested);
        long blocks = (wanted + MEDIA_PER_BLOCK - 1L) / MEDIA_PER_BLOCK;
        long removed = Math.min((long) stack.getCount(), blocks);
        long extracted = removed * MEDIA_PER_BLOCK;
        if (!simulate && removed > 0L) {
            stack.shrink((int) removed);
        }
        return extracted;
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               ITooltipFlag flag) {
        MediaTooltip.add(tooltip, getMedia(stack), getMaxMedia(stack));
    }

    private static long mediaForCount(ItemStack stack) {
        long count = stack == null ? 0L : Math.max(0L, stack.getCount());
        if (count == 0L || MEDIA_PER_BLOCK > Long.MAX_VALUE / count) {
            return count == 0L ? 0L : Long.MAX_VALUE;
        }
        return MEDIA_PER_BLOCK * count;
    }
}
