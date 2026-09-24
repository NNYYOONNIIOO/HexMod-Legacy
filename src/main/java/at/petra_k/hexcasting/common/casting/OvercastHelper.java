package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.misc.MediaConstants;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.ResourceLocation;

/** Player-health fallback used when a player cast runs out of media. */
public final class OvercastHelper {
    private static final String UNLOCKED_KEY = "hexcasting:overcast_unlocked";
    private static final ResourceLocation UNLOCK_ADVANCEMENT =
        new ResourceLocation("hexcasting", "y_u_no_cast_angy");
    private static final double MEDIA_TO_HEALTH =
        2.0D * (double) MediaConstants.CRYSTAL_UNIT / 20.0D;

    private OvercastHelper() {
    }

    /** Whether the player can currently turn health into media. */
    public static boolean canOvercast(EntityPlayer player) {
        if (player == null || player.world == null || player.world.isRemote
            || player.isSpectator()) {
            return false;
        }
        if (player.getEntityData().getBoolean(UNLOCKED_KEY)) {
            return true;
        }
        if (!(player instanceof net.minecraft.entity.player.EntityPlayerMP)
            || player.world.getMinecraftServer() == null) {
            return false;
        }
        net.minecraft.advancements.Advancement advancement =
            player.world.getMinecraftServer().getAdvancementManager()
                .getAdvancement(UNLOCK_ADVANCEMENT);
        return advancement != null
            && ((net.minecraft.entity.player.EntityPlayerMP) player)
                .getAdvancements().getProgress(advancement).isDone();
    }

    /** Persist the unlock earned by failing a world-specific great spell. */
    public static void unlock(EntityPlayer player) {
        if (player != null) {
            player.getEntityData().setBoolean(UNLOCKED_KEY, true);
        }
    }

    /** Maximum media the player's current health can provide. */
    public static long availableMedia(EntityPlayer player) {
        if (!canOvercast(player) || player.getHealth() <= 0.0F) {
            return 0L;
        }
        DamageSource source = damageSource(player);
        if (isInvulnerable(player, source)) {
            return 0L;
        }
        return ceilToLong(player.getHealth() * MEDIA_TO_HEALTH);
    }

    /** Spend health for up to {@code requested} missing media. */
    public static long consume(EntityPlayer player, long requested) {
        if (!canOvercast(player) || requested <= 0L
            || player.getHealth() <= 0.0F) {
            return 0L;
        }

        double healthToRemove = Math.max(
            requested / MEDIA_TO_HEALTH, 0.5D);
        double mediaBefore = player.getHealth() * MEDIA_TO_HEALTH;
        DamageSource source = damageSource(player);
        trulyHurt(player, source, (float) healthToRemove);
        double mediaAfter = Math.max(0.0D, player.getHealth() * MEDIA_TO_HEALTH);
        return ceilToLong(Math.max(0.0D, mediaBefore - mediaAfter));
    }

    private static DamageSource damageSource(EntityPlayer player) {
        return new EntityDamageSource("hexcasting.overcast", player)
            .setDamageBypassesArmor().setDamageIsAbsolute().setMagicDamage();
    }

    /** 1.12.2 equivalent of Mishap.trulyHurt. */
    private static void trulyHurt(EntityLivingBase entity, DamageSource source,
                                  float amount) {
        if (entity == null || entity.world == null || entity.world.isRemote
            || entity.isDead || amount <= 0.0F) {
            return;
        }
        entity.hurtResistantTime = 0;
        if (!entity.attackEntityFrom(source, amount)
            && !isInvulnerable(entity, source) && !entity.isDead) {
            entity.setHealth(entity.getHealth() - amount);
            if (entity.getHealth() <= 0.0F) {
                entity.setDead();
            }
        }
    }

    private static boolean isInvulnerable(EntityLivingBase entity,
                                           DamageSource source) {
        return entity.isEntityInvulnerable(source);
    }

    private static long ceilToLong(double value) {
        if (!(value > 0.0D)) {
            return 0L;
        }
        return value >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) Math.ceil(value);
    }
}
