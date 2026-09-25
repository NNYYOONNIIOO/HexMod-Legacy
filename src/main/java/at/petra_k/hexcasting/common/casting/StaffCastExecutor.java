package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.eval.Mishap;
import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;
import at.petra_k.hexcasting.common.lib.HexSounds;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private static final int MAX_PREVIEW_ENTRIES = 64;

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

    public static boolean isStackClear(ItemStack staff) {
        if (staff == null || staff.isEmpty()) {
            return false;
        }
        try {
            CastingVM vm = load(staff);
            // Hex keeps the staff spell open while a parenthesized program is
            // being collected, even when the value stack is empty.  Checking
            // only stack size makes drawing Introspection (open_paren) reset
            // the VM immediately, so the following patterns execute instead
            // of being captured until Retrospection (close_paren).
            return vm.getStack().size() == 0
                && vm.getParenDepth() == 0
                && !vm.isEscapeNext();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static boolean execute(EntityPlayer player, EnumHand hand, ItemStack staff,
                                  HexPattern pattern) {
        return executeDetailed(player, hand, staff, pattern).isSuccess();
    }

    /**
     * Execute one drawn pattern and retain the client-visible resolution state.
     * The 1.20.1 GUI uses this state to color each line independently; a
     * boolean success flag is not enough because escaped and parenthesized
     * patterns have different meanings and colors.
     */
    public static CastOutcome executeDetailed(EntityPlayer player, EnumHand hand,
                                              ItemStack staff, HexPattern pattern) {
        if (player == null || staff == null || staff.isEmpty() || pattern == null) {
            return CastOutcome.failure(Resolution.ERRORED,
                Collections.<String>emptyList(), 0, 0, false, HexSounds.CAST_FAILURE, null);
        }
        IHexCastingData castingData =
            player.getCapability(HexCapabilities.CASTING_DATA, null);
        if (castingData == null) {
            MishapFeedback.send(player,
                Mishap.error("hexcasting.message.staff_error"));
            return CastOutcome.failure(Resolution.ERRORED,
                Collections.<String>emptyList(), 0, 0, false, HexSounds.CAST_FAILURE, null);
        }

        CastingVM vm = null;
        at.petra_k.hexcasting.api.casting.action.HexAction action = null;
        try {
            HexActionRegistry.bootstrap();
            vm = load(staff);
            vm.setCastingData(castingData);
            vm.setPlayer(player);
            vm.setCastingHand(hand);
            vm.setMediaConsumptionBypassed(player.capabilities.isCreativeMode);
            boolean wasEscaped = vm.isEscapeNext();
            boolean wasInParens = vm.getParenDepth() > 0;
            action = HexActionRegistry.get(pattern, player.world);
            boolean isCaptured = wasEscaped || (wasInParens
                && (action == null || !action.executesInParentheses()));
            vm.enqueue(pattern);
            vm.run(CastingVM.DEFAULT_MAX_OPERATIONS);
            save(staff, vm);
            Resolution resolution = resolveResolution(action, wasEscaped,
                wasInParens, vm);
            return CastOutcome.success(resolution, preview(vm),
                vm.getStack().size(), vm.getParenDepth(), vm.isEscapeNext(),
                isStackClear(vm), soundFor(vm, true), null);
        } catch (CastingException exception) {
            Mishap mishap = exception instanceof Mishap
                ? (Mishap) exception
                : Mishap.from(exception, pattern,
                    vm == null ? null : HexActionRegistry.idFor(action),
                    player, vm == null ? 0 : vm.getParenDepth(),
                    vm == null ? 0 : vm.getOperationsConsumed());
            if (vm != null) {
                vm.clearPendingWork();
                save(staff, vm);
            }
            MishapFeedback.send(player, mishap);
            return CastOutcome.failure(Resolution.ERRORED,
                preview(vm), stackSize(vm), parenDepth(vm), escapeNext(vm),
                soundFor(vm, false), mishap);
        } catch (RuntimeException exception) {
            Mishap mishap = Mishap.fromRuntime(exception, pattern,
                HexActionRegistry.idFor(action), player,
                vm == null ? 0 : vm.getParenDepth(),
                vm == null ? 0 : vm.getOperationsConsumed());
            if (vm != null) {
                vm.clearPendingWork();
                save(staff, vm);
            }
            MishapFeedback.send(player, mishap);
            return CastOutcome.failure(Resolution.ERRORED,
                preview(vm), stackSize(vm), parenDepth(vm), escapeNext(vm),
                soundFor(vm, false), mishap);
        }
    }

    private static SoundEvent soundFor(CastingVM vm, boolean success) {
        if (vm != null && vm.getSound() != null && vm.getSound().getSound() != null) {
            return vm.getSound().getSound();
        }
        return success ? HexSounds.CAST_NORMAL : HexSounds.CAST_FAILURE;
    }

    private static boolean isStackClear(CastingVM vm) {
        return vm != null && vm.getStack().size() == 0
            && vm.getParenDepth() == 0 && !vm.isEscapeNext();
    }

    private static Resolution resolveResolution(
        at.petra_k.hexcasting.api.casting.action.HexAction action,
        boolean wasEscaped, boolean wasInParens, CastingVM vm) {
        if (wasEscaped) {
            return Resolution.ESCAPED;
        }
        if (action instanceof ParenControlAction) {
            ParenControlAction.Kind kind = ((ParenControlAction) action).getKind();
            if (kind == ParenControlAction.Kind.UNDO) {
                return Resolution.UNDONE;
            }
            if (kind == ParenControlAction.Kind.OPEN && wasInParens) {
                return Resolution.ESCAPED;
            }
            if (kind == ParenControlAction.Kind.CLOSE && wasInParens
                && vm.getParenDepth() > 0) {
                return Resolution.ESCAPED;
            }
        }
        if (wasInParens && (action == null || !action.executesInParentheses())) {
            return Resolution.ESCAPED;
        }
        return Resolution.EVALUATED;
    }

    private static int parenDepth(CastingVM vm) {
        return vm == null ? 0 : Math.max(0, vm.getParenDepth());
    }

    private static int stackSize(CastingVM vm) {
        return vm == null || vm.getStack() == null ? 0 : vm.getStack().size();
    }

    private static boolean escapeNext(CastingVM vm) {
        return vm != null && vm.isEscapeNext();
    }

    /**
     * Read the persisted VM state without executing another pattern.
     *
     * <p>The staff screen is opened locally on the client, but the VM lives
     * on the server-side staff stack.  A reopen/world join therefore needs a
     * real server snapshot instead of assuming that the last client packet
     * is still available.</p>
     */
    public static CastOutcome getCurrentState(ItemStack staff) {
        if (staff == null || staff.isEmpty()) {
            return CastOutcome.success(Resolution.UNRESOLVED,
                Collections.<String>emptyList(), 0, 0, false, true, null, null);
        }
        try {
            CastingVM vm = load(staff);
            return CastOutcome.success(Resolution.UNRESOLVED, preview(vm),
                stackSize(vm), parenDepth(vm), escapeNext(vm),
                isStackClear(vm), null, null);
        } catch (RuntimeException ignored) {
            return CastOutcome.success(Resolution.UNRESOLVED,
                Collections.<String>emptyList(), 0, 0, false, false, null, null);
        }
    }

    private static List<String> preview(CastingVM vm) {
        if (vm == null || vm.getStack() == null) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        List<at.petra_k.hexcasting.api.casting.iota.Iota> snapshot =
            vm.getStack().snapshot();
        for (int i = snapshot.size() - 1;
             i >= 0 && values.size() < MAX_PREVIEW_ENTRIES; i--) {
            at.petra_k.hexcasting.api.casting.iota.Iota value = snapshot.get(i);
            values.add(value == null ? "?" : value.display());
        }
        return values;
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

    /** The solid outer-stroke colour used by the 1.20.1 staff GUI. */
    public enum Resolution {
        UNRESOLVED(0xFF7F7F7F),
        EVALUATED(0xFF7385DE),
        ESCAPED(0xFFDDCC73),
        UNDONE(0xFFB26B6B),
        ERRORED(0xFFDE6262),
        INVALID(0xFFB26B6B);

        private final int color;

        Resolution(int color) {
            this.color = color;
        }

        public int getColor() {
            return color;
        }
    }

    public static final class CastOutcome {
        private final Resolution resolution;
        private final List<String> stackPreview;
        private final int stackSize;
        private final int parenDepth;
        private final boolean escapeNext;
        private final boolean stackClear;
        private final SoundEvent sound;
        private final Mishap mishap;

        private CastOutcome(Resolution resolution, List<String> stackPreview,
                            int stackSize, int parenDepth, boolean escapeNext,
                            boolean stackClear, SoundEvent sound, Mishap mishap) {
            this.resolution = resolution == null ? Resolution.ERRORED : resolution;
            this.stackPreview = Collections.unmodifiableList(new ArrayList<>(
                stackPreview == null ? Collections.<String>emptyList() : stackPreview));
            this.stackSize = Math.max(0, stackSize);
            this.parenDepth = Math.max(0, parenDepth);
            this.escapeNext = escapeNext;
            this.stackClear = stackClear;
            this.sound = sound;
            this.mishap = mishap;
        }

        private static CastOutcome success(Resolution resolution,
                                           List<String> stackPreview,
                                           int stackSize, int parenDepth, boolean escapeNext,
                                           boolean stackClear, SoundEvent sound,
                                           Mishap mishap) {
            return new CastOutcome(resolution, stackPreview, stackSize,
                parenDepth, escapeNext, stackClear, sound, mishap);
        }

        private static CastOutcome failure(Resolution resolution,
                                           List<String> stackPreview,
                                           int stackSize, int parenDepth,
                                            boolean escapeNext, SoundEvent sound,
                                            Mishap mishap) {
            return new CastOutcome(resolution, stackPreview, stackSize,
                parenDepth, escapeNext, false, sound, mishap);
        }

        public boolean isSuccess() {
            return resolution != Resolution.ERRORED
                && resolution != Resolution.INVALID;
        }

        public Resolution getResolution() {
            return resolution;
        }

        public List<String> getStackPreview() {
            return stackPreview;
        }

        public int getStackSize() {
            return stackSize;
        }

        public int getParenDepth() {
            return parenDepth;
        }

        public boolean isEscapeNext() {
            return escapeNext;
        }

        public boolean isStackClear() {
            return stackClear;
        }

        public SoundEvent getSound() {
            return sound;
        }

        public Mishap getMishap() {
            return mishap;
        }
    }
}
