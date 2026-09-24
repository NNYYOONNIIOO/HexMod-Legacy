package at.petra_k.hexcasting.common.effect;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.api.casting.eval.Mishap;
import at.petra_k.hexcasting.common.casting.StaffCastExecutor;
import at.petra_k.hexcasting.common.item.ItemHexStaff;
import at.petra_k.hexcasting.common.lib.HexSounds;
import at.petra_k.hexcasting.common.network.MsgCastParticlesS2C;
import at.petra_k.hexcasting.common.network.MsgCastingPatternS2C;
import at.petra_k.hexcasting.common.network.MsgClearCastingPatternsS2C;
import at.petrak.paucal.api.PaucalAPI;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.util.Collections;
import java.util.List;

/** Server-side bridge for the visual and audio feedback of Hex casting. */
public final class HexCastingEffects {
    private static final int ERROR_COLOR = 0xE05252;
    private static final int ORBIT_LIFETIME = Integer.MAX_VALUE;

    private HexCastingEffects() {
    }

    /** Send feedback for a single pattern appended through the staff GUI. */
    public static void onStaffPattern(EntityPlayer player, HexPattern pattern,
                                      StaffCastExecutor.CastOutcome outcome) {
        onStaffPattern(player, EnumHand.MAIN_HAND, pattern, outcome);
    }

    /** Send staff feedback using the hand containing the colourized staff. */
    public static void onStaffPattern(EntityPlayer player, EnumHand hand,
                                      HexPattern pattern,
                                      StaffCastExecutor.CastOutcome outcome) {
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }
        boolean success = outcome != null && outcome.isSuccess();
        HexPigmentSource source = playerPigment(player);
        Mishap mishap = outcome == null ? null : outcome.getMishap();
        int color = success ? sample(source, player)
            : mishap == null ? ERROR_COLOR : mishap.getAccentColor();
        if (pattern != null) {
            sendOrbitPattern(player, pattern, success ? ORBIT_LIFETIME : 36,
                color, source, true);
        }

