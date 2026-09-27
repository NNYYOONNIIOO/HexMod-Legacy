package at.petra_k.hexcasting.common.capability;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.addldata.ADMediaHolder;
import at.petra_k.hexcasting.api.addldata.ADIotaHolder;
import at.petra_k.hexcasting.api.addldata.ADHexHolder;
import at.petra_k.hexcasting.api.addldata.ADVariantItem;
import at.petra_k.hexcasting.api.addldata.ADPigment;
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

    @CapabilityInject(ADMediaHolder.class)
    public static Capability<ADMediaHolder> MEDIA = null;

    @CapabilityInject(ADIotaHolder.class)
    public static Capability<ADIotaHolder> IOTA = null;

    @CapabilityInject(ADHexHolder.class)
    public static Capability<ADHexHolder> HEX_HOLDER = null;

    @CapabilityInject(ADVariantItem.class)
    public static Capability<ADVariantItem> VARIANT = null;

    @CapabilityInject(ADPigment.class)
    public static Capability<ADPigment> PIGMENT = null;

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
        CapabilityManager.INSTANCE.register(
            ADMediaHolder.class,
            new Capability.IStorage<ADMediaHolder>() {
                @Override
                public NBTBase writeNBT(Capability<ADMediaHolder> capability,
                                         ADMediaHolder instance, EnumFacing side) {
                    NBTTagCompound tag = new NBTTagCompound();
                    tag.setLong("media", instance == null ? 0L : instance.getMedia());
                    tag.setLong("max_media", instance == null ? 0L : instance.getMaxMedia());
                    return tag;
                }

                @Override
                public void readNBT(Capability<ADMediaHolder> capability,
                                     ADMediaHolder instance, EnumFacing side, NBTBase nbt) {
                    if (instance != null && nbt instanceof NBTTagCompound) {
                        instance.setMedia(((NBTTagCompound) nbt).getLong("media"));
                    }
                }
            },
            () -> new ADMediaHolder() {
                private long media;
                @Override public long getMedia() { return media; }
                @Override public long getMaxMedia() { return Long.MAX_VALUE; }
                @Override public void setMedia(long value) { media = Math.max(0L, value); }
                @Override public boolean canRecharge() { return true; }
                @Override public boolean canProvide() { return true; }
                @Override public int getConsumptionPriority() { return 0; }
                @Override public boolean canConstructBattery() { return false; }
            }
        );
        CapabilityManager.INSTANCE.register(
            ADIotaHolder.class,
            new Capability.IStorage<ADIotaHolder>() {
                @Override
                public NBTBase writeNBT(Capability<ADIotaHolder> capability,
                                         ADIotaHolder instance, EnumFacing side) {
                    NBTTagCompound tag = instance == null ? null : instance.readIotaTag();
                    return tag == null ? new NBTTagCompound() : tag;
                }

                @Override
                public void readNBT(Capability<ADIotaHolder> capability,
                                     ADIotaHolder instance, EnumFacing side, NBTBase nbt) {
                    // ItemStack NBT is authoritative; providers are rebuilt
                    // from the stack when Forge attaches them.
                }
            },
            () -> null
        );
        CapabilityManager.INSTANCE.register(
            ADHexHolder.class,
            new Capability.IStorage<ADHexHolder>() {
                @Override
                public NBTBase writeNBT(Capability<ADHexHolder> capability,
                                         ADHexHolder instance, EnumFacing side) {
                    return new NBTTagCompound();
                }

                @Override
                public void readNBT(Capability<ADHexHolder> capability,
                                     ADHexHolder instance, EnumFacing side, NBTBase nbt) {
                    // The item stack is the authoritative storage.
                }
            },
            () -> null
        );
        CapabilityManager.INSTANCE.register(
            ADVariantItem.class,
            new Capability.IStorage<ADVariantItem>() {
                @Override
                public NBTBase writeNBT(Capability<ADVariantItem> capability,
                                         ADVariantItem instance, EnumFacing side) {
                    NBTTagCompound tag = new NBTTagCompound();
                    if (instance != null) {
                        tag.setInteger("variant", instance.getVariant());
                    }
                    return tag;
                }

                @Override
                public void readNBT(Capability<ADVariantItem> capability,
                                     ADVariantItem instance, EnumFacing side, NBTBase nbt) {
                    if (instance != null && nbt instanceof NBTTagCompound) {
                        instance.setVariant(((NBTTagCompound) nbt).getInteger("variant"));
                    }
                }
            },
            () -> null
        );
        CapabilityManager.INSTANCE.register(
            ADPigment.class,
            new Capability.IStorage<ADPigment>() {
                @Override
                public NBTBase writeNBT(Capability<ADPigment> capability,
                                         ADPigment instance, EnumFacing side) {
                    return new NBTTagCompound();
                }

                @Override
                public void readNBT(Capability<ADPigment> capability,
                                     ADPigment instance, EnumFacing side, NBTBase nbt) {
                    // Pigment items keep their colour and animation data in NBT.
                }
            },
            () -> null
        );
        registered = true;
    }
}
