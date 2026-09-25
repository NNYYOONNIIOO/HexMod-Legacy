package at.petra_k.hexcasting.api;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityShulkerBullet;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

/** Shared public constants for the 1.12.2 Hex Casting port. */
public final class HexAPI {
    public static final String MOD_ID = "hexcasting";
    public static final String MOD_NAME = "Hex Casting";
    public static final String MOD_VERSION = "0.1.0-1.12.2";
    /** Casting-image userdata key used by actions with per-cast bookkeeping. */
    public static final String MARKED_MOVED_USERDATA = "hexcasting:marked_moved";

    private HexAPI() {
    }

    public static ResourceLocation modLoc(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    /**
     * Return the direction exposed by Hex's entity-look query.
     *
     * <p>Several 1.12 projectile implementations store their model direction
     * opposite to the direction returned by {@code getLookVec()}.  Modern Hex
     * keeps these corrections in its platform API so every action sees the
     * same direction.  Keeping the compatibility rule here also avoids the
     * blink action and the query action drifting apart.</p>
     */
    public static Vec3d getEntityLookDirSpecial(Entity entity) {
        if (entity == null) {
            return Vec3d.ZERO;
        }
        Vec3d look = entity.getLookVec();
        if (entity instanceof EntityFireball
            || entity instanceof EntityShulkerBullet) {
            return new Vec3d(-look.x, look.y, -look.z);
        }
        if (entity instanceof EntityArrow || entity instanceof EntityThrowable) {
            return new Vec3d(-look.x, -look.y, look.z);
        }
        // Phantom's 1.12 model and hit direction use the inverse vertical
        // component, matching MC-134707 and the modern Hex implementation.
        ResourceLocation id = net.minecraft.entity.EntityList.getKey(entity);
        if (id != null && "minecraft".equals(id.getResourceDomain())
            && "phantom".equals(id.getResourcePath())) {
            return new Vec3d(look.x, -look.y, look.z);
        }
        return look;
    }
}
