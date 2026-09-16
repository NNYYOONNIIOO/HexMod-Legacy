package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.casting.math.HexCoord;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import at.petra_k.hexcasting.common.casting.StaffCastExecutor;
import at.petra_k.hexcasting.common.block.BlockConjuredLight;
import at.petra_k.hexcasting.common.item.ItemColorizer;
import at.petra_k.hexcasting.common.item.ItemHexStaff;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Client-side particles and world-space rune orbit used by staff casting. */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID, value = Side.CLIENT)
public final class HexClientEffects {
    private static final double SQRT_3 = Math.sqrt(3.0D);
    private static final Map<UUID, List<OrbitPattern>> ORBITS =
        new HashMap<>();
    /**
     * Hex's 1.20.1 particle has its own additive render type.  1.12.2's
     * ParticleManager only offers the vanilla alpha-blended layers, so these
     * particles are kept in a small client queue and rendered in the world
     * event with the same blend state as the upstream render type.
     */
    private static final List<HexConjureParticle> CONJURE_PARTICLES =
        new ArrayList<>();
    private static final int MAX_CONJURE_PARTICLES = 4096;
    private static final Random PARTICLE_RANDOM = new Random();
    private static TextureAtlasSprite CONJURE_SPRITE;
    private static World PARTICLE_WORLD;
    /** Number of client ticks for which a newly loaded world retries staff restoration. */
    private static int ORBIT_RESTORE_TICKS;

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
        // An explicit server clear must win over the short world-load retry
        // window; otherwise a stale client-side staff copy could immediately
        // put the old ring back.
        if (Minecraft.getMinecraft().player != null
            && Minecraft.getMinecraft().player.getUniqueID().equals(playerUuid)) {
            ORBIT_RESTORE_TICKS = 0;
        }
    }

    /**
     * Recreate the local player's orbit from the synchronized staff NBT.
     * Server packets are intentionally supplemented by this client-side
     * fallback because login/respawn packets can arrive before the new client
     * world has installed its player entity.
     */
    public static void restoreOrbitPatterns() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.world == null || minecraft.player == null) {
            return;
        }
        restoreOrbitPatterns(minecraft.player);
    }

    private static void restoreOrbitPatterns(EntityPlayer player) {
        if (player == null) {
            return;
        }
        int pigment = localPigment(player);
        for (ItemStack staff : player.inventory.mainInventory) {
            restoreStaffPatterns(player, staff, pigment);
        }
        restoreStaffPatterns(player, player.getHeldItemOffhand(), pigment);
    }

    private static void restoreStaffPatterns(EntityPlayer player, ItemStack staff,
                                             int pigment) {
        if (!ItemHexStaff.isStaff(staff)) {
            return;
        }
        StaffCastExecutor.Resolution[] resolutions =
            StaffCastExecutor.Resolution.values();
        for (ItemHexStaff.ProgramEntry entry : ItemHexStaff.getProgramEntries(staff)) {
            if (entry == null || entry.getPattern() == null) {
                continue;
            }
            int ordinal = entry.getResolutionOrdinal();
            StaffCastExecutor.Resolution resolution = ordinal >= 0
                && ordinal < resolutions.length
                ? resolutions[ordinal] : StaffCastExecutor.Resolution.UNRESOLVED;
            boolean error = resolution == StaffCastExecutor.Resolution.ERRORED
                || resolution == StaffCastExecutor.Resolution.INVALID;
            addSpiralPattern(player.getUniqueID(), entry.getPattern(),
                error ? 36 : Integer.MAX_VALUE,
                error ? 0xE05252 : pigment);
        }
    }

    private static int localPigment(EntityPlayer player) {
        try {
            int mainhandColor = ItemColorizer.getColor(player.getHeldItemMainhand());
            if (mainhandColor >= 0) {
                return mainhandColor & 0xFFFFFF;
            }
            int offhandColor = ItemColorizer.getColor(player.getHeldItemOffhand());
            if (offhandColor >= 0) {
                return offhandColor & 0xFFFFFF;
            }
            IHexCastingData data = player.getCapability(
                HexCapabilities.CASTING_DATA, null);
            return data == null ? 0xAA66FF : data.getPigment();
        } catch (RuntimeException ignored) {
            return 0xAA66FF;
        }
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
        Vec3d suppliedVelocity = new Vec3d(velX, velY, velZ);
        double originalSpeed = suppliedVelocity.lengthVector();
        Vec3d direction = originalSpeed < 0.0000001D
            ? new Vec3d(0.0D, 0.0D, 0.0D)
            : suppliedVelocity.scale(1.0D / originalSpeed);
        TextureAtlasSprite sprite = CONJURE_SPRITE;

        for (int i = 0; i < amount; i++) {
            // This is the same randomInCircle/velocity-cone construction as
            // MsgCastParticleS2C in Hex 1.20.1.  In particular, the final
            // speed is the supplied velocity length divided by 20; the old
            // port normalized first and then invented a fixed speed, which
            // made clouds and bursts visibly too slow or too fast.
            Vec3d offsetDirection = randomUnitVector(Math.PI * 2.0D);
            double distance = PARTICLE_RANDOM.nextDouble() * radius * 0.5D;
            Vec3d offset = offsetDirection.scale(distance);

            double phi = Math.acos(1.0D - PARTICLE_RANDOM.nextDouble()
                * (1.0D - Math.cos(maxSpread)));
            double theta = Math.PI * 2.0D * PARTICLE_RANDOM.nextDouble();
            Vec3d normal = direction.x == 0.0D && direction.y == 0.0D
                ? new Vec3d(1.0D, 0.0D, 0.0D)
                : direction.crossProduct(new Vec3d(0.0D, 0.0D, 1.0D));
            Vec3d particleMotion = direction.scale(Math.cos(phi))
                .add(normal.scale(Math.sin(phi) * Math.cos(theta)))
                .add(direction.crossProduct(normal)
                    .scale(Math.sin(phi) * Math.sin(theta)))
                .scale(originalSpeed / 20.0D);
            if (sprite != null) {
                addConjureParticle(HexConjureParticle.create(
                    minecraft.world, posX + offset.x, posY + offset.y,
                    posZ + offset.z, particleMotion.x, particleMotion.y,
                    particleMotion.z, sprite, color));
            }
        }
    }

    /**
     * Spawn the same custom cloud used by conjured blocks and spell sprays.
     * The common block entity calls this through a tiny reflective bridge so
     * a dedicated server never has to resolve client particle classes.
     */
    public static void spawnConjureParticle(double posX, double posY,
                                            double posZ, double motionX,
                                            double motionY, double motionZ,
                                            int color) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.world == null
            || CONJURE_SPRITE == null) {
            return;
        }
        addConjureParticle(HexConjureParticle.create(
            minecraft.world, posX, posY, posZ, motionX, motionY, motionZ,
            CONJURE_SPRITE, color));
    }

    private static void addConjureParticle(HexConjureParticle particle) {
        if (particle == null) {
            return;
        }
        if (CONJURE_PARTICLES.size() >= MAX_CONJURE_PARTICLES) {
            CONJURE_PARTICLES.remove(0);
        }
        CONJURE_PARTICLES.add(particle);
    }

    /** Uniformly sample a unit vector on the sphere, as Hex's helper does. */
    private static Vec3d randomUnitVector(double maxTheta) {
        double theta = PARTICLE_RANDOM.nextDouble() * (maxTheta + 0.001D);
        double z = PARTICLE_RANDOM.nextDouble() * 2.0D - 1.0D;
        double radial = Math.sqrt(Math.max(0.0D, 1.0D - z * z));
        return new Vec3d(radial * Math.cos(theta),
            radial * Math.sin(theta), z);
    }

    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Pre event) {
        TextureMap map = event.getMap();
        if (map != null) {
            CONJURE_SPRITE = map.registerSprite(
                HexAPI.modLoc("particle/cloud"));
        }
    }

    /**
     * Vanilla 1.12.2 skips the selection-box renderer for Material.AIR. A
     * conjured light deliberately keeps that material so ordinary blocks can
     * replace it, so draw only its small target box through Forge's highlight
     * hook while leaving the block intangible.
     */
    @SubscribeEvent
    public static void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
        if (event == null || event.getSubID() != 0
            || event.getTarget() == null
            || event.getTarget().typeOfHit != RayTraceResult.Type.BLOCK
            || event.getPlayer() == null) {
            return;
        }
        EntityPlayer player = event.getPlayer();
        BlockPos pos = event.getTarget().getBlockPos();
        if (pos == null || player.world == null
            || !player.world.getWorldBorder().contains(pos)) {
            return;
        }
        IBlockState state = player.world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockConjuredLight)) {
            return;
        }

        double cameraX = player.lastTickPosX
            + (player.posX - player.lastTickPosX) * event.getPartialTicks();
        double cameraY = player.lastTickPosY
            + (player.posY - player.lastTickPosY) * event.getPartialTicks();
        double cameraZ = player.lastTickPosZ
            + (player.posZ - player.lastTickPosZ) * event.getPartialTicks();
        AxisAlignedBB box = state.getSelectedBoundingBox(player.world, pos)
            .grow(0.0020000000949949026D)
            .offset(-cameraX, -cameraY, -cameraZ);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);
        GlStateManager.glLineWidth(2.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        RenderGlobal.drawSelectionBoundingBox(box, 0.0F, 0.0F, 0.0F, 0.4F);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.world == null) {
            ORBITS.clear();
            CONJURE_PARTICLES.clear();
            PARTICLE_WORLD = null;
            ORBIT_RESTORE_TICKS = 0;
            return;
        }
        if (PARTICLE_WORLD != minecraft.world) {
            PARTICLE_WORLD = minecraft.world;
            ORBITS.clear();
            CONJURE_PARTICLES.clear();
            ORBIT_RESTORE_TICKS = 40;
            restoreOrbitPatterns(minecraft.player);
        }
        if (ORBIT_RESTORE_TICKS > 0) {
            restoreOrbitPatterns(minecraft.player);
            ORBIT_RESTORE_TICKS--;
        }
        Iterator<HexConjureParticle> particles = CONJURE_PARTICLES.iterator();
        while (particles.hasNext()) {
            HexConjureParticle particle = particles.next();
            particle.onUpdate();
            if (!particle.isAlive()) {
                particles.remove();
            }
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
        if (minecraft == null || minecraft.world == null) {
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

        if (!ORBITS.isEmpty()) {
            GlStateManager.pushMatrix();
            beginOrbitRender();

            boolean inventoryPlayerRender = minecraft.currentScreen instanceof GuiInventory;
            for (Map.Entry<UUID, List<OrbitPattern>> owner : ORBITS.entrySet()) {
                EntityPlayer player = minecraft.world.getPlayerEntityByUUID(owner.getKey());
                if (player == null || player.isDead) {
                    continue;
                }
                if (inventoryPlayerRender && player == minecraft.player) {
                    // GuiInventory renders a second copy of the player through
                    // RenderPlayerEvent.Pre; do not draw the world copy below
                    // the inventory model as well.
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

            endOrbitRender(false);
            GlStateManager.popMatrix();
        }
        renderConjureParticles(minecraft, camera, partialTicks);
    }

    /**
     * The inventory screen renders the player through RenderManager rather
     * than through the world pass.  Mirror Hex's PlayerRenderer hook so the
     * same floating stack is visible around that model as well.
     */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || !(minecraft.currentScreen instanceof GuiInventory)
            || event == null || event.getEntityPlayer() == null
            || event.getEntityPlayer() != minecraft.player) {
            return;
        }
        List<OrbitPattern> patterns = ORBITS.get(event.getEntityPlayer().getUniqueID());
        if (patterns == null || patterns.isEmpty()) {
            return;
        }

        GlStateManager.pushMatrix();
        beginOrbitRender();
        long worldTime = event.getEntityPlayer().world.getTotalWorldTime();
        for (int i = 0; i < patterns.size(); i++) {
            renderPattern(patterns.get(i), i, event.getEntityPlayer(),
                event.getPartialRenderTick(), worldTime, 0.0D, 0.0D, 0.0D);
        }
        endOrbitRender(true);
        GlStateManager.popMatrix();
    }

    /** Render the queued cloud with Hex's SRC_ALPHA/ONE blend mode. */
    private static void renderConjureParticles(Minecraft minecraft,
                                                Entity camera,
                                                float partialTicks) {
        if (CONJURE_PARTICLES.isEmpty() || CONJURE_SPRITE == null) {
            return;
        }
        Particle.interpPosX = camera.lastTickPosX
            + (camera.posX - camera.lastTickPosX) * partialTicks;
        Particle.interpPosY = camera.lastTickPosY
            + (camera.posY - camera.lastTickPosY) * partialTicks;
        Particle.interpPosZ = camera.lastTickPosZ
            + (camera.posZ - camera.lastTickPosZ) * partialTicks;
        Particle.cameraViewDir = camera.getLook(partialTicks);

        Minecraft.getMinecraft().getTextureManager().bindTexture(
            TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GL11.GL_SRC_ALPHA, GL11.GL_ONE,
            GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.003921569F);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS,
            DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
        float rotationX = ActiveRenderInfo.getRotationX();
        float rotationZ = ActiveRenderInfo.getRotationZ();
        float rotationYZ = ActiveRenderInfo.getRotationYZ();
        float rotationXY = ActiveRenderInfo.getRotationXY();
        float rotationXZ = ActiveRenderInfo.getRotationXZ();
        for (HexConjureParticle particle : CONJURE_PARTICLES) {
            particle.renderParticle(buffer, camera, partialTicks,
                rotationX, rotationXZ, rotationZ, rotationYZ, rotationXY);
        }
        Tessellator.getInstance().draw();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
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
        if (orbit.lifetime != Integer.MAX_VALUE && orbit.lifetime <= 5) {
            fit *= Math.max(0.0D, orbit.lifetime / 5.0D);
        }
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
            1.0D + Math.sin(index) * 0.75D,
            0.75D + Math.cos(index / 8.0D) * 0.25D
                + Math.cos(time / (7.0D + index / 4.0D)) * 0.065D);
        GlStateManager.scale((float) fit, (float) fit, (float) fit);
        GlStateManager.translate(0.0D, Math.floor(index / 8.0D), 0.0D);
        GlStateManager.translate(0.0D,
            Math.sin(time / (7.0D + index / 8.0D)), 0.0D);
        GlStateManager.translate(-centerX, -centerY, 0.0D);

        // Hex 1.20.1 uses a five-hop, noise-driven "zappy" line rather than
        // a straight GL_LINE_STRIP.  Keep the points in pattern space and
        // let the pose scale below convert them to world units.
        // The upstream renderer uses player.hashCode() as its seed. Keep the
        // same value; the simplex implementation handles the resulting
        // coordinates without the old port's arbitrary 1e-6 rescaling.
        List<double[]> line = makeZappy(positions, time, index,
            player == null ? 0L : (long) player.hashCode());
        int alpha = Math.max(0, Math.min(255, (int) (lifeAlpha * 255.0F)));
        int rgb = orbit.color & 0x00FFFFFF;
        int outerColor = (alpha << 24) | rgb;
        int innerColor = screenColor(outerColor);
        // The original uses pose-space widths 0.35 and 0.14.  Our pattern
        // coordinates are scaled by fit, so convert those widths back to
        // pattern space before building the quads.
        float outerWidth = (float) (0.35D / Math.max(0.001D, poseScale));
        float innerWidth = (float) (0.14D / Math.max(0.001D, poseScale));
        drawLine(line, outerColor, outerWidth, 0.0D);
        // The camera looks down -Z.  The 1.20.1 renderer flips its local Z
        // axis before drawing, which puts the bright wash line in front of
        // the broad line.  Keep that ordering explicitly in the 1.12 port.
        drawLine(line, innerColor, innerWidth, -0.012D);
        GlStateManager.popMatrix();
    }

    private static void beginOrbitRender() {
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(
            GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
            GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableDepth();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void endOrbitRender(boolean keepBlendEnabled) {
        GL11.glLineWidth(1.0F);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        if (keepBlendEnabled) {
            GlStateManager.enableBlend();
        } else {
            GlStateManager.disableBlend();
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Java 8 port of Hex's RenderLib.makeZappy. */
    private static List<double[]> makeZappy(List<HexCoord> positions,
                                             float time, int patternIndex,
                                             long seed) {
        List<double[]> bare = new ArrayList<>();
        for (HexCoord position : positions) {
            bare.add(xy(position));
        }
        if (bare.size() <= 1) {
            return bare;
        }

        Set<Integer> duplicateIndices = findDuplicateIndices(positions);
        List<double[]> output = new ArrayList<>(bare.size() * 6);
        List<double[]> daisyChain = new ArrayList<>();
        final float readabilityOffset = 0.0F;
        for (int i = 0; i < bare.size() - 1; i++) {
            double[] head = bare.get(i);
            double[] tail = bare.get(i + 1);
            double[] tangent = scale(subtract(tail, head), readabilityOffset);
            if (i != 0 && duplicateIndices.contains(i)) {
                daisyChain.add(add(head, tangent));
            } else {
                daisyChain.add(copy(head));
            }

            if (i == bare.size() - 2) {
                daisyChain.add(copy(tail));
                output.addAll(zappify(daisyChain, time, patternIndex, seed, i, true));
            } else if (duplicateIndices.contains(i + 1)) {
                daisyChain.add(subtract(tail, tangent));
                output.addAll(zappify(daisyChain, time, patternIndex, seed, i, false));
                daisyChain.clear();
            }
        }
        return output;
    }

    private static List<double[]> zappify(List<double[]> points, float time,
                                          int patternIndex, long seed,
                                          int segmentOffset, boolean drawLast) {
        List<double[]> out = new ArrayList<>();
        if (points.size() <= 1) {
            return out;
        }
        final int hops = 5;
        final double variance = 0.65D;
        final double speed = 0.1D;
        final double zSeed = time * speed;
        out.add(copy(points.get(0)));
        for (int i = 0; i < points.size() - 1; i++) {
            double[] source = points.get(i);
            double[] target = points.get(i + 1);
            double dx = target[0] - source[0];
            double dy = target[1] - source[1];
            double length = Math.sqrt(dx * dx + dy * dy);
            if (length < 0.000001D) {
                // Preserve repeated nodes without feeding a NaN normal into
                // the renderer.
                out.add(copy(target));
                continue;
            }
            double hopDistance = length / hops;
            double maxVariance = hopDistance * variance;
            for (int j = 1; j <= hops; j++) {
                // This is deliberately j/(hops+1), as in the upstream
                // implementation: the endpoint is added separately.
                double progress = j / (double) (hops + 1);
                double minorPerturb = noise(i + 31.0D, j + 17.0D,
                    Math.sin(zSeed)) * 0.2D;
                double theta = 3.0D * noise(i + progress + minorPerturb - zSeed,
                    1337.0D, seed) * Math.PI * 2.0D;
                double taper = Math.min(1.0D,
                    8.0D * (0.5D - Math.abs(0.5D - progress)));
                double radius = noise(i + progress - zSeed,
                    69420.0D, seed) * maxVariance * taper;
                double[] point = new double[] {
                    source[0] + dx * progress + Math.cos(theta) * radius,
                    source[1] + dy * progress + Math.sin(theta) * radius
                };
                out.add(point);
            }
            // A daisy-chain segment may intentionally stop before its final
            // node; all other segments include the exact target.
            if (i < points.size() - 2 || drawLast) {
                out.add(copy(target));
            }
        }
        return out;
    }

    private static Set<Integer> findDuplicateIndices(List<HexCoord> positions) {
        Map<HexCoord, Integer> first = new HashMap<>();
        Set<Integer> duplicates = new HashSet<>();
        for (int i = 0; i < positions.size(); i++) {
            Integer previous = first.get(positions.get(i));
            if (previous != null) {
                duplicates.add(previous);
                duplicates.add(i);
            } else {
                first.put(positions.get(i), i);
            }
        }
        return duplicates;
    }

    /** The same seeded simplex source used by Hex's 1.20.1 RenderLib. */
    private static final SimplexNoise ZAPPY_NOISE = new SimplexNoise(9001L);

    private static double noise(double x, double y, double z) {
        return ZAPPY_NOISE.value(x * 0.6D, y * 0.6D, z * 0.6D) / 2.0D;
    }

    private static final class SimplexNoise {
        private static final int[][] GRADIENTS = {
            {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
            {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
            {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}};
        private final short[] permutation = new short[512];

        private SimplexNoise(long seed) {
            Random random = new Random(seed);
            int[] source = new int[256];
            for (int i = 0; i < source.length; i++) {
                source[i] = i;
            }
            for (int i = source.length - 1; i > 0; i--) {
                int swap = random.nextInt(i + 1);
                int value = source[i];
                source[i] = source[swap];
                source[swap] = value;
            }
            for (int i = 0; i < permutation.length; i++) {
                permutation[i] = (short) source[i & 255];
            }
        }

        private double value(double x, double y, double z) {
            final double skew = 1.0D / 3.0D;
            final double unskew = 1.0D / 6.0D;
            double s = (x + y + z) * skew;
            int i = fastFloor(x + s);
            int j = fastFloor(y + s);
            int k = fastFloor(z + s);
            double t = (i + j + k) * unskew;
            double x0 = x - (i - t);
            double y0 = y - (j - t);
            double z0 = z - (k - t);

            int i1;
            int j1;
            int k1;
            int i2;
            int j2;
            int k2;
            if (x0 >= y0) {
                if (y0 >= z0) {
                    i1 = 1; j1 = 0; k1 = 0;
                    i2 = 1; j2 = 1; k2 = 0;
                } else if (x0 >= z0) {
                    i1 = 1; j1 = 0; k1 = 0;
                    i2 = 1; j2 = 0; k2 = 1;
                } else {
                    i1 = 0; j1 = 0; k1 = 1;
                    i2 = 1; j2 = 0; k2 = 1;
                }
            } else if (y0 < z0) {
                i1 = 0; j1 = 0; k1 = 1;
                i2 = 0; j2 = 1; k2 = 1;
            } else if (x0 < z0) {
                i1 = 0; j1 = 1; k1 = 0;
                i2 = 0; j2 = 1; k2 = 1;
            } else {
                i1 = 0; j1 = 1; k1 = 0;
                i2 = 1; j2 = 1; k2 = 0;
            }

            double x1 = x0 - i1 + unskew;
            double y1 = y0 - j1 + unskew;
            double z1 = z0 - k1 + unskew;
            double x2 = x0 - i2 + 2.0D * unskew;
            double y2 = y0 - j2 + 2.0D * unskew;
            double z2 = z0 - k2 + 2.0D * unskew;
            double x3 = x0 - 1.0D + 3.0D * unskew;
            double y3 = y0 - 1.0D + 3.0D * unskew;
            double z3 = z0 - 1.0D + 3.0D * unskew;

            return 32.0D * (contribution(i, j, k, x0, y0, z0)
                + contribution(i + i1, j + j1, k + k1, x1, y1, z1)
                + contribution(i + i2, j + j2, k + k2, x2, y2, z2)
                + contribution(i + 1, j + 1, k + 1, x3, y3, z3));
        }

        private double contribution(int x, int y, int z,
                                    double dx, double dy, double dz) {
            double radius = 0.6D - dx * dx - dy * dy - dz * dz;
            if (radius <= 0.0D) {
                return 0.0D;
            }
            int hash = permutation[(x + permutation[(y
                + permutation[z & 255]) & 255]) & 255] % 12;
            int[] gradient = GRADIENTS[hash];
            radius *= radius;
            return radius * radius
                * (gradient[0] * dx + gradient[1] * dy + gradient[2] * dz);
        }

        private static int fastFloor(double value) {
            int floor = (int) value;
            return value < floor ? floor - 1 : floor;
        }
    }

    private static double[] copy(double[] point) {
        return new double[] { point[0], point[1] };
    }

    private static double[] add(double[] a, double[] b) {
        return new double[] { a[0] + b[0], a[1] + b[1] };
    }

    private static double[] subtract(double[] a, double[] b) {
        return new double[] { a[0] - b[0], a[1] - b[1] };
    }

    private static double[] scale(double[] point, double amount) {
        return new double[] { point[0] * amount, point[1] * amount };
    }

    private static void drawLine(List<double[]> points, int color, float width,
                                 double z) {
        if (points == null || points.size() < 2 || width <= 0.0F) {
            return;
        }
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        int alpha = (color >> 24) & 0xFF;
        int pointCount = points.size();
        double half = width * 0.5D;
        double[] joinAngles = new double[pointCount];
        double[] joinOffsets = new double[pointCount];
        for (int i = 2; i < pointCount; i++) {
            double[] p0 = points.get(i - 2);
            double[] p1 = points.get(i - 1);
            double[] p2 = points.get(i);
            double prevX = p1[0] - p0[0];
            double prevY = p1[1] - p0[1];
            double nextX = p2[0] - p1[0];
            double nextY = p2[1] - p1[1];
            double prevLength = Math.sqrt(prevX * prevX + prevY * prevY);
            double nextLength = Math.sqrt(nextX * nextX + nextY * nextY);
            if (prevLength < 0.000001D || nextLength < 0.000001D) {
                continue;
            }
            double angle = Math.atan2(prevX * nextY - prevY * nextX,
                prevX * nextX + prevY * nextY);
            joinAngles[i - 1] = angle;
            double clamp = Math.min(prevLength, nextLength) / half;
            double denominator = 1.0D + Math.cos(angle);
            double offset = denominator < 0.000001D
                ? 0.0D : Math.sin(angle) / denominator;
            joinOffsets[i - 1] = Math.max(-clamp, Math.min(clamp, offset));
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < pointCount - 1; i++) {
            double[] p1 = points.get(i);
            double[] p2 = points.get(i + 1);
            double dx = p2[0] - p1[0];
            double dy = p2[1] - p1[1];
            double segmentLength = Math.sqrt(dx * dx + dy * dy);
            if (segmentLength < 0.000001D) {
                continue;
            }
            double tangentX = dx / segmentLength * half;
            double tangentY = dy / segmentLength * half;
            double normalX = -tangentY;
            double normalY = tangentX;
            double low = joinOffsets[i];
            double high = joinOffsets[i + 1];
            double[] p1Down = new double[] {
                p1[0] + tangentX * Math.max(0.0D, low) + normalX,
                p1[1] + tangentY * Math.max(0.0D, low) + normalY};
            double[] p1Up = new double[] {
                p1[0] + tangentX * Math.max(0.0D, -low) - normalX,
                p1[1] + tangentY * Math.max(0.0D, -low) - normalY};
            double[] p2Down = new double[] {
                p2[0] - tangentX * Math.max(0.0D, high) + normalX,
                p2[1] - tangentY * Math.max(0.0D, high) + normalY};
            double[] p2Up = new double[] {
                p2[0] - tangentX * Math.max(0.0D, -high) - normalX,
                p2[1] - tangentY * Math.max(0.0D, -high) - normalY};

            putColorVertex(buffer, p1Down, z, red, green, blue, alpha);
            putColorVertex(buffer, p1, z, red, green, blue, alpha);
            putColorVertex(buffer, p1Up, z, red, green, blue, alpha);
            putColorVertex(buffer, p1Down, z, red, green, blue, alpha);
            putColorVertex(buffer, p1Up, z, red, green, blue, alpha);
            putColorVertex(buffer, p2Up, z, red, green, blue, alpha);
            putColorVertex(buffer, p1Down, z, red, green, blue, alpha);
            putColorVertex(buffer, p2Up, z, red, green, blue, alpha);
            putColorVertex(buffer, p2, z, red, green, blue, alpha);
            putColorVertex(buffer, p1Down, z, red, green, blue, alpha);
            putColorVertex(buffer, p2, z, red, green, blue, alpha);
            putColorVertex(buffer, p2Down, z, red, green, blue, alpha);

            if (i > 0) {
                double signedAngle = joinAngles[i];
                double angle = Math.abs(signedAngle);
                int joinSteps = Math.max(1, (int) Math.ceil(angle
                    * 180.0D / (18.0D * Math.PI)));
                if (angle > 0.000001D) {
                    double rnormalX = -normalX;
                    double rnormalY = -normalY;
                    if (signedAngle < 0.0D) {
                        double previousX = p1[0] - rnormalX;
                        double previousY = p1[1] - rnormalY;
                        for (int j = 1; j <= joinSteps; j++) {
                            double[] fan = rotate(rnormalX, rnormalY,
                                -signedAngle * j / joinSteps);
                            double fanX = p1[0] - fan[0];
                            double fanY = p1[1] - fan[1];
                            putColorVertex(buffer, p1, z, red, green, blue, alpha);
                            putColorVertex(buffer,
                                new double[] {previousX, previousY}, z,
                                red, green, blue, alpha);
                            putColorVertex(buffer,
                                new double[] {fanX, fanY}, z,
                                red, green, blue, alpha);
                            previousX = fanX;
                            previousY = fanY;
                        }
                    } else {
                        double[] startFan = rotate(normalX, normalY, -signedAngle);
                        double previousX = p1[0] - startFan[0];
                        double previousY = p1[1] - startFan[1];
                        for (int j = joinSteps - 1; j >= 0; j--) {
                            double[] fan = rotate(normalX, normalY,
                                -signedAngle * j / joinSteps);
                            double fanX = p1[0] - fan[0];
                            double fanY = p1[1] - fan[1];
                            putColorVertex(buffer, p1, z, red, green, blue, alpha);
                            putColorVertex(buffer,
                                new double[] {previousX, previousY}, z,
                                red, green, blue, alpha);
                            putColorVertex(buffer,
                                new double[] {fanX, fanY}, z,
                                red, green, blue, alpha);
                            previousX = fanX;
                            previousY = fanY;
                        }
                    }
                }
            }
        }
        tessellator.draw();
        drawCapFan(points.get(0), points.get(1), half, z,
            red, green, blue, alpha);
        drawCapFan(points.get(pointCount - 1), points.get(pointCount - 2),
            half, z, red, green, blue, alpha);
    }

    private static void putColorVertex(BufferBuilder buffer, double[] point,
                                       double z, int red, int green, int blue,
                                       int alpha) {
        buffer.pos(point[0], point[1], z)
            .color(red, green, blue, alpha).endVertex();
    }

    private static double[] rotate(double x, double y, double theta) {
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);
        return new double[] {x * cos - y * sin, y * cos + x * sin};
    }

    private static void drawCapFan(double[] point, double[] previous,
                                   double radius, double z, int red,
                                   int green, int blue, int alpha) {
        double dx = point[0] - previous[0];
        double dy = point[1] - previous[1];
        double segmentLength = Math.sqrt(dx * dx + dy * dy);
        if (segmentLength < 0.000001D) {
            return;
        }
        double tangentX = dx / segmentLength * radius;
        double tangentY = dy / segmentLength * radius;
        double normalX = -tangentY;
        double normalY = tangentX;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        putColorVertex(buffer, point, z, red, green, blue, alpha);
        for (int i = 10; i >= 0; i--) {
            double[] fan = rotate(normalX, normalY, -Math.PI * i / 10.0D);
            putColorVertex(buffer,
                new double[] {point[0] + fan[0], point[1] + fan[1]}, z,
                red, green, blue, alpha);
        }
        tessellator.draw();
    }

    private static int screenColor(int color) {
        int alpha = (color >> 24) & 0xFF;
        int red = ((color >> 16) & 0xFF) + 255;
        int green = ((color >> 8) & 0xFF) + 255;
        int blue = (color & 0xFF) + 255;
        return (alpha << 24)
            | ((red / 2) << 16)
            | ((green / 2) << 8)
            | (blue / 2);
    }

    private static double[] xy(HexCoord coord) {
        return new double[] {
            (SQRT_3 * coord.getQ() + SQRT_3 * 0.5D * coord.getR()),
            -1.5D * coord.getR()
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
