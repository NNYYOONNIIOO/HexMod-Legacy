package at.petra_k.hexcasting.common.capability;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

/** Capability provider attached to each player entity. */
public final class HexCapabilityProvider implements ICapabilitySerializable<NBTTagCompound> {
    private final IHexCastingData data = new HexCastingData();

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == HexCapabilities.CASTING_DATA;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        return capability == HexCapabilities.CASTING_DATA ? (T) data : null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        return data.serializeNBT();
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        data.deserializeNBT(nbt);
    }
}
