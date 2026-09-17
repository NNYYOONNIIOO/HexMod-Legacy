package at.petra_k.hexcasting.common.capability;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Attaches persistent Hex Casting data to every player. */
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID)
public final class HexCapabilityHandler {
    private HexCapabilityHandler() {
    }

    @SubscribeEvent
    public static void attachPlayerCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            event.addCapability(HexAPI.modLoc("casting_data"), new HexCapabilityProvider());
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
            replacement.setFlightTicks(original.getFlightTicks());
            replacement.setAltioraTicks(original.getAltioraTicks());
            replacement.setAltioraActive(original.isAltioraActive());
            replacement.getCastingStack().restore(original.getCastingStack().snapshot());
            replacement.getCastingStack().writeLocal(original.getCastingStack().readLocal());
        } catch (CastingException ignored) {
            // A malformed or over-sized stack must not prevent respawn.
            replacement.clearCastingStack();
        }
    }
}
