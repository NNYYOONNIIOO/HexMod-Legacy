package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.api.casting.math.HexCoord;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Client-side particles and world-space rune orbit used by staff casting. */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID, value = Side.CLIENT)
public final class HexClientEffects {
    private static final double SQRT_3 = Math.sqrt(3.0D);
    private static final Map<UUID, List<OrbitPattern>> ORBITS =
        new HashMap<>();
    private static final Random PARTICLE_RANDOM = new Random();
    private static TextureAtlasSprite CONJURE_SPRITE;

    private HexClientEffects() {
    }

    /** Called by MsgCastingPatternS2C through the common-side bridge. */
    public static void addSpiralPattern(UUID playerUuid, HexPattern pattern,
                                        int lifetime, int color) {
        if (playerUuid == null || pattern == null) {
            return;
        }
        List<OrbitPattern> patterns = ORBITS.get(playerUuid);
        if (patterns == null) {
            patterns = new ArrayList<>();
            ORBITS.put(playerUuid, patterns);
        }
        for (OrbitPattern existing : patterns) {
            if (existing.pattern.equals(pattern)) {
                existing.lifetime = Math.max(existing.lifetime, lifetime);
                existing.maxLifetime = Math.max(existing.maxLifetime,
                    finiteLifetime(lifetime));
                existing.color = 0xFF000000 | (color & 0xFFFFFF);
                return;
            }
        }
        if (patterns.size() >= 100) {
            patterns.remove(0);
        }
        int safeLifetime = lifetime <= 0 ? 1 : lifetime;
        patterns.add(new OrbitPattern(pattern, safeLifetime,
            finiteLifetime(safeLifetime), color));
    }

    /** Called by MsgClearCastingPatternsS2C; clear with the same soft fade as Hex. */
    public static void clearSpiralPatterns(UUID playerUuid) {
        // A staff clear is an explicit user action. Remove the client cache
        // immediately so no stale ring can survive a GUI reopen or a delayed
        // authoritative program snapshot.
        ORBITS.remove(playerUuid);
    }

