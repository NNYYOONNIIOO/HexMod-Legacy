package at.petrak.paucal.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/** Embedded Paucal API entry point for the Hex Casting 1.12.2 port. */
public final class PaucalAPI {
    public static final String MOD_ID = "paucal";
    private static boolean initialized;

    private PaucalAPI() {
    }

    public static synchronized void init() {
        if (!initialized) {
            PaucalNetwork.init();
            initialized = true;
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static SimpleNetworkWrapper network() {
        return PaucalNetwork.channel();
    }

    public static <T extends PaucalMessage> void registerMessage(
        Class<T> messageType, Side side) {
        PaucalNetwork.registerMessage(messageType, side);
    }

    public static <T extends PaucalMessage> void sendTo(T message, EntityPlayer player) {
        PaucalNetwork.sendTo(message, player);
    }

    public static <T extends PaucalMessage> void sendToServer(T message) {
        PaucalNetwork.sendToServer(message);
    }

    public static <T extends PaucalMessage> void sendToAll(T message) {
        PaucalNetwork.sendToAll(message);
    }
}
