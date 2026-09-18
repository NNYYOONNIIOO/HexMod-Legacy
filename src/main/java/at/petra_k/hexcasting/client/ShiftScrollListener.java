package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.common.item.ItemAbacus;
import at.petra_k.hexcasting.common.item.ItemSpellbook;
import at.petra_k.hexcasting.common.network.MsgShiftScrollC2S;
import at.petrak.paucal.api.PaucalAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Client-side equivalent of Hex's ShiftScrollListener. */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID, value = Side.CLIENT)
public final class ShiftScrollListener {
    private static double mainHandDelta;
    private static double offHandDelta;

    private ShiftScrollListener() {
    }

    /** Consume in-world wheel input before vanilla changes the hotbar slot. */
    @SubscribeEvent
    public static void onMouseScroll(MouseEvent event) {
        if (event == null || event.getDwheel() == 0) {
            return;
        }
        double delta = event.getDwheel() > 0 ? 1.0D : -1.0D;
        if (onScrollInGameplay(delta)) {
            event.setCanceled(true);
        }
    }

    /** Flush accumulated wheel input on the client tick. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            clientTickEnd();
        }
    }

    public static boolean onScrollInGameplay(double delta) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.currentScreen != null
            || minecraft.player == null) {
            return false;
        }
        // Use the key binding rather than the entity's crouching state; this
        // also works while the player is airborne and with remapped keys.
        if (!minecraft.gameSettings.keyBindSneak.isKeyDown()) {
            return false;
        }
        return onScroll(delta, true);
    }

    public static boolean onScroll(double delta, boolean needsSneaking) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.player == null) {
            return false;
        }
        if (needsSneaking && !minecraft.gameSettings.keyBindSneak.isKeyDown()) {
            return false;
        }
        if (minecraft.player.isSpectator()) {
            return false;
        }

        Item main = minecraft.player.getHeldItemMainhand().getItem();
        Item off = minecraft.player.getHeldItemOffhand().getItem();
        if (isScrollable(main)) {
            mainHandDelta += delta;
            return true;
        }
        if (isScrollable(off)) {
            offHandDelta += delta;
            return true;
        }
        return false;
    }

    public static void clientTickEnd() {
        if (mainHandDelta == 0.0D && offHandDelta == 0.0D) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.player == null) {
            mainHandDelta = 0.0D;
            offHandDelta = 0.0D;
            return;
        }
        PaucalAPI.sendToServer(new MsgShiftScrollC2S(
            mainHandDelta, offHandDelta,
            minecraft.gameSettings.keyBindSprint.isKeyDown()));
        mainHandDelta = 0.0D;
        offHandDelta = 0.0D;
    }

    private static boolean isScrollable(Item item) {
        return item instanceof ItemSpellbook || item instanceof ItemAbacus;
    }
}
