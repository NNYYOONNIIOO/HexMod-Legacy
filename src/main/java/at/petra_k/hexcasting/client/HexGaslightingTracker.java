package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.HexAPI;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Client animation counter shared by Quenched Allay models and items. */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID, value = Side.CLIENT)
public final class HexGaslightingTracker {
    private static int amount;
    private static int cooldown = 40;

    private HexGaslightingTracker() {
    }

    public static int getVariant() {
        // Rendering an item or block means that the player can currently
        // observe it.  Keep the variant stable while it is being observed;
        // the counter only advances after the object has been out of view
        // for the full cooldown, matching Hex's gaslighting behaviour.
        cooldown = 40;
        return Math.abs(amount % 4);
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.world == null) {
            amount = 0;
            cooldown = 40;
            return;
        }
        // Modern Hex checks gaslighting once per rendered frame. In 1.12.2
        // RenderTickEvent.END is the matching hook and keeps the four model
        // variants independent of the client's simulation tick rate.
        if (minecraft.isGamePaused()) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
        } else {
            amount++;
        }
    }
}
