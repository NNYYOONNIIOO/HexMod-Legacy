package at.petra_k.hexcasting.common.capability;

import at.petra_k.hexcasting.api.addldata.ADIotaHolder;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.function.Function;

/** Read-only Iota capability for items whose value is derived from the stack. */
public final class HexStaticIotaHolder implements ADIotaHolder {
    private final ItemStack stack;
    private final Function<ItemStack, Iota> provider;

    public HexStaticIotaHolder(ItemStack stack, Function<ItemStack, Iota> provider) {
        this.stack = stack;
        this.provider = provider;
    }

    @Override
    public NBTTagCompound readIotaTag() {
        Iota value = provider.apply(stack);
        return value == null ? null : value.serialize();
    }

    @Override
    public boolean writeIota(Iota iota, boolean simulate) {
        return false;
    }

    @Override
    public boolean writeable() {
        return false;
    }
}
