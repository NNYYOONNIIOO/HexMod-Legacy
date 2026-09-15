package at.petra_k.hexcasting.common.event;

import at.petra_k.hexcasting.common.lib.hex.HexActions;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Server-side per-player maintenance for temporary Hex abilities. */
@Mod.EventBusSubscriber(modid = "hexcasting", value = Side.SERVER)
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
}
