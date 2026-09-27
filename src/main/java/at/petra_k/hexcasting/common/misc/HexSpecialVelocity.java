package at.petra_k.hexcasting.common.misc;

import at.petra_k.hexcasting.api.HexAPI;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntitySpectralArrow;
import net.minecraft.util.math.Vec3d;

/** Vanilla-specific velocity corrections used by Hex's velocity query. */
public final class HexSpecialVelocity {
    private HexSpecialVelocity() {
    }

    public static void register() {
        HexAPI.registerSpecialVelocityGetter(EntityPlayer.class,
            PlayerPositionRecorder::getMotion);
        HexAPI.registerSpecialVelocityGetter(EntityArrow.class,
            HexSpecialVelocity::arrowVelocity);
        HexAPI.registerSpecialVelocityGetter(EntitySpectralArrow.class,
            HexSpecialVelocity::arrowVelocity);
        HexAPI.registerSpecialVelocityGetter(EntityItem.class,
            HexSpecialVelocity::itemVelocity);
    }

    private static Vec3d arrowVelocity(EntityArrow arrow) {
        if (arrow == null || isInGround(arrow)) {
            return Vec3d.ZERO;
        }
        return new Vec3d(arrow.motionX, arrow.motionY, arrow.motionZ);
    }

    private static Vec3d itemVelocity(EntityItem item) {
        if (item == null || (item.onGround
            && item.motionX * item.motionX + item.motionZ * item.motionZ <= 1.0E-5D)) {
            return Vec3d.ZERO;
        }
        return new Vec3d(item.motionX, item.motionY, item.motionZ);
    }

    private static boolean isInGround(EntityArrow arrow) {
        try {
            return ((at.petra_k.hexcasting.mixin.AccessorEntityArrow) (Object) arrow)
                .hexcasting$isInGround();
        } catch (ClassCastException ignored) {
            return false;
        }
    }
}
