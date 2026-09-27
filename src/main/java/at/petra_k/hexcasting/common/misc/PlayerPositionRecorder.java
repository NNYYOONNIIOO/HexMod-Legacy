package at.petra_k.hexcasting.common.misc;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Map;
import java.util.WeakHashMap;

/** Records server-side player displacement for the velocity query action. */
public final class PlayerPositionRecorder {
    private static final Map<EntityPlayer, Vec3d> LAST_POSITION = new WeakHashMap<>();
    private static final Map<EntityPlayer, Vec3d> PREVIOUS_POSITION = new WeakHashMap<>();

    private PlayerPositionRecorder() {
    }

    /** Capture all players after a server world tick has advanced. */
    public static synchronized void updateAllPlayers(World world) {
        if (world == null || world.isRemote) {
            return;
        }
        for (EntityPlayer player : world.playerEntities) {
            Vec3d previous = LAST_POSITION.get(player);
            if (previous != null) {
                PREVIOUS_POSITION.put(player, previous);
            }
            LAST_POSITION.put(player, new Vec3d(player.posX, player.posY, player.posZ));
        }
    }

    /** Return the displacement measured during the latest complete tick. */
    public static synchronized Vec3d getMotion(EntityPlayer player) {
        if (player == null) {
            return Vec3d.ZERO;
        }
        Vec3d current = LAST_POSITION.get(player);
        Vec3d previous = PREVIOUS_POSITION.get(player);
        if (current == null) {
            return Vec3d.ZERO;
        }
        if (previous == null) {
            return new Vec3d(player.posX, player.posY, player.posZ)
                .subtract(current);
        }
        return current.subtract(previous);
    }
}
