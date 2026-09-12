package at.petra_k.hexcasting.common.lib.hex;

import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.NullIota;
import at.petra_k.hexcasting.api.casting.iota.Vec3Iota;
import at.petra_k.hexcasting.api.misc.MediaConstants;
import at.petra_k.hexcasting.common.world.SentinelData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.api.casting.math.HexDir;
import at.petra_k.hexcasting.api.casting.math.HexAngle;

/** 1.12.2 implementations of Hex Casting's sentinel actions. */
public final class SentinelActions {
    private static final long NEGLIGIBLE_MEDIA = MediaConstants.DUST_UNIT / 10L;
    private static boolean registered;

    private SentinelActions() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        HexActionRegistry.register(
            new ResourceLocation("hexcasting", "sentinel/create"),
            pattern(HexDir.EAST, "waeawae"), createAction(false));
        HexActionRegistry.register(
            new ResourceLocation("hexcasting", "sentinel/create/great"),
            pattern(HexDir.EAST, "waeawaeqqqwqwqqwq"), createAction(true));
        HexActionRegistry.register(
            new ResourceLocation("hexcasting", "sentinel/destroy"),
            pattern(HexDir.NORTH_EAST, "qdwdqdw"), destroyAction());
        HexActionRegistry.register(
            new ResourceLocation("hexcasting", "sentinel/get_pos"),
            pattern(HexDir.EAST, "waeawaede"), getPositionAction());
        HexActionRegistry.register(
            new ResourceLocation("hexcasting", "sentinel/wayfind"),
            pattern(HexDir.EAST, "waeawaedwa"), wayfindAction());
        registered = true;
    }

    private static HexAction createAction(final boolean extendedRange) {
        return new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw new CastingException("hexcasting.error.sentinel_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                EntityPlayer player = requirePlayer(vm);
                Vec3d target = vectorOf(stack.pop(Vec3Iota.class));
                assertTargetInRange(player, target, extendedRange);
                vm.consumeMedia(MediaConstants.DUST_UNIT * (extendedRange ? 2L : 1L));
                SentinelData.get(player.world).set(
                    player.getUniqueID(), extendedRange, target.x, target.y, target.z,
                    player.world.provider.getDimension());
            }
        };
    }

    private static HexAction destroyAction() {
        return new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw new CastingException("hexcasting.error.sentinel_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                EntityPlayer player = requirePlayer(vm);
                vm.consumeMedia(NEGLIGIBLE_MEDIA);
                SentinelData.State state = SentinelData.get(player.world).get(player.getUniqueID());
                if (state != null && state.dimension != player.world.provider.getDimension()) {
                    throw new CastingException("hexcasting.error.sentinel_wrong_dimension");
                }
                SentinelData.get(player.world).clear(player.getUniqueID());
            }
        };
    }

    private static HexAction getPositionAction() {
        return new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw new CastingException("hexcasting.error.sentinel_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                EntityPlayer player = requirePlayer(vm);
                vm.consumeMedia(NEGLIGIBLE_MEDIA);
                SentinelData.State state = SentinelData.get(player.world).get(player.getUniqueID());
                if (state == null) {
                    stack.push(new NullIota());
                    return;
                }
                if (state.dimension != player.world.provider.getDimension()) {
                    throw new CastingException("hexcasting.error.sentinel_wrong_dimension");
                }
                stack.push(vectorIota(new Vec3d(state.x, state.y, state.z)));
            }
        };
    }

    private static HexAction wayfindAction() {
        return new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw new CastingException("hexcasting.error.sentinel_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                EntityPlayer player = requirePlayer(vm);
                Vec3d from = vectorOf(stack.pop(Vec3Iota.class));
                vm.consumeMedia(NEGLIGIBLE_MEDIA);
                SentinelData.State state = SentinelData.get(player.world).get(player.getUniqueID());
                if (state == null) {
                    stack.push(new NullIota());
                    return;
                }
                if (state.dimension != player.world.provider.getDimension()) {
                    throw new CastingException("hexcasting.error.sentinel_wrong_dimension");
                }
                double dx = state.x - from.x;
                double dy = state.y - from.y;
                double dz = state.z - from.z;
                double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (length > 0.0D) {
                    dx /= length;
                    dy /= length;
                    dz /= length;
                }
                stack.push(vectorIota(new Vec3d(dx, dy, dz)));
            }
        };
    }

    private static EntityPlayer requirePlayer(CastingVM vm) throws CastingException {
        if (vm == null || vm.getPlayer() == null || vm.getPlayer().world == null
            || vm.getPlayer().world.isRemote) {
            throw new CastingException("hexcasting.error.sentinel_context");
        }
        return vm.getPlayer();
    }

    private static void assertTargetInRange(EntityPlayer player, Vec3d target,
                                             boolean extendedRange) throws CastingException {
        if (target == null || Double.isNaN(target.x) || Double.isNaN(target.y)
            || Double.isNaN(target.z) || Double.isInfinite(target.x)
            || Double.isInfinite(target.y) || Double.isInfinite(target.z)) {
            throw new CastingException("hexcasting.error.sentinel_out_of_range");
        }
        double dx = target.x - player.posX;
        double dy = target.y - player.posY;
        double dz = target.z - player.posZ;
        double range = extendedRange ? 128.0D : 64.0D;
        if (dx * dx + dy * dy + dz * dz > range * range) {
            throw new CastingException("hexcasting.error.sentinel_out_of_range");
        }
    }

    private static HexPattern pattern(HexDir start, String notation) {
        HexAngle[] angles = new HexAngle[notation.length()];
        for (int i = 0; i < notation.length(); i++) {
            switch (notation.charAt(i)) {
                case 'w': angles[i] = HexAngle.FORWARD; break;
                case 'e': angles[i] = HexAngle.RIGHT; break;
                case 'd': angles[i] = HexAngle.RIGHT_BACK; break;
                case 's': angles[i] = HexAngle.BACK; break;
                case 'a': angles[i] = HexAngle.LEFT_BACK; break;
                case 'q': angles[i] = HexAngle.LEFT; break;
                default: throw new IllegalArgumentException("Unknown Hex angle: " + notation.charAt(i));
            }
        }
        return new HexPattern(start, Arrays.asList(angles));
    }

    private static Vec3d vectorOf(Vec3Iota iota) {
        return iota.getValue();
    }

    private static Iota vectorIota(Vec3d value) {
        return new Vec3Iota(value);
    }
}
