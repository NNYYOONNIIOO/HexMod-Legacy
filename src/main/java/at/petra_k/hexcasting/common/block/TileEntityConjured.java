package at.petra_k.hexcasting.common.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.ITickable;

import java.lang.reflect.Method;
import java.util.Random;

/** Client particle state for Hex's invisible conjured block. */
public final class TileEntityConjured extends TileEntity implements ITickable {
    private static final String KEY_COLOR = "color";
    private static final int DEFAULT_COLOR = 0xAA66FF;
    private static final Random RANDOM = new Random();
    private static boolean particleBridgeResolved;
    private static Method particleBridge;
    private int color = DEFAULT_COLOR;

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color & 0xFFFFFF;
        markDirty();
        if (world != null) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger(KEY_COLOR, color);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        color = compound.hasKey(KEY_COLOR, 3)
            ? compound.getInteger(KEY_COLOR) & 0xFFFFFF : DEFAULT_COLOR;
    }

    @Override
    public void update() {
        // Modern Hex has a client-only block-entity ticker.  In 1.12 the
        // ITickable hook is shared by both sides, so explicitly keep the
        // server side empty; conjured blocks are persistent until replaced
        // or broken and must never count down on their own.
        if (world != null && world.isRemote) {
            particleEffect();
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        readFromNBT(packet.getNbtCompound());
    }

    private int red() {
        return (color >> 16) & 0xFF;
    }

    private int green() {
        return (color >> 8) & 0xFF;
    }

    private int blue() {
        return color & 0xFF;
    }

    private void spawnColoredParticle(double x, double y, double z,
                                      double motionX, double motionY,
                                      double motionZ) {
        if (world == null || !world.isRemote) {
            return;
        }
        try {
            if (!particleBridgeResolved) {
                particleBridgeResolved = true;
                Class<?> effects = Class.forName(
                    "at.petra_k.hexcasting.client.HexClientEffects");
                particleBridge = effects.getMethod("spawnConjureParticle",
                    double.class, double.class, double.class,
                    double.class, double.class, double.class, int.class);
            }
            if (particleBridge != null) {
                particleBridge.invoke(null, x, y, z, motionX, motionY,
                    motionZ, color);
            }
        } catch (ReflectiveOperationException ignored) {
            // The client-only particle bridge is unavailable on a server or
            // before the client renderer has been loaded.
        }
    }

    /** Client-side particle hook used by both the tile tick and block callback. */
    public void particleEffect() {
        if (world == null || !world.isRemote) {
            return;
        }
        if (!(world.getBlockState(pos).getBlock() instanceof BlockConjured)) {
            return;
        }
        boolean light = world.getBlockState(pos).getBlock()
            instanceof BlockConjuredLight;
        if (light) {
            if (RANDOM.nextFloat() >= 0.5F) {
                return;
            }
            spawnColoredParticle(
                pos.getX() + 0.45D + RANDOM.nextDouble() * 0.1D,
                pos.getY() + 0.45D + RANDOM.nextDouble() * 0.1D,
                pos.getZ() + 0.45D + RANDOM.nextDouble() * 0.1D,
                randomBetween(-0.005D, 0.005D),
                randomBetween(-0.002D, 0.02D),
                randomBetween(-0.005D, 0.005D));
        } else {
            if (RANDOM.nextFloat() >= 0.2F) {
                return;
            }
            spawnColoredParticle(
                pos.getX() + RANDOM.nextDouble(),
                pos.getY() + RANDOM.nextDouble(),
                pos.getZ() + RANDOM.nextDouble(),
                randomBetween(-0.02D, 0.02D),
                randomBetween(-0.02D, 0.02D),
                randomBetween(-0.02D, 0.02D));
        }
    }

    /** Client-side footstep hook retained for parity with modern Hex. */
    public void walkParticle(net.minecraft.entity.Entity entity) {
        if (world == null || !world.isRemote || entity == null) {
            return;
        }
        if (world.getBlockState(pos).getBlock() instanceof BlockConjuredLight) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            spawnColoredParticle(
                entity.posX + RANDOM.nextDouble() * 0.6D - 0.3D,
                pos.getY() + RANDOM.nextDouble() * 0.05D + 0.95D,
                entity.posZ + RANDOM.nextDouble() * 0.6D - 0.3D,
                randomBetween(-0.02D, 0.02D),
                RANDOM.nextDouble() * 0.02D,
                randomBetween(-0.02D, 0.02D));
        }
    }

    /**
     * Emit the short Hex-style cloud burst used when a conjured block is
     * destroyed.  This is called from the client-only Forge destroy-effects
     * hook before the tile entity is removed.
     */
    public void destroyParticle() {
        if (world == null || !world.isRemote) {
            return;
        }
        if (!(world.getBlockState(pos).getBlock() instanceof BlockConjured)) {
            return;
        }

        boolean light = world.getBlockState(pos).getBlock()
            instanceof BlockConjuredLight;
        int count = light ? 24 : 40;
        double spread = light ? 0.18D : 0.48D;
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        for (int i = 0; i < count; i++) {
            double offsetX = randomBetween(-spread, spread);
            double offsetY = randomBetween(-spread, spread);
            double offsetZ = randomBetween(-spread, spread);
            double length = Math.sqrt(offsetX * offsetX + offsetY * offsetY
                + offsetZ * offsetZ);
            if (length < 0.000001D) {
                offsetY = 1.0D;
                length = 1.0D;
            }
            double speed = randomBetween(0.018D, 0.055D);
            spawnColoredParticle(
                centerX + offsetX,
                centerY + offsetY,
                centerZ + offsetZ,
                offsetX / length * speed,
                offsetY / length * speed,
                offsetZ / length * speed);
        }
    }

    private static double randomBetween(double min, double max) {
        return min + RANDOM.nextDouble() * (max - min);
    }
}