        if (success) {
            // Hex sprays a small upward fan for every accepted staff pattern.
            sendSpray(player, player.posX, player.posY + 0.8D, player.posZ,
                0.0D, 1.5D, 0.0D, 0.4D, Math.PI / 3.0D, 30, color, source);
            playSound(player, outcome == null || outcome.getSound() == null
                ? HexSounds.CAST_NORMAL : outcome.getSound(), 1.0F, 1.0F);
            if (outcome.isStackClear()) {
                // The final pattern is a real spell completion, not merely a
                // parenthesized/escaped step. Give both caster and target a
                // stronger burst, then fade the orbiting source patterns.
                sendTargetFeedback(player, color, source);
                sendSpray(player, player.posX, player.posY + 1.0D, player.posZ,
                    1.0D, 0.0D, 0.0D, 0.0D, Math.PI, 42, color, source);
                clearOrbitPatterns(player);
            }
        } else {
            sendSpray(player, player.posX, player.posY + 0.8D, player.posZ,
                0.0D, 1.0D, 0.0D, 0.55D, Math.PI * 0.85D, 26, color, null);
            if (color != ERROR_COLOR) {
                sendSpray(player, player.posX, player.posY + 0.8D, player.posZ,
                    0.0D, 1.0D, 0.0D, 0.55D, Math.PI * 0.85D, 26,
                    ERROR_COLOR, null);
            }
            playSound(player, HexSounds.CAST_FAILURE, 1.0F, 1.0F);
        }
    }

    /** Feedback for a completed cast launched from a scroll, focus or packaged item. */
    public static void onPortableCast(EntityPlayer player, List<HexPattern> patterns,
                                      boolean success) {
        onPortableCast(player, EnumHand.MAIN_HAND, patterns, success, null);
    }

    /** Emit Altiora's small downward media trail while its grace is active. */
    public static void onAltioraTick(EntityPlayer player) {
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }
        HexPigmentSource source = playerPigment(player);
        int color = sample(source, player);
        sendSpray(player, player.posX, player.posY, player.posZ,
            0.0D, -0.2D, 0.0D, 0.4D, Math.PI * 0.5D, 3, color, source);
    }

    /** Emit the danger-weighted trail used by range/time-limited flight. */
    public static void onFlightTick(EntityPlayer player, double danger) {
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }
        HexPigmentSource source = playerPigment(player);
        int color = sample(source, player);
        int dangerCount = (int) Math.ceil(5.0D * Math.max(0.0D,
            Math.min(1.0D, danger)));
        int normalCount = Math.max(0, 5 - dangerCount);
        if (normalCount > 0) {
            sendSpray(player, player.posX, player.posY, player.posZ,
                0.0D, -0.6D, 0.0D, 0.6D, Math.PI * 0.3D,
                normalCount, color, source);
        }
        if (dangerCount > 0) {
            sendSpray(player, player.posX, player.posY, player.posZ,
                0.0D, 0.8D, 0.0D, 0.3D, Math.PI * 0.75D,
                dangerCount, 0x202020, null);
            sendSpray(player, player.posX, player.posY, player.posZ,
                0.0D, 0.8D, 0.0D, 0.3D, Math.PI * 0.75D,
                dangerCount, 0xE05252, null);
        }
    }

    /** Emit the end burst when ordinary Hex flight becomes unstable. */
    public static void onFlightFinish(EntityPlayer player) {
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }
        HexPigmentSource source = playerPigment(player);
        int color = sample(source, player);
        sendSpray(player, player.posX, player.posY, player.posZ,
            0.0D, 1.0D, 0.0D, 3.141592653589793D, 0.4D,
            20, color, source);
        sendSpray(player, player.posX, player.posY, player.posZ,
            0.0D, 1.0D, 0.0D, 3.141592653589793D, 0.4D,
            20, 0x202020, null);
    }

    /** Feedback for a portable cast using the hand containing its source item. */
    public static void onPortableCast(EntityPlayer player, EnumHand hand,
                                      List<HexPattern> patterns, boolean success) {
        onPortableCast(player, hand, patterns, success, null);
    }

    /** Feedback with the sound selected by the casting VM. */
    public static void onPortableCast(EntityPlayer player, EnumHand hand,
                                      List<HexPattern> patterns, boolean success,
                                      SoundEvent castSound) {
        onPortableCast(player, hand, patterns, success, castSound, null);
    }

    /** Feedback with a structured Mishap accent for failed portable casts. */
    public static void onPortableCast(EntityPlayer player, EnumHand hand,
                                      List<HexPattern> patterns, boolean success,
                                      SoundEvent castSound, Mishap mishap) {
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }
        HexPigmentSource source = HexPigmentSource.resolve(player, hand);
        int color = success ? sample(source, player)
            : mishap == null ? ERROR_COLOR : mishap.getAccentColor();
        List<HexPattern> safePatterns = patterns == null
            ? Collections.<HexPattern>emptyList() : patterns;
        for (HexPattern pattern : safePatterns) {
            if (pattern != null) {
                // PackagedItemCastEnv in modern Hex keeps these visible for
                // 140 ticks instead of the staff's open-ended spiral.
                sendOrbitPattern(player, pattern, success ? 140 : 36,
                    color, source, false);
            }
        }
        if (success) {
            sendSpray(player, player.posX, player.posY + 0.8D, player.posZ,
                0.0D, 1.5D, 0.0D, 0.4D, Math.PI / 3.0D, 30, color, source);
            sendTargetFeedback(player, color, source);
            playSound(player, castSound == null ? HexSounds.CAST_NORMAL : castSound,
                1.0F, 1.0F);
        } else {
            sendSpray(player, player.posX, player.posY + 0.8D, player.posZ,
                0.0D, 1.0D, 0.0D, 0.55D, Math.PI * 0.85D, 26, color, null);
            if (color != ERROR_COLOR) {
                sendSpray(player, player.posX, player.posY + 0.8D, player.posZ,
                    0.0D, 1.0D, 0.0D, 0.55D, Math.PI * 0.85D, 26,
                    ERROR_COLOR, null);
            }
            playSound(player, HexSounds.CAST_FAILURE, 1.0F, 1.0F);
        }
    }

    public static void clearOrbitPatterns(EntityPlayer player) {
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }
        PaucalAPI.sendPacketNearS2C(player.getPositionVector(), 128.0D, player.world,
            new MsgClearCastingPatternsS2C(player.getUniqueID()));
    }

    /**
     * Rebuild the client-side spiral after login, respawn, or a dimension
     * change.  The original cast packets are transient, while an unfinished
     * staff program is persisted in the staff NBT, so the latter is the
     * authoritative source for this resynchronization.
     */
    public static void syncOrbitPatterns(EntityPlayer player) {
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }
        // A respawn or dimension change can leave the client-side cache with
        // patterns from the previous world.  Send the clear first so the
        // following authoritative entries are a complete replacement rather
        // than an append to stale visual state.
        clearOrbitPatterns(player);
        // The visual stack belongs to the caster, not to the currently
        // selected slot.  Scan the whole player inventory so the ring can be
        // reconstructed even when the staff was moved to another hotbar slot
        // or the off-hand before rejoining the world.
        for (ItemStack staff : player.inventory.mainInventory) {
            if (!ItemHexStaff.isStaff(staff)) {
                continue;
            }
            for (ItemHexStaff.ProgramEntry entry : ItemHexStaff.getProgramEntries(staff)) {
                if (entry == null || entry.getPattern() == null) {
                    continue;
                }
                StaffCastExecutor.Resolution[] resolutions =
                    StaffCastExecutor.Resolution.values();
                int ordinal = entry.getResolutionOrdinal();
                StaffCastExecutor.Resolution resolution = ordinal >= 0
                    && ordinal < resolutions.length
                    ? resolutions[ordinal] : StaffCastExecutor.Resolution.UNRESOLVED;
                HexPigmentSource source = playerPigment(player);
                int color = resolution == StaffCastExecutor.Resolution.ERRORED
                    || resolution == StaffCastExecutor.Resolution.INVALID
                    ? ERROR_COLOR : sample(source, player);
                int lifetime = resolution == StaffCastExecutor.Resolution.ERRORED
                    || resolution == StaffCastExecutor.Resolution.INVALID
                    ? 36 : ORBIT_LIFETIME;
                sendOrbitPattern(player, entry.getPattern(), lifetime, color,
                    source, true);
            }
        }
        ItemStack offhand = player.getHeldItemOffhand();
        if (ItemHexStaff.isStaff(offhand)) {
            for (ItemHexStaff.ProgramEntry entry : ItemHexStaff.getProgramEntries(offhand)) {
                if (entry == null || entry.getPattern() == null) {
                    continue;
                }
                StaffCastExecutor.Resolution[] resolutions =
                    StaffCastExecutor.Resolution.values();
                int ordinal = entry.getResolutionOrdinal();
                StaffCastExecutor.Resolution resolution = ordinal >= 0
                    && ordinal < resolutions.length
                    ? resolutions[ordinal] : StaffCastExecutor.Resolution.UNRESOLVED;
                HexPigmentSource source = playerPigment(player);
                int color = resolution == StaffCastExecutor.Resolution.ERRORED
                    || resolution == StaffCastExecutor.Resolution.INVALID
                    ? ERROR_COLOR : sample(source, player);
                int lifetime = resolution == StaffCastExecutor.Resolution.ERRORED
                    || resolution == StaffCastExecutor.Resolution.INVALID
                    ? 36 : ORBIT_LIFETIME;
                sendOrbitPattern(player, entry.getPattern(), lifetime, color,
                    source, true);
            }
        }
    }

    private static void sendOrbitPattern(EntityPlayer player, HexPattern pattern,
                                         int lifetime, int color,
                                         HexPigmentSource source,
                                         boolean usePlayerPigment) {
        PaucalAPI.sendPacketNearS2C(player.getPositionVector(), 128.0D, player.world,
            new MsgCastingPatternS2C(player.getUniqueID(), pattern, lifetime, color,
                source, usePlayerPigment));
    }

    /** Staff orbits always use the caster's internalized pigment. */
    private static HexPigmentSource playerPigment(EntityPlayer player) {
        HexPigmentSource source = HexPigmentSource.resolvePlayer(player);
        return source == null ? HexPigmentSource.defaultSource() : source;
    }

    private static void sendSpray(EntityPlayer player, double posX, double posY,
                                  double posZ, double velX, double velY, double velZ,
                                  double fuzziness, double spread, int count, int color,
                                  HexPigmentSource source) {
        PaucalAPI.sendPacketNearS2C(new Vec3d(posX, posY, posZ), 128.0D, player.world,
            new MsgCastParticlesS2C(posX, posY, posZ, velX, velY, velZ,
                fuzziness, spread, count, color, source));
    }

    /** Feedback at the looked-at block/entity and at an off-hand data holder. */
    private static void sendTargetFeedback(EntityPlayer player, int color,
                                           HexPigmentSource source) {
        Vec3d target = findLookTarget(player);
        sendSpray(player, target.x, target.y, target.z,
            0.0D, 0.8D, 0.0D, 0.45D, Math.PI, 34, color, source);

        ItemStack offhand = player.getHeldItemOffhand();
        if (offhand != null && !offhand.isEmpty()) {
            Vec3d hand = player.getPositionEyes(1.0F)
                .add(player.getLookVec().scale(0.75D))
                .addVector(0.0D, -0.35D, 0.0D);
            sendSpray(player, hand.x, hand.y, hand.z,
                0.0D, 0.35D, 0.0D, 0.18D, Math.PI, 14, color, source);
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

    private static int sample(HexPigmentSource source, EntityPlayer player) {
        HexPigmentSource safe = source == null
            ? HexPigmentSource.defaultSource() : source;
        float time = player == null || player.world == null
            ? 0.0F : (float) player.world.getTotalWorldTime();
        double x = player == null ? 0.0D : player.posX;
        double y = player == null ? 0.0D : player.posY;
        double z = player == null ? 0.0D : player.posZ;
        return safe.sample(time, x, y, z);
    }

    private static void playSound(EntityPlayer player, net.minecraft.util.SoundEvent sound,
                                  float volume, float pitch) {
        player.world.playSound(null, player.posX, player.posY, player.posZ, sound,
            SoundCategory.PLAYERS, volume, pitch);
    }
}
