package at.petra_k.hexcasting.common.event;

import at.petra_k.hexcasting.common.lib.hex.HexActions;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Server-side maintenance for range- and time-limited Hex flight. */
@Mod.EventBusSubscriber(modid = "hexcasting")
public final class HexFlightTickEvents {
    private HexFlightTickEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        EntityPlayer player = event.player;
        if (player != null) {
            HexActions.tickFlight(player);
        }
    }
}
