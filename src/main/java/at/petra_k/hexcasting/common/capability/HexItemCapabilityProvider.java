package at.petra_k.hexcasting.common.capability;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/** Small immutable provider used for ItemStack-scoped Hex capabilities. */
public final class HexItemCapabilityProvider<T> implements ICapabilityProvider {
    private final Capability<T> capability;
    private final T instance;

    public HexItemCapabilityProvider(Capability<T> capability, T instance) {
        this.capability = capability;
        this.instance = instance;
    }

    @Override
    public boolean hasCapability(Capability<?> requested, EnumFacing facing) {
        return requested == capability;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C> C getCapability(Capability<C> requested, EnumFacing facing) {
        return requested == capability ? (C) instance : null;
    }
}
