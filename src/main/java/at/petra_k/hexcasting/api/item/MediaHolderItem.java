package at.petra_k.hexcasting.api.item;

import net.minecraft.item.ItemStack;
import at.petra_k.hexcasting.api.addldata.ADMediaHolder;

/** Stack-backed media storage contract for 1.12.2 items. */
public interface MediaHolderItem {
    long getMaxMedia(ItemStack stack);
    long getMedia(ItemStack stack);
    void setMedia(ItemStack stack, long media);

    default boolean canRecharge(ItemStack stack) { return true; }
    default boolean canProvide(ItemStack stack) { return true; }
    /** Match modern media-holder items: unspecified sources use battery priority. */
    default int getConsumptionPriority(ItemStack stack) {
        return (int) ADMediaHolder.BATTERY_PRIORITY;
    }
    default boolean canConstructBattery(ItemStack stack) { return false; }

    default long withdrawMedia(ItemStack stack, long amount, boolean simulate) {
        long available = Math.max(0L, getMedia(stack));
        long extracted = amount < 0L ? available : Math.min(available, Math.max(0L, amount));
        if (!simulate && extracted > 0L) setMedia(stack, available - extracted);
        return extracted;
    }

    default long insertMedia(ItemStack stack, long amount, boolean simulate) {
        long current = Math.max(0L, getMedia(stack));
        long capacity = Math.max(0L, getMaxMedia(stack));
        long empty = Math.max(0L, capacity - current);
        if (empty <= 0L || amount == 0L) return 0L;
        long inserted = Math.min(amount < 0L ? empty : amount, empty);
        if (!simulate && inserted > 0L) setMedia(stack, current + inserted);
        return inserted;
    }
}
