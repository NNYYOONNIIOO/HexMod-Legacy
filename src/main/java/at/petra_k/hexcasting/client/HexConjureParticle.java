package at.petra_k.hexcasting.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.util.Random;

/**
 * The small additive-looking cloud used by Hex instead of Minecraft's
 * potion/spell particle.  The sprite is the original Hex cloud texture,
 * tinted by the caster's pigment.
 */
final class HexConjureParticle extends Particle {
    private static final Random RANDOM = new Random();

    private HexConjureParticle(World world, double x, double y, double z,
                               double motionX, double motionY, double motionZ,
                               TextureAtlasSprite sprite, int color) {
        super(world, x, y, z, motionX, motionY, motionZ);
        setParticleTexture(sprite);
        setRBGColorF(((color >> 16) & 0xFF) / 255.0F,
            ((color >> 8) & 0xFF) / 255.0F,
            (color & 0xFF) / 255.0F);
        particleAlpha = 0.30F;
        // 1.12.2 renders particleScale as 0.1 * particleScale, whereas
        // 1.20.1 renders quadSize directly.  Modern Hex starts with a
        // 0.1..0.2 quad and applies 0.9, so the equivalent 1.12 value is
        // 0.9..1.8 here.  Using 0.18 directly makes the cloud about ten
        // times too small.
        particleScale = 0.9F + RANDOM.nextFloat() * 0.9F;
        particleAngle = RANDOM.nextFloat() * (float) (Math.PI * 2.0D);
        prevParticleAngle = particleAngle;
        particleMaxAge = (int) (64.0D
            / ((RANDOM.nextDouble() + 3.0D) * 0.25D));
        particleGravity = motionX != 0.0D && motionY != 0.0D
            && motionZ != 0.0D ? -0.01F : 0.0F;
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
        canCollide = false;
    }

    static HexConjureParticle create(World world, double x, double y, double z,
                                     double motionX, double motionY, double motionZ,
                                     TextureAtlasSprite sprite, int color) {
        return new HexConjureParticle(world, x, y, z, motionX, motionY,
            motionZ, sprite, color);
    }

    @Override
    public int getFXLayer() {
        // Layer 1 is the translucent particle atlas in 1.12.2.
        return 1;
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (particleAge++ >= particleMaxAge) {
            setExpired();
            return;
        }
        motionY -= 0.04D * particleGravity;
        move(motionX, motionY, motionZ);
        motionX *= 0.96D;
        motionY *= 0.96D;
        motionZ *= 0.96D;
        particleAlpha = 0.30F
            * (1.0F - (float) particleAge / (float) particleMaxAge);
        particleScale *= 0.96F;
    }
}
