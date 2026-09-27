package at.petra_k.hexcasting.common.event;

import at.petra_k.hexcasting.HexCasting;
import at.petra_k.hexcasting.common.effect.HexCastingEffects;
import at.petra_k.hexcasting.common.network.MsgPerWorldPatternsS2C;
import at.petra_k.hexcasting.common.capability.HexCapabilityHandler;
import at.petra_k.hexcasting.common.capability.HexCapabilitySync;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import at.petra_k.hexcasting.common.network.MsgSentinelStatusS2C;
import at.petrak.paucal.api.PaucalAPI;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Sends the authoritative great-spell stroke table whenever a client enters a world. */
// Do not restrict this subscriber to the physical server side.  An
// integrated server runs in the client process, but these handlers still
// receive logical-server player events.  send() keeps the EntityPlayerMP
// guard so the client-side event bus is harmless.
@Mod.EventBusSubscriber(modid = HexCasting.MOD_ID)
public final class HexWorldSyncEvents {
    private HexWorldSyncEvents() {
    }

    @SubscribeEvent
    public static void onLogin(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        send(event == null ? null : event.player);
    }

    @SubscribeEvent
    public static void onRespawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
        send(event == null ? null : event.player);
    }

    @SubscribeEvent
    public static void onDimensionChange(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent event) {
        send(event == null ? null : event.player);
    }

    /** Send an already-loaded player's casting state to a newly tracking client. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event == null || !(event.getTarget() instanceof EntityPlayer)
            || !(event.getEntityPlayer() instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayer target = (EntityPlayer) event.getTarget();
        EntityPlayerMP observer = (EntityPlayerMP) event.getEntityPlayer();
        if (target.getCapability(HexCapabilities.CASTING_DATA, null) != null) {
            PaucalAPI.sendTo(new at.petra_k.hexcasting.common.network.MsgCastingDataS2C(
                target, target.getCapability(HexCapabilities.CASTING_DATA, null)), observer);
        }
        // The orbit packet is broadcast with the target's position and is
        // harmless for clients that are already synchronized.
        HexCastingEffects.syncOrbitPatterns(target);
    }

    private static void send(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        HexCapabilityHandler.restorePersistent(serverPlayer);
        PaucalAPI.sendTo(new MsgPerWorldPatternsS2C(serverPlayer.world), serverPlayer);
        HexCapabilitySync.send(serverPlayer);
        PaucalAPI.sendTo(new MsgSentinelStatusS2C(serverPlayer), serverPlayer);
        HexCastingEffects.syncOrbitPatterns(serverPlayer);
    }
}
