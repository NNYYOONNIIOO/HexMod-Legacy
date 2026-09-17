package at.petra_k.hexcasting.common.capability;

import at.petra_k.hexcasting.api.addldata.ADMediaHolder;
import at.petra_k.hexcasting.api.item.MediaHolderItem;
import net.minecraft.item.ItemStack;

/** Capability adapter for an ItemStack-backed media holder. */
public final class HexItemMediaHolder implements ADMediaHolder {
    private final MediaHolderItem item;
    private final ItemStack stack;

    public HexItemMediaHolder(MediaHolderItem item, ItemStack stack) {
        this.item = item;
        this.stack = stack;
    }

    @Override
    public long getMedia() {
        return item.getMedia(stack);
    }

    @Override
    public long getMaxMedia() {
        return item.getMaxMedia(stack);
    }

    @Override
    public void setMedia(long media) {
        item.setMedia(stack, media);
    }

    @Override
    public boolean canRecharge() {
        return item.canRecharge(stack);
    }

    @Override
    public boolean canProvide() {
        return item.canProvide(stack);
    }

    @Override
    public int getConsumptionPriority() {
        return item.getConsumptionPriority(stack);
    }

    @Override
    public boolean canConstructBattery() {
        return item.canConstructBattery(stack);
    }
}
