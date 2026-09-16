package at.petra_k.hexcasting.common.network;

import at.petrak.paucal.api.PaucalAPI;
import at.petrak.paucal.api.PaucalMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;

/** Server-to-client colored particle spray, matching Hex's ParticleSpray payload. */
public final class MsgCastParticlesS2C implements PaucalMessage {
    private double posX;
    private double posY;
    private double posZ;
    private double velX;
    private double velY;
    private double velZ;
    private double fuzziness;
    private double spread;
    private int count;
    private int color;

    public MsgCastParticlesS2C() {
        color = 0xFFAA66FF;
    }

    public MsgCastParticlesS2C(double posX, double posY, double posZ,
                               double velX, double velY, double velZ,
                               double fuzziness, double spread, int count,
                               int color) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.velX = velX;
        this.velY = velY;
        this.velZ = velZ;
        this.fuzziness = Math.max(0.0D, fuzziness);
        this.spread = Math.max(0.0D, Math.min(Math.PI, spread));
        this.count = Math.max(0, Math.min(256, count));
        this.color = 0xFF000000 | (color & 0xFFFFFF);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        posX = buf.readDouble();
        posY = buf.readDouble();
        posZ = buf.readDouble();
        velX = buf.readDouble();
        velY = buf.readDouble();
        velZ = buf.readDouble();
        fuzziness = Math.max(0.0D, buf.readDouble());
        spread = Math.max(0.0D, Math.min(Math.PI, buf.readDouble()));
        count = Math.max(0, Math.min(256, buf.readInt()));
        color = 0xFF000000 | (buf.readInt() & 0xFFFFFF);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(posX);
        buf.writeDouble(posY);
        buf.writeDouble(posZ);
        buf.writeDouble(velX);
        buf.writeDouble(velY);
        buf.writeDouble(velZ);
        buf.writeDouble(fuzziness);
        buf.writeDouble(spread);
        buf.writeInt(count);
        buf.writeInt(color & 0xFFFFFF);
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
            bridge.getMethod("spawnSpray", double.class, double.class, double.class,
                double.class, double.class, double.class, double.class, double.class,
                int.class, int.class).invoke(null, posX, posY, posZ, velX, velY,
                velZ, fuzziness, spread, count, color);
        } catch (ReflectiveOperationException ignored) {
            // Client-only rendering is deliberately optional on a dedicated server.
        }
    }

    public static void register() {
        PaucalAPI.registerMessage(MsgCastParticlesS2C.class, Side.CLIENT);
    }
}
