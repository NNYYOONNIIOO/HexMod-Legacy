package at.petra_k.hexcasting.common.effect;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.casting.StaffCastExecutor;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import at.petra_k.hexcasting.common.network.MsgCastParticlesS2C;
import at.petra_k.hexcasting.common.network.MsgCastingPatternS2C;
import at.petra_k.hexcasting.common.network.MsgClearCastingPatternsS2C;
import at.petrak.paucal.api.PaucalAPI;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.init.SoundEvents;

import java.util.Collections;
import java.util.List;

/** Server-side bridge for the visual and audio feedback of Hex casting. */
public final class HexCastingEffects {
    private static final int DEFAULT_PIGMENT = 0xAA66FF;
    private static final int ERROR_COLOR = 0xE05252;
    private static final int ORBIT_LIFETIME = Integer.MAX_VALUE;

    private HexCastingEffects() {
    }

    /** Send feedback for a single pattern appended through the staff GUI. */
    public static void onStaffPattern(EntityPlayer player, HexPattern pattern,
                                      StaffCastExecutor.CastOutcome outcome) {
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }
        boolean success = outcome != null && outcome.isSuccess();
        int color = success ? pigment(player) : ERROR_COLOR;
        if (pattern != null) {
            sendOrbitPattern(player, pattern, success ? ORBIT_LIFETIME : 36, color);
        }

        if (success) {
            // Hex sprays a small upward fan for every accepted staff pattern.
            sendSpray(player, player.posX, player.posY + 0.8D, player.posZ,
                0.0D, 1.5D, 0.0D, 0.4D, Math.PI / 3.0D, 30, color);
            playSound(player, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 0.62F, 1.15F);
            if (outcome.isStackClear()) {
                // The final pattern is a real spell completion, not merely a
                // parenthesized/escaped step. Give both caster and target a
                // stronger burst, then fade the orbiting source patterns.
                sendTargetFeedback(player, color);
                sendSpray(player, player.posX, player.posY + 1.0D, player.posZ,
                    1.0D, 0.0D, 0.0D, 0.0D, Math.PI, 42, color);
                playSound(player, SoundEvents.ENTITY_PLAYER_LEVELUP, 0.75F, 1.35F);
                clearOrbitPatterns(player);
            }
        } else {
            sendSpray(player, player.posX, player.posY + 0.8D, player.posZ,
                0.0D, 1.0D, 0.0D, 0.55D, Math.PI * 0.85D, 26, color);
            playSound(player, SoundEvents.BLOCK_NOTE_BASS, 0.8F, 0.55F);
        }
    }

    /** Feedback for a completed cast launched from a scroll, focus or packaged item. */
    public static void onPortableCast(EntityPlayer player, List<HexPattern> patterns,
                                      boolean success) {
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }
        int color = success ? pigment(player) : ERROR_COLOR;
        List<HexPattern> safePatterns = patterns == null
            ? Collections.<HexPattern>emptyList() : patterns;
        for (HexPattern pattern : safePatterns) {
            if (pattern != null) {
                // PackagedItemCastEnv in modern Hex keeps these visible for
                // 140 ticks instead of the staff's open-ended spiral.
                sendOrbitPattern(player, pattern, success ? 140 : 36, color);
            }
        }
        if (success) {
            sendSpray(player, player.posX, player.posY + 0.8D, player.posZ,
                0.0D, 1.5D, 0.0D, 0.4D, Math.PI / 3.0D, 30, color);
            sendTargetFeedback(player, color);
            playSound(player, SoundEvents.ENTITY_PLAYER_LEVELUP, 0.72F, 1.25F);
        } else {
            sendSpray(player, player.posX, player.posY + 0.8D, player.posZ,
                0.0D, 1.0D, 0.0D, 0.55D, Math.PI * 0.85D, 26, color);
            playSound(player, SoundEvents.BLOCK_NOTE_BASS, 0.8F, 0.55F);
        }
    }

    public static void clearOrbitPatterns(EntityPlayer player) {
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }
        PaucalAPI.sendPacketNearS2C(player.getPositionVector(), 128.0D, player.world,
            new MsgClearCastingPatternsS2C(player.getUniqueID()));
    }

    private static void sendOrbitPattern(EntityPlayer player, HexPattern pattern,
                                         int lifetime, int color) {
        PaucalAPI.sendPacketNearS2C(player.getPositionVector(), 128.0D, player.world,
            new MsgCastingPatternS2C(player.getUniqueID(), pattern, lifetime, color));
    }

    private static void sendSpray(EntityPlayer player, double posX, double posY,
                                  double posZ, double velX, double velY, double velZ,
                                  double fuzziness, double spread, int count, int color) {
        PaucalAPI.sendPacketNearS2C(new Vec3d(posX, posY, posZ), 128.0D, player.world,
            new MsgCastParticlesS2C(posX, posY, posZ, velX, velY, velZ,
                fuzziness, spread, count, color));
    }

    /** Feedback at the looked-at block/entity and at an off-hand data holder. */
    private static void sendTargetFeedback(EntityPlayer player, int color) {
        Vec3d target = findLookTarget(player);
        sendSpray(player, target.x, target.y, target.z,
            0.0D, 0.8D, 0.0D, 0.45D, Math.PI, 34, color);

        ItemStack offhand = player.getHeldItemOffhand();
        if (offhand != null && !offhand.isEmpty()) {
            Vec3d hand = player.getPositionEyes(1.0F)
                .add(player.getLookVec().scale(0.75D))
                .addVector(0.0D, -0.35D, 0.0D);
            sendSpray(player, hand.x, hand.y, hand.z,
                0.0D, 0.35D, 0.0D, 0.18D, Math.PI, 14, color);
        }
    }

    private static Vec3d findLookTarget(EntityPlayer player) {
        Vec3d start = player.getPositionEyes(1.0F);
        Vec3d look = player.getLookVec().normalize();
        Vec3d end = start.add(look.scale(32.0D));
        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;
        List<Entity> candidates = player.world.getEntitiesWithinAABBExcludingEntity(
            player, player.getEntityBoundingBox().grow(32.0D));
        for (Entity entity : candidates) {
            if (entity == null || entity.isDead || !entity.canBeCollidedWith()) {
                continue;
            }
            AxisAlignedBB bounds = entity.getEntityBoundingBox()
                .grow(entity.getCollisionBorderSize() + 0.25D);
            if (bounds.calculateIntercept(start, end) != null) {
                double distance = start.distanceTo(entity.getPositionVector());
                if (distance < closestDistance) {
                    closest = entity;
                    closestDistance = distance;
                }
            }
        }
        if (closest != null) {
            return new Vec3d(closest.posX,
                closest.posY + Math.max(0.25D, closest.height * 0.5D), closest.posZ);
        }

        RayTraceResult block = player.rayTrace(32.0D, 1.0F);
        if (block != null && block.typeOfHit == RayTraceResult.Type.BLOCK
            && block.hitVec != null) {
            return block.hitVec;
        }
        return start.add(look.scale(1.25D));
    }

    private static int pigment(EntityPlayer player) {
        IHexCastingData data = player.getCapability(HexCapabilities.CASTING_DATA, null);
        return data == null ? DEFAULT_PIGMENT : data.getPigment();
    }

    private static void playSound(EntityPlayer player, net.minecraft.util.SoundEvent sound,
                                  float volume, float pitch) {
        player.world.playSound(null, player.posX, player.posY, player.posZ, sound,
            SoundCategory.PLAYERS, volume, pitch);
    }
}
