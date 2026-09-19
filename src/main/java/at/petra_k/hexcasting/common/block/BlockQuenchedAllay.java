package at.petra_k.hexcasting.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * The solid block used to hold a quenched Allay's model.
 *
 * <p>Modern Hex renders the visible model from a block entity so that the
 * four gaslighting variants can be swapped without changing block state.
 * The visible model is supplied by the block-entity renderer, while the
 * solid block remains present for collision, selection, and interaction.</p>
 */
public final class BlockQuenchedAllay extends Block {
    public static final int VARIANTS = 4;
    private static final int PARTICLE_COLOR = 0x8932B8;

    private static boolean particleBridgeResolved;
    private static Method particleBridge;

    public BlockQuenchedAllay() {
        super(Material.ROCK);
        setHardness(1.5F);
        setResistance(6.0F);
        setSoundType(SoundType.STONE);
        // The block emits a little light so its translucent model is not
        // self-occluded, matching Hex's quenched() properties.
        setLightLevel(4.0F / 15.0F);
        setLightOpacity(0);
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        // The block entity renderer supplies the visible model.  Keeping the
        // block itself invisible matches modern Hex and prevents the chunk
        // renderer from trying to draw a second, empty block model.
        return EnumBlockRenderType.INVISIBLE;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityQuenchedAllay();
    }

    /** Emit the same sparse purple construction particles as Hex 1.20.1. */
    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos,
                                  Random rand) {
        if (!world.isRemote) {
            return;
        }

        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        for (EnumFacing direction : EnumFacing.values()) {
            int stepX = direction.getFrontOffsetX();
            int stepY = direction.getFrontOffsetY();
            int stepZ = direction.getFrontOffsetZ();

            int count = rand.nextInt(10) / 4;
            for (int i = 0; i < count; i++) {
                double particleX = centerX + (stepX == 0
                    ? randomBetween(rand, -0.5D, 0.5D) : stepX * 0.55D);
                double particleY = centerY + (stepY == 0
                    ? randomBetween(rand, -0.5D, 0.5D) : stepY * 0.55D);
                double particleZ = centerZ + (stepZ == 0
                    ? randomBetween(rand, -0.5D, 0.5D) : stepZ * 0.55D);
                double velocity = randomBetween(rand, 0.0D, 0.01D);
                spawnParticle(world, particleX, particleY, particleZ,
                    velocity * stepX, velocity * stepY, velocity * stepZ);
            }
        }
    }

    private static double randomBetween(Random rand, double min, double max) {
        return min + rand.nextDouble() * (max - min);
    }

    /** Keep the common block class free of client-only class references. */
    private static void spawnParticle(World world, double x, double y, double z,
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
                    motionZ, PARTICLE_COLOR);
            }
        } catch (ReflectiveOperationException ignored) {
            // The client bridge is not present on a dedicated server.
        }
    }
}
