package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.HexAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * One render-safe clock for client-only Hex animations.
 *
 * <p>Using a world's total time directly is tempting, but it does not carry
 * the render partial tick and it is not a good clock for effects that also
 * render in inventory screens. Keeping the clock here mirrors Hex's modern
 * client tick counter and makes all effects stop together while a
 * single-player world is paused.</p>
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID, value = Side.CLIENT)
public final class HexClientTickCounter {
    private static World activeWorld;
    private static long ticksInGame;
    private static float partialTicks;

    private HexClientTickCounter() {
    }

    public static float getTotal() {
        return (float) ticksInGame + partialTicks;
    }

    public static float getPartialTicks() {
        return partialTicks;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.world == null) {
            activeWorld = null;
            ticksInGame = 0L;
            partialTicks = 0.0F;
            return;
        }
        if (activeWorld != minecraft.world) {
            activeWorld = minecraft.world;
            ticksInGame = 0L;
            partialTicks = 0.0F;
        }
        partialTicks = minecraft.isGamePaused()
            ? 0.0F : Math.max(0.0F, event.renderTickTime);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.world == null) {
            activeWorld = null;
            ticksInGame = 0L;
            partialTicks = 0.0F;
            return;
        }
        if (activeWorld != minecraft.world) {
            activeWorld = minecraft.world;
            ticksInGame = 0L;
        }
        if (minecraft.isGamePaused()) {
            return;
        }
        ticksInGame++;
        partialTicks = 0.0F;
    }
}
