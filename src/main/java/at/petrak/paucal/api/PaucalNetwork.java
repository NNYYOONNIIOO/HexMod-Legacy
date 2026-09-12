package at.petrak.paucal.api;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.network.NetworkRegistry;

/** Small SimpleNetworkWrapper-backed equivalent of Paucal's message boundary. */
public final class PaucalNetwork {
    private static final String CHANNEL_NAME = "hexcasting_paucal";
    private static SimpleNetworkWrapper channel;
    private static int discriminator;

    private PaucalNetwork() {
    }

    public static synchronized void init() {
        if (channel == null) {
            channel = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);
        }
    }

    public static synchronized SimpleNetworkWrapper channel() {
        init();
        return channel;
    }

    public static synchronized <T extends IMessage & PaucalMessage> void registerMessage(
        Class<T> messageType, Side side) {
        if (messageType == null || side == null) {
            throw new IllegalArgumentException("Paucal messages need a type and side");
        }
        channel().registerMessage(new Handler<T>(), messageType, discriminator++, side);
    }

    public static <T extends IMessage & PaucalMessage> void sendTo(T message, EntityPlayer player) {
        if (message != null && player instanceof EntityPlayerMP) {
            channel().sendTo(message, (EntityPlayerMP) player);
        }
    }

    public static <T extends IMessage & PaucalMessage> void sendToServer(T message) {
        if (message != null) {
            channel().sendToServer(message);
        }
    }

    public static <T extends IMessage & PaucalMessage> void sendToAll(T message) {
        if (message != null) {
            channel().sendToAll(message);
        }
    }

    private static final class Handler<T extends IMessage & PaucalMessage>
        implements IMessageHandler<T, IMessage> {
        @Override
        public IMessage onMessage(T message, MessageContext context) {
            if (message == null || context == null) {
                return null;
            }
            if (context.side == Side.SERVER && context.getServerHandler() != null) {
                EntityPlayerMP player = context.getServerHandler().player;
                ((net.minecraft.world.WorldServer) player.world).addScheduledTask(
                    () -> message.handleMessage(player, Side.SERVER));
            } else if (context.side == Side.CLIENT) {
                dispatchClient(message);
            }
            return null;
        }

        private void dispatchClient(T message) {
            try {
                Class<?> bridge = Class.forName("at.petrak.paucal.client.PaucalClientNetwork");
                bridge.getMethod("dispatch", PaucalMessage.class).invoke(null, message);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(
                    "Paucal client network bridge is unavailable", exception);
            }
        }
    }
}
