package at.petrak.paucal.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

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

    public static ResourceLocation modLoc(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static <T extends PaucalMessage> void sendPacketToPlayerS2C(
        EntityPlayer player, T message) {
        PaucalNetwork.sendTo(message, player);
    }

    public static <T extends PaucalMessage> void sendPacketToServerC2S(T message) {
        PaucalNetwork.sendToServer(message);
    }

    public static <T extends PaucalMessage> void sendPacketNearS2C(
        Vec3d position, double radius, World world, T message) {
        if (position == null || world == null || message == null
            || radius < 0.0D || Double.isNaN(radius)) {
            return;
        }
        double radiusSquared = radius * radius;
        for (EntityPlayer player : world.playerEntities) {
            if (player.getDistanceSq(position.x, position.y, position.z) <= radiusSquared) {
                PaucalNetwork.sendTo(message, player);
            }
        }
    }
}