    /** Called by MsgCastParticlesS2C through the common-side bridge. */
    public static void spawnSpray(double posX, double posY, double posZ,
                                  double velX, double velY, double velZ,
                                  double fuzziness, double spread, int count,
                                  int color) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.world == null) {
            return;
        }
        int amount = Math.max(0, Math.min(256, count));
        if (amount == 0) {
            return;
        }
        double radius = Math.max(0.0D, fuzziness);
        double maxSpread = Math.max(0.0D, Math.min(Math.PI, spread));
        Vec3d base = new Vec3d(velX, velY, velZ);
        if (base.lengthVector() < 0.00001D) {
            base = new Vec3d(0.0D, 1.0D, 0.0D);
        }
        base = base.normalize();
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        TextureAtlasSprite sprite = CONJURE_SPRITE;

        for (int i = 0; i < amount; i++) {
            // Uniform-ish spherical offset, matching ParticleSpray's fuzzy origin.
            double theta = PARTICLE_RANDOM.nextDouble() * Math.PI * 2.0D;
            double z = PARTICLE_RANDOM.nextDouble() * 2.0D - 1.0D;
            double radial = Math.sqrt(Math.max(0.0D, 1.0D - z * z));
            double distance = PARTICLE_RANDOM.nextDouble() * radius * 0.5D;
            double xOffset = radial * Math.cos(theta) * distance;
            double yOffset = z * distance;
            double zOffset = radial * Math.sin(theta) * distance;

            double speed = 0.025D + base.lengthVector() * 0.018D;
            Vec3d particleMotion = base.scale(speed);
            if (maxSpread > 0.01D) {
                double drift = PARTICLE_RANDOM.nextDouble() * maxSpread;
                Vec3d driftDir = base.add(new Vec3d(
                    (PARTICLE_RANDOM.nextDouble() - 0.5D) * drift,
                    (PARTICLE_RANDOM.nextDouble() - 0.5D) * drift,
                    (PARTICLE_RANDOM.nextDouble() - 0.5D) * drift)).normalize();
                particleMotion = driftDir.scale(speed);
            }
            if (sprite != null) {
                minecraft.effectRenderer.addEffect(HexConjureParticle.create(
                    minecraft.world, posX + xOffset, posY + yOffset,
                    posZ + zOffset, particleMotion.x, particleMotion.y,
                    particleMotion.z, sprite, color));
            }
        }
    }

    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Pre event) {
        TextureMap map = event.getMap();
        if (map != null) {
            CONJURE_SPRITE = map.registerSprite(
                HexAPI.modLoc("particle/cloud"));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.world == null) {
            ORBITS.clear();
            return;
        }
        Iterator<Map.Entry<UUID, List<OrbitPattern>>> owners =
            ORBITS.entrySet().iterator();
        while (owners.hasNext()) {
            List<OrbitPattern> patterns = owners.next().getValue();
            Iterator<OrbitPattern> iterator = patterns.iterator();
            while (iterator.hasNext()) {
                OrbitPattern pattern = iterator.next();
                if (pattern.lifetime != Integer.MAX_VALUE) {
                    pattern.lifetime--;
                }
                if (pattern.lifetime <= 0) {
                    iterator.remove();
                }
            }
            if (patterns.isEmpty()) {
                owners.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.world == null || ORBITS.isEmpty()) {
            return;
        }
        Entity camera = minecraft.getRenderViewEntity();
        if (camera == null) {
            return;
        }
        float partialTicks = event.getPartialTicks();
        double cameraX = camera.lastTickPosX
            + (camera.posX - camera.lastTickPosX) * partialTicks;
        double cameraY = camera.lastTickPosY
            + (camera.posY - camera.lastTickPosY) * partialTicks;
        double cameraZ = camera.lastTickPosZ
            + (camera.posZ - camera.lastTickPosZ) * partialTicks;
        long worldTime = minecraft.world.getTotalWorldTime();

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(
            GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
            GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        for (Map.Entry<UUID, List<OrbitPattern>> owner : ORBITS.entrySet()) {
            EntityPlayer player = minecraft.world.getPlayerEntityByUUID(owner.getKey());
            if (player == null || player.isDead) {
                continue;
            }
            double playerX = player.lastTickPosX
                + (player.posX - player.lastTickPosX) * partialTicks;
            double playerY = player.lastTickPosY
                + (player.posY - player.lastTickPosY) * partialTicks;
            double playerZ = player.lastTickPosZ
                + (player.posZ - player.lastTickPosZ) * partialTicks;
            double dx = playerX - cameraX;
            double dy = playerY - cameraY;
            double dz = playerZ - cameraZ;
            if (dx * dx + dy * dy + dz * dz > 128.0D * 128.0D) {
                continue;
            }
            for (int i = 0; i < owner.getValue().size(); i++) {
                renderPattern(owner.getValue().get(i), i, player,
                    partialTicks, worldTime, dx, dy, dz);
            }
        }

        GL11.glLineWidth(1.0F);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static void renderPattern(OrbitPattern orbit, int index,
                                      EntityPlayer player, float partialTicks,
                                      long worldTime, double x, double y, double z) {
        List<HexCoord> positions = orbit.pattern.positions();
        if (positions.size() < 2) {
            return;
        }
        Bounds bounds = bounds(positions);
        double centerX = (bounds.minX + bounds.maxX) * 0.5D;
        double centerY = (bounds.minY + bounds.maxY) * 0.5D;
        double maxDx = Math.max(Math.abs(bounds.minX - centerX),
            Math.abs(bounds.maxX - centerX));
        double maxDy = Math.max(Math.abs(bounds.minY - centerY),
            Math.abs(bounds.maxY - centerY));
        // Hex's ClientRenderHelper uses 1/24 world units and caps the
        // centered glyph at 3.8 pose units (about half a block wide). The
        // previous port fitted every glyph to 1.45 blocks, making large
        // symbols dominate the scene.
        double poseScale = 3.8D;
        if (maxDx > 0.00001D) {
            poseScale = Math.min(poseScale, 6.4D / maxDx);
        }
        if (maxDy > 0.00001D) {
            poseScale = Math.min(poseScale, 6.4D / maxDy);
        }
        double fit = poseScale / 24.0D;
        float lifeAlpha = orbit.alpha();
        if (lifeAlpha <= 0.01F) {
            return;
        }
        float time = (float) worldTime + partialTicks;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        // Keep the exact slow angular motion used by Hex's
        // ClientRenderHelper.  The earlier fixed 1.35 degrees/tick term
        // made the rings visibly spin several times too quickly.
        float rotation = (time * ((float) Math.sin(index * 12.543565D)
            * 3.4F) * (index / 12.43F)) % 360.0F
            + (index + 1) * 45.0F;
        GlStateManager.rotate(rotation, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(0.0D,
            1.0D + Math.sin(index) * 0.75D + Math.floor(index / 8.0D),
            0.72D + Math.cos(index / 8.0D) * 0.25D
                + Math.cos(time / (7.0D + index / 4.0D)) * 0.065D);
        GlStateManager.translate(0.0D,
            Math.sin(time / (7.0D + index / 8.0D)) * 0.08D, 0.0D);
        GlStateManager.scale((float) fit, (float) fit, (float) fit);
        GlStateManager.translate(-centerX, -centerY, 0.0D);
        // This translation is deliberately after the scale, matching Hex's
        // frame-relative bob and avoiding visible positional stepping.
        GlStateManager.translate(0.0D,
            Math.sin(time / (7.0D + index / 8.0D)), 0.0D);

        List<double[]> line = jitteredPoints(positions, time, index);
        int alpha = Math.max(0, Math.min(255, (int) (lifeAlpha * 255.0F)));
        int rgb = orbit.color & 0x00FFFFFF;
        int outerColor = (Math.max(20, alpha / 3) << 24) | rgb;
        int innerColor = (alpha << 24) | rgb;
        drawLine(line, outerColor, 5.0F);
        drawLine(line, innerColor, 1.45F);
        GlStateManager.popMatrix();
    }

    private static List<double[]> jitteredPoints(List<HexCoord> positions,
                                                 float time, int patternIndex) {
        List<double[]> points = new ArrayList<>();
        for (int segment = 0; segment < positions.size() - 1; segment++) {
            double[] start = xy(positions.get(segment));
            double[] end = xy(positions.get(segment + 1));
            if (segment == 0) {
                points.add(start);
            }
            double dx = end[0] - start[0];
            double dy = end[1] - start[1];
            double length = Math.sqrt(dx * dx + dy * dy);
            double nx = length < 0.00001D ? 0.0D : -dy / length;
            double ny = length < 0.00001D ? 0.0D : dx / length;
            for (int step = 1; step <= 4; step++) {
                double amount = step / 4.0D;
                double jitter = step == 4 ? 0.0D
                    : Math.sin(time * 0.12D + patternIndex * 1.71D
                        + segment * 2.37D + step * 0.91D) * 0.035D;
                points.add(new double[] {
                    start[0] + dx * amount + nx * jitter,
                    start[1] + dy * amount + ny * jitter
                });
            }
        }
        return points;
    }

    private static void drawLine(List<double[]> points, int color, float width) {
        if (points.size() < 2) {
            return;
        }
        GL11.glLineWidth(width);
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        int alpha = (color >> 24) & 0xFF;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (double[] point : points) {
            buffer.pos(point[0], point[1], 0.0D)
                .color(red, green, blue, alpha).endVertex();
        }
        tessellator.draw();
    }

    private static double[] xy(HexCoord coord) {
        return new double[] {
            (SQRT_3 * coord.getQ() + SQRT_3 * 0.5D * coord.getR()),
            1.5D * coord.getR()
        };
    }

    private static Bounds bounds(List<HexCoord> positions) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (HexCoord position : positions) {
            double[] point = xy(position);
            minX = Math.min(minX, point[0]);
            minY = Math.min(minY, point[1]);
            maxX = Math.max(maxX, point[0]);
            maxY = Math.max(maxY, point[1]);
        }
        return new Bounds(minX, minY, maxX, maxY);
    }

    private static int finiteLifetime(int lifetime) {
        return lifetime == Integer.MAX_VALUE ? 140 : Math.max(1, lifetime);
    }

    private static final class OrbitPattern {
        private final HexPattern pattern;
        private int lifetime;
        private int maxLifetime;
        private int color;

        private OrbitPattern(HexPattern pattern, int lifetime, int maxLifetime, int color) {
            this.pattern = pattern;
            this.lifetime = lifetime;
            this.maxLifetime = maxLifetime;
            this.color = 0xFF000000 | (color & 0xFFFFFF);
        }

        private float alpha() {
            if (lifetime == Integer.MAX_VALUE) {
                return 1.0F;
            }
            if (lifetime <= 5) {
                return Math.max(0.0F, lifetime / 5.0F);
            }
            if (lifetime <= 60) {
                return Math.max(0.0F, Math.min(1.0F, lifetime / 60.0F));
            }
            return 1.0F;
        }
    }

    private static final class Bounds {
        private final double minX;
        private final double minY;
        private final double maxX;
        private final double maxY;

        private Bounds(double minX, double minY, double maxX, double maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }
    }
}
