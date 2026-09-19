package at.petra_k.hexcasting.common.network;

import at.petra_k.hexcasting.common.world.SentinelData;
import at.petrak.paucal.api.PaucalAPI;
import at.petrak.paucal.api.PaucalMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;

import java.util.UUID;

/** Authoritative server-to-client snapshot of one player's sentinel. */
public final class MsgSentinelStatusS2C implements PaucalMessage {
    private UUID playerUuid;
    private boolean exists;
    private boolean extendedRange;
    private double x;
    private double y;
    private double z;
    private int dimension;

    public MsgSentinelStatusS2C() {
        playerUuid = new UUID(0L, 0L);
    }

    public MsgSentinelStatusS2C(EntityPlayer player) {
        playerUuid = player == null ? new UUID(0L, 0L) : player.getUniqueID();
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }
        SentinelData.State state = SentinelData.get(player.world).get(playerUuid);
        if (state != null) {
            exists = true;
            extendedRange = state.extendedRange;
            x = state.x;
            y = state.y;
            z = state.z;
            dimension = state.dimension;
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        playerUuid = new UUID(buf.readLong(), buf.readLong());
        exists = buf.readBoolean();
        if (!exists) {
            return;
        }
        extendedRange = buf.readBoolean();
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        dimension = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        UUID safeUuid = playerUuid == null ? new UUID(0L, 0L) : playerUuid;
        buf.writeLong(safeUuid.getMostSignificantBits());
        buf.writeLong(safeUuid.getLeastSignificantBits());
        buf.writeBoolean(exists);
        if (!exists) {
            return;
        }
        buf.writeBoolean(extendedRange);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeInt(dimension);
    }

    @Override
    public void handleMessage(EntityPlayer player) {
        handleMessage(player, Side.CLIENT);
    }

    @Override
    public void handleMessage(EntityPlayer player, Side side) {
        if (side != Side.CLIENT) {
            return;
        }
        try {
            Class<?> bridge = Class.forName(
                "at.petra_k.hexcasting.client.HexClientEffects");
            bridge.getMethod("updateSentinel", UUID.class, boolean.class,
                boolean.class, double.class, double.class, double.class,
                int.class).invoke(null, playerUuid, exists, extendedRange,
                x, y, z, dimension);
        } catch (ReflectiveOperationException ignored) {
            // Client-only rendering is deliberately optional on a dedicated server.
        }
    }

    public static void register() {
        PaucalAPI.registerMessage(MsgSentinelStatusS2C.class, Side.CLIENT);
    }
}
