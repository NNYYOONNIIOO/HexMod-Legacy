package at.petra_k.hexcasting.common.capability;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.iota.DoubleIota;
import at.petra_k.hexcasting.api.item.IotaHolderItem;
import at.petra_k.hexcasting.api.item.MediaHolderItem;
import at.petra_k.hexcasting.api.item.HexHolderItem;
import at.petra_k.hexcasting.api.item.VariantItem;
import at.petra_k.hexcasting.api.item.PigmentItem;
import at.petra_k.hexcasting.common.entity.EntityWallScroll;
import at.petra_k.hexcasting.api.misc.MediaConstants;
import at.petra_k.hexcasting.common.misc.AmethystCompat;
import at.petra_k.hexcasting.common.lib.hex.CustomMediaValues;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
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
        if (CustomMediaValues.has(stack)) {
            long worth = CustomMediaValues.get(stack);
            if (worth > 0L) {
                event.addCapability(HexAPI.modLoc("static_media"),
                    new HexItemCapabilityProvider<>(HexCapabilities.MEDIA,
                        new HexStaticMediaHolder(stack, worth, staticMediaPriority(stack))));
            }
        } else if (stack.getItem() instanceof MediaHolderItem) {
            MediaHolderItem holder = (MediaHolderItem) stack.getItem();
            event.addCapability(HexAPI.modLoc("media_holder"),
                new HexItemCapabilityProvider<>(HexCapabilities.MEDIA,
                    new HexItemMediaHolder(holder, stack)));
        } else {
            long worth = staticMediaWorth(stack);
            if (worth > 0L) {
                event.addCapability(HexAPI.modLoc("static_media"),
                    new HexItemCapabilityProvider<>(HexCapabilities.MEDIA,
                        new HexStaticMediaHolder(stack, worth, staticMediaPriority(stack))));
            }
        }
        if (stack.getItem() instanceof IotaHolderItem) {
            IotaHolderItem holder = (IotaHolderItem) stack.getItem();
            event.addCapability(HexAPI.modLoc("iota_holder"),
                new HexItemCapabilityProvider<>(HexCapabilities.IOTA,
                    new HexItemIotaHolder(holder, stack)));
        } else if (stack.getItem() == Items.PUMPKIN_PIE) {
            event.addCapability(HexAPI.modLoc("static_iota"),
                new HexItemCapabilityProvider<>(HexCapabilities.IOTA,
                    new HexStaticIotaHolder(stack,
                        value -> new DoubleIota(Math.PI * value.getCount()))));
        }
        if (stack.getItem() instanceof HexHolderItem) {
            HexHolderItem holder = (HexHolderItem) stack.getItem();
            event.addCapability(HexAPI.modLoc("hex_holder"),
                new HexItemCapabilityProvider<>(HexCapabilities.HEX_HOLDER,
                    new HexItemHexHolder(holder, stack)));
        }
        if (stack.getItem() instanceof VariantItem) {
            VariantItem variant = (VariantItem) stack.getItem();
            event.addCapability(HexAPI.modLoc("variant_item"),
                new HexItemCapabilityProvider<>(HexCapabilities.VARIANT,
                    new HexItemVariant(variant, stack)));
        }
        if (stack.getItem() instanceof PigmentItem) {
            PigmentItem pigment = (PigmentItem) stack.getItem();
            event.addCapability(HexAPI.modLoc("pigment"),
                new HexItemCapabilityProvider<>(HexCapabilities.PIGMENT,
                    new HexItemPigment(pigment, stack)));
        }
    }

    @SubscribeEvent
    public static void attachEntityCapabilities(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof EntityItem || entity instanceof EntityItemFrame
            || entity instanceof EntityWallScroll) {
            event.addCapability(HexAPI.modLoc("entity_iota"),
                new HexEntityCapabilityProvider(
                    new HexEntityIotaHolder(entity)));
        }
    }

    private static long staticMediaWorth(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null) {
            return 0L;
        }
        if (CustomMediaValues.has(stack)) {
            return CustomMediaValues.get(stack);
        }
        net.minecraft.util.ResourceLocation id = stack.getItem().getRegistryName();
        if ("minecraft".equals(id.getResourceDomain())
            && "amethyst_shard".equals(id.getResourcePath())) {
            return MediaConstants.SHARD_UNIT;
        }
        net.minecraft.util.ResourceLocation selected = AmethystCompat.shardId();
        if (selected != null && selected.equals(id)) {
            return MediaConstants.SHARD_UNIT;
        }
        if (AmethystCompat.legacyShardId().equals(id) && selected == null) {
            return MediaConstants.SHARD_UNIT;
        }
        if (HexAPI.MOD_ID.equals(id.getResourceDomain())
            && "quenched_allay".equals(id.getResourcePath())) {
            return MediaConstants.QUENCHED_BLOCK_UNIT;
        }
        return 0L;
    }

    private static int staticMediaPriority(ItemStack stack) {
        net.minecraft.util.ResourceLocation id = stack.getItem().getRegistryName();
        return id != null && HexAPI.MOD_ID.equals(id.getResourceDomain())
            && "quenched_allay".equals(id.getResourcePath())
            ? (int) at.petra_k.hexcasting.api.addldata.ADMediaHolder.QUENCHED_ALLAY_PRIORITY
            : (int) at.petra_k.hexcasting.api.addldata.ADMediaHolder.AMETHYST_SHARD_PRIORITY;
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
            replacement.setFlightActive(original.isFlightActive());
            replacement.setFlightDimension(original.getFlightDimension());
            replacement.setFlightOrigin(original.getFlightOriginX(),
                original.getFlightOriginY(), original.getFlightOriginZ());
            replacement.setFlightRadius(original.getFlightRadius());
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
