package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.item.MediaHolderItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * 1.12.2 media inventory bridge.  It deliberately scans only player-owned
 * stacks and keeps the persistent player capability as the final source.
 */
public final class MediaInventoryHelper {
    private MediaInventoryHelper() {
    }

    public static long getAvailableMedia(EntityPlayer player, IHexCastingData data) {
        long total = data == null ? 0L : Math.max(0L, data.getMedia());
        if (player == null) {
            return total;
        }
        for (ItemStack stack : player.inventory.mainInventory) {
            total = saturatingAdd(total, getAvailable(stack));
        }
        for (ItemStack stack : player.inventory.armorInventory) {
            total = saturatingAdd(total, getAvailable(stack));
        }
        for (ItemStack stack : player.inventory.offHandInventory) {
            total = saturatingAdd(total, getAvailable(stack));
        }
        return total;
    }

    private static long getAvailable(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof MediaHolderItem)) {
            return 0L;
        }
        MediaHolderItem holder = (MediaHolderItem) stack.getItem();
        return holder.canProvide(stack) ? holder.withdrawMedia(stack, -1L, true) : 0L;
    }

    private static long saturatingAdd(long left, long right) {
        if (right <= 0L || Long.MAX_VALUE - left < right) {
            return right <= 0L ? left : Long.MAX_VALUE;
        }
        return left + right;
    }
}
