package at.petra_k.hexcasting.common.lib.hex;

import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.eval.Mishap;
import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.NullIota;
import at.petra_k.hexcasting.api.casting.iota.Vec3Iota;
import at.petra_k.hexcasting.api.misc.MediaConstants;
import at.petra_k.hexcasting.common.world.SentinelData;
import at.petra_k.hexcasting.common.network.MsgSentinelStatusS2C;
import at.petrak.paucal.api.PaucalAPI;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
                throw Mishap.legacy("hexcasting.error.sentinel_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                EntityPlayer player = requirePlayer(vm);
                Vec3d target = vectorOf(stack.pop(Vec3Iota.class));
                assertTargetInRange(player, target);
                vm.consumeMedia(MediaConstants.DUST_UNIT * (extendedRange ? 2L : 1L));
                SentinelData.get(player.world).set(
                    player.getUniqueID(), extendedRange, target.x, target.y, target.z,
                    player.world.provider.getDimension());
                sync(player);
            }
        };
    }

    private static HexAction destroyAction() {
        return new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw Mishap.legacy("hexcasting.error.sentinel_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                EntityPlayer player = requirePlayer(vm);
                SentinelData.State state = SentinelData.get(player.world).get(player.getUniqueID());
                if (state != null && state.dimension != player.world.provider.getDimension()) {
                    throw Mishap.legacy("hexcasting.error.sentinel_wrong_dimension");
                }
                vm.consumeMedia(NEGLIGIBLE_MEDIA);
                SentinelData.get(player.world).clear(player.getUniqueID());
                sync(player);
            }
        };
    }

    private static HexAction getPositionAction() {
        return new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw Mishap.legacy("hexcasting.error.sentinel_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                EntityPlayer player = requirePlayer(vm);
                SentinelData.State state = SentinelData.get(player.world).get(player.getUniqueID());
                if (state == null) {
                    vm.consumeMedia(NEGLIGIBLE_MEDIA);
                    stack.push(new NullIota());
                    return;
                }
                if (state.dimension != player.world.provider.getDimension()) {
                    throw Mishap.legacy("hexcasting.error.sentinel_wrong_dimension");
                }
                vm.consumeMedia(NEGLIGIBLE_MEDIA);
                stack.push(vectorIota(new Vec3d(state.x, state.y, state.z)));
            }
        };
    }

    private static HexAction wayfindAction() {
        return new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw Mishap.legacy("hexcasting.error.sentinel_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                EntityPlayer player = requirePlayer(vm);
                Vec3d from = vectorOf(stack.pop(Vec3Iota.class));
                SentinelData.State state = SentinelData.get(player.world).get(player.getUniqueID());
                if (state == null) {
                    vm.consumeMedia(NEGLIGIBLE_MEDIA);
                    stack.push(new NullIota());
                    return;
                }
                if (state.dimension != player.world.provider.getDimension()) {
                    throw Mishap.legacy("hexcasting.error.sentinel_wrong_dimension");
                }
                vm.consumeMedia(NEGLIGIBLE_MEDIA);
                stack.push(vectorIota(normalizedDifference(state, from)));
            }
        };
    }

    private static EntityPlayer requirePlayer(CastingVM vm) throws CastingException {
        if (vm == null || vm.getPlayer() == null || vm.getPlayer().world == null
            || vm.getPlayer().world.isRemote) {
            throw Mishap.legacy("hexcasting.error.sentinel_context");
        }
        return vm.getPlayer();
    }

    private static void sync(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            PaucalAPI.sendTo(new MsgSentinelStatusS2C(player), player);
        }
    }

    private static void assertTargetInRange(EntityPlayer player, Vec3d target)
        throws CastingException {
        if (target == null || Double.isNaN(target.x) || Double.isNaN(target.y)
            || Double.isNaN(target.z) || Double.isInfinite(target.x)
            || Double.isInfinite(target.y) || Double.isInfinite(target.z)) {
            throw Mishap.legacy("hexcasting.error.sentinel_out_of_range");
        }
        if (target.y < 0.0D || target.y >= 256.0D
            || Math.abs(target.x) > 30000000.0D
            || Math.abs(target.z) > 30000000.0D) {
            throw Mishap.legacy("hexcasting.error.sentinel_out_of_range");
        }
        double dx = target.x - player.posX;
        double dy = target.y - player.posY;
        double dz = target.z - player.posZ;
        double range = 32.0D;
        if (dx * dx + dy * dy + dz * dz > range * range + 1.0E-8D) {
            throw Mishap.legacy("hexcasting.error.sentinel_out_of_range");
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

    private static Vec3d vectorOf(Vec3Iota iota) throws CastingException {
        Vec3d value = iota == null ? null : iota.getValue();
        if (value == null || !isFinite(value)) {
            throw Mishap.legacy("hexcasting.error.sentinel_vector");
        }
        return value;
    }

    private static Iota vectorIota(Vec3d value) {
        return new Vec3Iota(value);
    }

    private static boolean isFinite(Vec3d value) {
        return value != null
            && !Double.isNaN(value.x) && !Double.isInfinite(value.x)
            && !Double.isNaN(value.y) && !Double.isInfinite(value.y)
            && !Double.isNaN(value.z) && !Double.isInfinite(value.z);
    }

    private static Vec3d normalizedDifference(SentinelData.State state,
                                               Vec3d from) {
        double dx = state.x - from.x;
        double dy = state.y - from.y;
        double dz = state.z - from.z;
        double scale = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        if (!Double.isFinite(scale) || scale < 1.0E-4D) {
            return new Vec3d(0.0D, 0.0D, 0.0D);
        }
        dx /= scale;
        dy /= scale;
        dz /= scale;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!Double.isFinite(length) || length < 1.0E-4D) {
            return new Vec3d(0.0D, 0.0D, 0.0D);
        }
        return new Vec3d(dx / length, dy / length, dz / length);
    }
}
