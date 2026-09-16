package at.petra_k.hexcasting.common.network;

import at.petrak.paucal.api.PaucalAPI;
import at.petrak.paucal.api.PaucalMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;

import java.util.UUID;

/** Server-to-client request to fade the orbiting patterns of one caster. */
public final class MsgClearCastingPatternsS2C implements PaucalMessage {
    private UUID playerUuid;

    public MsgClearCastingPatternsS2C() {
        playerUuid = new UUID(0L, 0L);
    }

    public MsgClearCastingPatternsS2C(UUID playerUuid) {
        this.playerUuid = playerUuid == null ? new UUID(0L, 0L) : playerUuid;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        playerUuid = new UUID(buf.readLong(), buf.readLong());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(playerUuid == null ? 0L : playerUuid.getMostSignificantBits());
        buf.writeLong(playerUuid == null ? 0L : playerUuid.getLeastSignificantBits());
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
            bridge.getMethod("clearSpiralPatterns", UUID.class).invoke(null, playerUuid);
        } catch (ReflectiveOperationException ignored) {
            // Client-only rendering is deliberately optional on a dedicated server.
        }
    }

    public static void register() {
        PaucalAPI.registerMessage(MsgClearCastingPatternsS2C.class, Side.CLIENT);
    }
}
