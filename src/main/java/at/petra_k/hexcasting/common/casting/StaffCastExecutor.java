package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;

import java.util.Locale;

/**
 * Executes one newly drawn staff pattern with a resumable per-staff VM.
 *
 * <p>The modern Hex staff keeps the continuation, stack, parentheses, and
 * local value between individual patterns.  A 1.12.2 ItemStack is a useful
 * equivalent persistence boundary: two physical staffs cannot accidentally
 * share a continuation, and the state follows the staff through inventories.
 * The player's capability remains the environmental source for media and
 * other contextual actions.</p>
 */
public final class StaffCastExecutor {
    private static final String KEY_CASTING_STATE = "casting_state";

    private StaffCastExecutor() {
    }

    public static void clear(ItemStack staff) {
        if (staff == null || staff.isEmpty() || staff.getTagCompound() == null) {
            return;
        }
        staff.getTagCompound().removeTag(KEY_CASTING_STATE);
    }

    public static int getStackSize(ItemStack staff) {
        if (staff == null || staff.isEmpty()) {
            return 0;
        }
        try {
            return load(staff).getStack().size();
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    public static boolean execute(EntityPlayer player, EnumHand hand, ItemStack staff,
                                  HexPattern pattern) {
        if (player == null || staff == null || staff.isEmpty() || pattern == null) {
            return false;
        }
        IHexCastingData castingData =
            player.getCapability(HexCapabilities.CASTING_DATA, null);
        if (castingData == null) {
            sendError(player, "hexcasting.message.staff_error");
            return false;
        }

        CastingVM vm = null;
        try {
            HexActionRegistry.bootstrap();
            vm = load(staff);
            vm.setCastingData(castingData);
            vm.setPlayer(player);
            vm.enqueue(pattern);
            vm.run(CastingVM.DEFAULT_MAX_OPERATIONS);
            save(staff, vm);
            return true;
        } catch (CastingException exception) {
            if (vm != null) {
                vm.clearPendingWork();
                save(staff, vm);
            }
            sendError(player, exception.getMessage());
            return false;
        } catch (RuntimeException exception) {
            if (vm != null) {
                vm.clearPendingWork();
                save(staff, vm);
            }
            sendError(player, "hexcasting.message.staff_error");
            return false;
        }
    }

    private static CastingVM load(ItemStack staff) {
        NBTTagCompound tag = staff.getTagCompound();
        if (tag != null && tag.hasKey(KEY_CASTING_STATE, 10)) {
            try {
                return CastingVM.deserializeState(tag.getCompoundTag(KEY_CASTING_STATE));
            } catch (CastingException ignored) {
                tag.removeTag(KEY_CASTING_STATE);
            }
        }
        return new CastingVM(new CastingStack());
    }

    private static void save(ItemStack staff, CastingVM vm) {
        NBTTagCompound tag = staff.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            staff.setTagCompound(tag);
        }
        tag.setTag(KEY_CASTING_STATE, vm.serializeState());
    }

    private static void sendError(EntityPlayer player, String message) {
        player.sendMessage(new TextComponentString(localizeError(message)));
    }

    private static String localizeError(String message) {
        if (message == null || message.isEmpty()) {
            return I18n.translateToLocal("hexcasting.message.staff_error");
        }
        String lower = message.toLowerCase(Locale.ROOT);
        String marker = "no action is registered for pattern";
        int markerIndex = lower.indexOf(marker);
        if (markerIndex >= 0) {
            String signature = message.substring(markerIndex + marker.length()).trim();
            int open = signature.indexOf("HexPattern(");
            int close = signature.lastIndexOf(')');
            if (open >= 0 && close > open) {
                String raw = signature.substring(open + "HexPattern(".length(), close);
                int slash = raw.indexOf('/');
                signature = slash >= 0 ? raw.substring(slash + 1) : raw;
                signature = signature.replace("FORWARD", "w")
                    .replace("RIGHT_BACK", "d")
                    .replace("RIGHT", "e")
                    .replace("BACK", "s")
                    .replace("LEFT_BACK", "a")
                    .replace("LEFT", "q")
                    .replace("/", "");
            }
            return I18n.translateToLocalFormatted(
                "hexcasting.message.pattern_unregistered", signature);
        }
        String translated = I18n.translateToLocal(message);
        return message.equals(translated)
            ? I18n.translateToLocal("hexcasting.message.staff_error")
            : translated;
    }
}
