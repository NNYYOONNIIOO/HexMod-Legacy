package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.common.effect.HexPigmentSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 1.12.2 world renderer for Hex's client-only sentinel geometry. */
final class HexSentinelRenderer {
    private static final Map<UUID, SentinelVisual> SENTINELS = new HashMap<>();
    private static final float TWO_PI = (float) (Math.PI * 2.0D);
    private static final float RADIANS_TO_DEGREES = 180.0F / (float) Math.PI;

    private static final float[] TOP = {0.0F, 1.0F, 0.0F};
    private static final float[] BOTTOM = {0.0F, -1.0F, 0.0F};
    private static final float[][] TOP_RING = new float[5][];
    private static final float[][] BOTTOM_RING = new float[5][];

    static {
        float theta = (float) Math.atan2(0.5D, 1.0D);
        for (int i = 0; i < 5; i++) {
            float phi = (float) i / 5.0F * TWO_PI;
            float x = (float) Math.cos(theta) * (float) Math.cos(phi);
            float y = (float) Math.sin(theta);
            float z = (float) Math.cos(theta) * (float) Math.sin(phi);
            TOP_RING[i] = new float[] {x, y, z};
            BOTTOM_RING[i] = new float[] {-x, -y, -z};
        }
    }

    private HexSentinelRenderer() {
    }

    static void update(UUID playerUuid, boolean exists, boolean extendedRange,
                       double x, double y, double z, int dimension) {
        if (playerUuid == null) {
            return;
        }
        if (!exists) {
            SENTINELS.remove(playerUuid);
            return;
        }
        SENTINELS.put(playerUuid,
            new SentinelVisual(extendedRange, x, y, z, dimension));
    }

    static void clear() {
        SENTINELS.clear();
    }

    static void render(Minecraft minecraft, Entity camera, float partialTicks,
                       float visualTime) {
        if (minecraft == null || minecraft.world == null || camera == null
            || SENTINELS.isEmpty()) {
            return;
        }
        int dimension = minecraft.world.provider.getDimension();
        double cameraX = camera.lastTickPosX
            + (camera.posX - camera.lastTickPosX) * partialTicks;
        double cameraY = camera.lastTickPosY
            + (camera.posY - camera.lastTickPosY) * partialTicks;
        double cameraZ = camera.lastTickPosZ
            + (camera.posZ - camera.lastTickPosZ) * partialTicks;

        for (Map.Entry<UUID, SentinelVisual> entry : SENTINELS.entrySet()) {
            SentinelVisual sentinel = entry.getValue();
            if (sentinel.dimension != dimension) {
                continue;
            }
            EntityPlayer owner = minecraft.world.getPlayerEntityByUUID(entry.getKey());
            if (owner == null && minecraft.player != null
                && entry.getKey().equals(minecraft.player.getUniqueID())) {
                owner = minecraft.player;
            }
            if (owner == null || owner.isDead) {
                continue;
            }

            double dx = sentinel.x - cameraX;
            double dy = sentinel.y - cameraY;
            double dz = sentinel.z - cameraZ;
            if (dx * dx + dy * dy + dz * dz > 256.0D * 256.0D) {
                continue;
            }
            HexPigmentSource pigment = HexPigmentSource.resolvePlayer(owner);
            if (pigment == null) {
                pigment = HexPigmentSource.defaultSource();
            }
            renderSentinel(sentinel, pigment, visualTime,
                dx, dy, dz);
        }
    }

    private static void renderSentinel(SentinelVisual sentinel,
                                       HexPigmentSource pigment, float ticks,
                                       double x, double y, double z) {
        // ClientTickCounter.getTotal() / 2 in modern Hex. The world tick plus
        // partial tick gives the same smooth phase in the 1.12 renderer.
        float time = ticks / 2.0F;
        float spinRadians = time / 30.0F;

        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        GlStateManager.translate(x, y, z);
        GlStateManager.translate(0.0D,
            Math.sin(time / 20.0F) * 0.1D, 0.0D);
        GlStateManager.rotate(spinRadians * RADIANS_TO_DEGREES,
            0.0F, 1.0F, 0.0F);
        if (sentinel.extendedRange) {
            GlStateManager.rotate(spinRadians / 8.0F * RADIANS_TO_DEGREES,
                1.0F, 0.0F, 0.0F);
        }
        GlStateManager.scale(0.5F, 0.5F, 0.5F);

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glLineWidth(5.0F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        for (int side = 0; side <= 1; side++) {
            float[][] ring = side == 0 ? BOTTOM_RING : TOP_RING;
            float[] apex = side == 0 ? BOTTOM : TOP;
            for (int i = 0; i < 5; i++) {
                edge(buffer, apex, ring[i], pigment, time);
            }
            for (int i = 0; i < 5; i++) {
                edge(buffer, ring[i], ring[(i + 1) % 5], pigment, time);
            }
        }
        for (int i = 0; i < 5; i++) {
            float[] bottom = BOTTOM_RING[i];
            edge(buffer, TOP_RING[(i + 2) % 5], bottom, pigment, time);
            edge(buffer, bottom, TOP_RING[(i + 3) % 5], pigment, time);
        }
        tessellator.draw();

        GL11.glLineWidth(1.0F);
        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }

    private static void edge(BufferBuilder buffer, float[] left, float[] right,
                             HexPigmentSource pigment, float time) {
        vertex(buffer, left, pigment.sample(time, left[0], left[1], left[2]));
        vertex(buffer, right, pigment.sample(time, right[0], right[1], right[2]));
    }

    private static void vertex(BufferBuilder buffer, float[] point, int color) {
        buffer.pos(point[0], point[1], point[2])
            .color((color >> 16) & 0xFF, (color >> 8) & 0xFF,
                color & 0xFF, 255).endVertex();
    }

    private static final class SentinelVisual {
        private final boolean extendedRange;
        private final double x;
        private final double y;
        private final double z;
        private final int dimension;

        private SentinelVisual(boolean extendedRange, double x, double y,
                               double z, int dimension) {
            this.extendedRange = extendedRange;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
        }
    }
}
