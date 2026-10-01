package at.petra_k.hexcasting.common.capability;

import at.petra_k.hexcasting.api.addldata.ADMediaHolder;
import at.petra_k.hexcasting.common.lib.hex.CustomMediaValues;
import net.minecraft.item.ItemStack;

/** Capability adapter for discrete media items represented by stack count. */
public final class HexStaticMediaHolder implements ADMediaHolder {
    private final ItemStack stack;
    private final long worth;
    private final int priority;

    public HexStaticMediaHolder(ItemStack stack, long worth, int priority) {
        this.stack = stack;
        this.worth = Math.max(0L, worth);
        this.priority = priority;
    }

    @Override
    public long getMedia() {
        return multiply(getWorth(), stack == null ? 0L : stack.getCount());
    }

    @Override
    public long getMaxMedia() {
        return getMedia();
    }

    @Override
    public void setMedia(long media) {
        // Discrete media is represented by the ItemStack count.
    }

    @Override
    public boolean canRecharge() {
        return false;
    }

    @Override
    public boolean canProvide() {
        return getWorth() > 0L;
    }

    @Override
    public int getConsumptionPriority() {
        return priority;
    }

    @Override
    public boolean canConstructBattery() {
        return getWorth() > 0L;
    }

    @Override
    public long withdrawMedia(long amount, boolean simulate) {
        long available = getMedia();
        long requested = amount < 0L ? available : Math.max(0L, amount);
        long worth = getWorth();
        if (available <= 0L || requested <= 0L || worth <= 0L) {
            return 0L;
        }
        long count = (requested + worth - 1L) / worth;
        count = Math.min(count, Math.max(0L, (long) stack.getCount()));
        long extracted = multiply(worth, count);
        if (!simulate && count > 0L) {
            stack.shrink((int) Math.min(Integer.MAX_VALUE, count));
        }
        return extracted;
    }

    private static long multiply(long left, long right) {
        return left <= 0L || right <= 0L || left > Long.MAX_VALUE / right
            ? (left > 0L && right > 0L ? Long.MAX_VALUE : 0L) : left * right;
    }

    private long getWorth() {
        return stack != null && CustomMediaValues.has(stack)
            ? CustomMediaValues.get(stack) : worth;
    }
}
