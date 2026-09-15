package at.petra_k.hexcasting.common.event;

import at.petra_k.hexcasting.HexCasting;
import at.petra_k.hexcasting.common.network.MsgPerWorldPatternsS2C;
import at.petrak.paucal.api.PaucalAPI;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Sends the authoritative great-spell stroke table whenever a client enters a world. */
@Mod.EventBusSubscriber(modid = HexCasting.MOD_ID, value = Side.SERVER)
public final class HexWorldSyncEvents {
    private HexWorldSyncEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        send(event == null ? null : event.player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        send(event == null ? null : event.player);
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        send(event == null ? null : event.player);
    }

    private static void send(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        PaucalAPI.sendTo(new MsgPerWorldPatternsS2C(serverPlayer.world), serverPlayer);
    }
}
