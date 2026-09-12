package at.petrak.paucal.api;

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
        channel().registerMessage(new Handler<T>(), messageType, discriminator++, side);
    }

    private static final class Handler<T extends IMessage & PaucalMessage>
        implements IMessageHandler<T, IMessage> {
        @Override
        public IMessage onMessage(T message, MessageContext context) {
            if (context.side == Side.SERVER && context.getServerHandler() != null) {
                EntityPlayer player = context.getServerHandler().player;
                message.handleMessage(player);
            }
            return null;
        }
    }
}
