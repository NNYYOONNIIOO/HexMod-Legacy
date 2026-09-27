package at.petra_k.hexcasting.common.event;

import at.petra_k.hexcasting.common.lib.hex.HexActions;
import at.petra_k.hexcasting.common.misc.PlayerPositionRecorder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Server-side per-player maintenance for temporary Hex abilities. */
// Do not restrict this subscriber to the physical server side.  In an
// integrated server the logical server runs in the client process, so a
// SERVER-only automatic subscriber is never registered there.  The action
// itself still guards against client-world execution.
@Mod.EventBusSubscriber(modid = "hexcasting")
public final class HexPlayerTickEvents {
    private HexPlayerTickEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        EntityPlayer player = event.player;
        if (player != null) {
            HexActions.tickAltiora(player);
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.world != null
            && !event.world.isRemote) {
            PlayerPositionRecorder.updateAllPlayers(event.world);
        }
    }
}
