package at.petra_k.hexcasting.common.capability;

import at.petra_k.hexcasting.api.addldata.ADIotaHolder;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/** Forge capability provider for an entity-backed Iota holder. */
public final class HexEntityCapabilityProvider implements ICapabilityProvider {
    private final ADIotaHolder holder;

    public HexEntityCapabilityProvider(ADIotaHolder holder) {
        this.holder = holder;
    }

    @Override
    public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability,
                                 EnumFacing facing) {
        return capability == HexCapabilities.IOTA;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability,
                               EnumFacing facing) {
        return capability == HexCapabilities.IOTA ? (T) holder : null;
    }
}
