package at.petra_k.hexcasting.common.capability;

import at.petra_k.hexcasting.api.addldata.ADIotaHolder;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.item.IotaHolderItem;
import net.minecraft.item.ItemStack;

/** Capability adapter for an ItemStack-backed Iota holder. */
public final class HexItemIotaHolder implements ADIotaHolder {
    private final IotaHolderItem item;
    private final ItemStack stack;

    public HexItemIotaHolder(IotaHolderItem item, ItemStack stack) {
        this.item = item;
        this.stack = stack;
    }

    @Override
    public net.minecraft.nbt.NBTTagCompound readIotaTag() {
        return item.readIotaTag(stack);
    }

    @Override
    public boolean writeIota(Iota iota, boolean simulate) {
        if (!item.writeable(stack) || !item.canWrite(stack, iota)) {
            return false;
        }
        if (!simulate) {
            item.writeDatum(stack, iota);
        }
        return true;
    }

    @Override
    public boolean writeable() {
        return item.writeable(stack);
    }
}
