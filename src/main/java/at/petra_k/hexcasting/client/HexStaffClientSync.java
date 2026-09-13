package at.petra_k.hexcasting.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Client-only bridge for refreshing an already-open staff screen. */
@SideOnly(Side.CLIENT)
public final class HexStaffClientSync {
    private HexStaffClientSync() {
    }

    public static void refresh() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen instanceof GuiHexStaff) {
            ((GuiHexStaff) minecraft.currentScreen).refreshFromServer();
        }
    }

    public static void showCastResult(String message) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen instanceof GuiHexStaff) {
            ((GuiHexStaff) minecraft.currentScreen).showCastResult(message);
        }
    }
}
