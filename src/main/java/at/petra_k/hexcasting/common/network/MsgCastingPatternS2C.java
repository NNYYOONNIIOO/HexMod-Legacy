package at.petra_k.hexcasting.common.network;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petrak.paucal.api.PaucalAPI;
import at.petrak.paucal.api.PaucalMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;

import java.util.UUID;

/** Server-to-client update for one of the rune patterns orbiting a caster. */
public final class MsgCastingPatternS2C implements PaucalMessage {
    private UUID playerUuid;
    private NBTTagCompound patternData;
    private int lifetime;
    private int color;

    public MsgCastingPatternS2C() {
        playerUuid = new UUID(0L, 0L);
        patternData = new NBTTagCompound();
        lifetime = 0;
        color = 0xFFAA66FF;
    }

    public MsgCastingPatternS2C(UUID playerUuid, HexPattern pattern,
                                int lifetime, int color) {
        this.playerUuid = playerUuid == null ? new UUID(0L, 0L) : playerUuid;
        this.patternData = pattern == null ? new NBTTagCompound()
            : pattern.serializeToNBT();
        this.lifetime = Math.max(0, lifetime);
        this.color = 0xFF000000 | (color & 0xFFFFFF);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        playerUuid = new UUID(buf.readLong(), buf.readLong());
        patternData = ByteBufUtils.readTag(buf);
        lifetime = Math.max(0, buf.readInt());
        color = 0xFF000000 | (buf.readInt() & 0xFFFFFF);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(playerUuid == null ? 0L : playerUuid.getMostSignificantBits());
        buf.writeLong(playerUuid == null ? 0L : playerUuid.getLeastSignificantBits());
        ByteBufUtils.writeTag(buf, patternData == null ? new NBTTagCompound() : patternData);
        buf.writeInt(Math.max(0, lifetime));
        buf.writeInt(color & 0xFFFFFF);
    }

    @Override
    public void handleMessage(EntityPlayer player) {
        handleMessage(player, Side.CLIENT);
    }

    @Override
    public void handleMessage(EntityPlayer player, Side side) {
        if (side != Side.CLIENT || patternData == null) {
            return;
        }
        try {
            HexPattern pattern = HexPattern.fromNBT(patternData);
            Class<?> bridge = Class.forName(
                "at.petra_k.hexcasting.client.HexClientEffects");
            bridge.getMethod("addSpiralPattern", UUID.class, HexPattern.class,
                int.class, int.class).invoke(null, playerUuid, pattern,
                lifetime, color);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Client-only rendering is deliberately optional on a dedicated server.
        }
    }

    public static void register() {
        PaucalAPI.registerMessage(MsgCastingPatternS2C.class, Side.CLIENT);
    }
}
