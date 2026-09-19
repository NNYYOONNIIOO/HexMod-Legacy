package at.petra_k.hexcasting.common.capability;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.item.IotaHolderItem;
import at.petra_k.hexcasting.api.item.MediaHolderItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Attaches persistent Hex Casting data to every player. */
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID)
public final class HexCapabilityHandler {
    private static final String PERSISTED_CASTING_DATA =
        HexAPI.MOD_ID + ":casting_data";

    private HexCapabilityHandler() {
    }

    @SubscribeEvent
    public static void attachPlayerCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getObject();
            HexCapabilityProvider provider = new HexCapabilityProvider();
            loadPersisted(player, provider);
            event.addCapability(HexAPI.modLoc("casting_data"), provider);
        }
    }

    @SubscribeEvent
    public static void attachItemCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (stack.getItem() instanceof MediaHolderItem) {
            MediaHolderItem holder = (MediaHolderItem) stack.getItem();
            event.addCapability(HexAPI.modLoc("media_holder"),
                new HexItemCapabilityProvider<>(HexCapabilities.MEDIA,
                    new HexItemMediaHolder(holder, stack)));
        }
        if (stack.getItem() instanceof IotaHolderItem) {
            IotaHolderItem holder = (IotaHolderItem) stack.getItem();
            event.addCapability(HexAPI.modLoc("iota_holder"),
                new HexItemCapabilityProvider<>(HexCapabilities.IOTA,
                    new HexItemIotaHolder(holder, stack)));
        }
    }

    /** Preserve the casting stack when Forge creates a replacement player. */
    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        IHexCastingData original = event.getOriginal().getCapability(HexCapabilities.CASTING_DATA, null);
        IHexCastingData replacement = event.getEntityPlayer().getCapability(HexCapabilities.CASTING_DATA, null);
        if (original == null || replacement == null) {
            return;
        }
        try {
            replacement.setMedia(original.getMedia());
            replacement.setPigment(original.getPigment());
            replacement.setPigmentVariant(original.getPigmentVariant(),
                original.getPigmentOwner());
            replacement.setInternalizedPigment(original.hasInternalizedPigment());
            replacement.setFlightTicks(original.getFlightTicks());
            replacement.setAltioraTicks(original.getAltioraTicks());
            replacement.setAltioraActive(original.isAltioraActive());
            replacement.getCastingStack().restore(original.getCastingStack().snapshot());
            replacement.getCastingStack().writeLocal(original.getCastingStack().readLocal());
            savePersistent(event.getEntityPlayer(), replacement);
        } catch (CastingException ignored) {
            // A malformed or over-sized stack must not prevent respawn.
            replacement.clearCastingStack();
            savePersistent(event.getEntityPlayer(), replacement);
        }
    }

    /** Re-apply the fallback snapshot after the player has been loaded. */
    @SubscribeEvent
    public static void restoreOnLogin(
        net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        if (event != null && event.player != null) {
            restorePersistent(event.player);
        }
    }

    /** Save the capability in PlayerPersisted, which survives world re-entry. */
    public static void savePersistent(EntityPlayer player, IHexCastingData data) {
        if (player == null || data == null) {
            return;
        }
        NBTTagCompound entityData = player.getEntityData();
        NBTTagCompound persisted = getPersisted(entityData, true);
        persisted.setTag(PERSISTED_CASTING_DATA, data.serializeNBT());
        entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
    }

    /** Restore the fallback snapshot into the attached live capability. */
    public static void restorePersistent(EntityPlayer player) {
        if (player == null) {
            return;
        }
        IHexCastingData data = player.getCapability(
            HexCapabilities.CASTING_DATA, null);
        if (data == null) {
            return;
        }
        NBTTagCompound persisted = getPersisted(player.getEntityData(), false);
        if (persisted != null && persisted.hasKey(PERSISTED_CASTING_DATA, 10)) {
            data.deserializeNBT(persisted.getCompoundTag(PERSISTED_CASTING_DATA));
        }
    }

    private static void loadPersisted(EntityPlayer player,
                                      HexCapabilityProvider provider) {
        NBTTagCompound persisted = getPersisted(player.getEntityData(), false);
        if (persisted != null && persisted.hasKey(PERSISTED_CASTING_DATA, 10)) {
            provider.deserializeNBT(persisted.getCompoundTag(PERSISTED_CASTING_DATA));
        }
    }

    private static NBTTagCompound getPersisted(NBTTagCompound entityData,
                                               boolean create) {
        if (!entityData.hasKey(EntityPlayer.PERSISTED_NBT_TAG, 10)) {
            if (!create) {
                return null;
            }
            NBTTagCompound persisted = new NBTTagCompound();
            entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
            return persisted;
        }
        return entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }
}
