package at.petra_k.hexcasting.common.capability;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

import java.util.concurrent.Callable;

/** Registration point for the 1.12.2 Forge player capability. */
public final class HexCapabilities {
    @CapabilityInject(IHexCastingData.class)
    public static Capability<IHexCastingData> CASTING_DATA = null;

    private static boolean registered;

    private HexCapabilities() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        CapabilityManager.INSTANCE.register(
            IHexCastingData.class,
            new Capability.IStorage<IHexCastingData>() {
                @Override
                public NBTBase writeNBT(Capability<IHexCastingData> capability,
                                         IHexCastingData instance, EnumFacing side) {
                    return instance.serializeNBT();
                }

                @Override
                public void readNBT(Capability<IHexCastingData> capability,
                                     IHexCastingData instance, EnumFacing side, NBTBase nbt) {
                    if (nbt instanceof NBTTagCompound) {
                        instance.deserializeNBT((NBTTagCompound) nbt);
                    }
                }
            },
            new Callable<IHexCastingData>() {
                @Override
                public IHexCastingData call() {
                    return new HexCastingData();
                }
            }
        );
        registered = true;
    }
}
