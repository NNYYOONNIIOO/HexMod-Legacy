package at.petra_k.hexcasting.api.casting.eval.vm;

import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.ListIota;
import at.petra_k.hexcasting.api.casting.iota.PatternIota;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import at.petra_k.hexcasting.common.lib.hex.HexIotaTypes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import at.petra_k.hexcasting.api.capability.IHexCastingData;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Small, server-safe casting VM for the 1.12.2 port.
 *
 * <p>The original mod drives evaluation through continuation frames. This
 * first Java slice keeps the important property that the remaining work is
 * explicit and can be stepped or resumed while the frame types are migrated.
 * Future parenthesis, operation, and for_each frames can enqueue their own
 * work without changing callers of this class.</p>
 */
public final class CastingVM {
    /** Conservative default matching the old port's bounded evaluation goal. */
    public static final int DEFAULT_MAX_OPERATIONS = 1024;

    private static final class WorkItem {
        private final HexPattern pattern;
        private final Iota iota;

        private WorkItem(HexPattern pattern, Iota iota) {
            this.pattern = pattern;
            this.iota = iota;
        }

        private static WorkItem pattern(HexPattern pattern) {
            return new WorkItem(pattern, null);
        }

        private static WorkItem iota(Iota iota) {
            return new WorkItem(null, iota);
        }
    }

    private static final class ParenFrame {
        private final ArrayList<Iota> values = new ArrayList<>();
    }

    private final CastingStack stack;
    private final ArrayDeque<WorkItem> continuation = new ArrayDeque<>();
    private final ArrayDeque<ParenFrame> parentheses = new ArrayDeque<>();
    private boolean escapeNext;
    private boolean halted;
    private boolean lastNestedRunHalted;
    private int operationsConsumed;
    private int activeOperationLimit = DEFAULT_MAX_OPERATIONS;
    private IHexCastingData castingData;
    private EntityPlayer player;

    public CastingVM() {
        this(new CastingStack());
    }

    public CastingVM(CastingStack stack) {
        if (stack == null) {
            throw new IllegalArgumentException("Casting VM stack cannot be null");
        }
        this.stack = stack;
    }

    /** Create a VM with a sequence of patterns ready to execute. */
    public static CastingVM from(List<HexPattern> patterns) {
        return new CastingVM().enqueue(patterns);
    }

    /** Append patterns in source order to the pending continuation. */
    public CastingVM enqueue(List<HexPattern> patterns) {
        if (patterns == null) {
            throw new IllegalArgumentException("Pattern sequence cannot be null");
        }
        for (HexPattern pattern : patterns) {
            enqueue(pattern);
        }
        return this;
    }

    /** Prepend patterns in source order so they execute before existing work. */
    public CastingVM enqueueFront(List<HexPattern> patterns) {
        if (patterns == null) {
            throw new IllegalArgumentException("Pattern sequence cannot be null");
        }
        for (int i = patterns.size() - 1; i >= 0; i--) {
            enqueueFront(patterns.get(i));
        }
        return this;
    }

    /** Append one pattern to the pending continuation. */
    public CastingVM enqueue(HexPattern pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }
        continuation.addLast(WorkItem.pattern(pattern));
        return this;
    }

    /** Prepend one pattern before all currently pending work. */
    public CastingVM enqueueFront(HexPattern pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }
        continuation.addFirst(WorkItem.pattern(pattern));
        return this;
    }

    /** Append executable Iotas in source order to the pending continuation. */
    public CastingVM enqueueIotas(List<? extends Iota> iotas) {
        if (iotas == null) {
            throw new IllegalArgumentException("Iota sequence cannot be null");
        }
        for (Iota iota : iotas) {
            enqueueIota(iota);
        }
        return this;
    }

    /** Append one executable Iota to the pending continuation. */
    public CastingVM enqueueIota(Iota iota) {
        if (iota == null) {
            throw new IllegalArgumentException("Iota cannot be null");
        }
        continuation.addLast(WorkItem.iota(iota));
        return this;
    }

    /** Prepend executable Iotas in source order. */
    public CastingVM enqueueFrontIotas(List<? extends Iota> iotas) {
        if (iotas == null) {
            throw new IllegalArgumentException("Iota sequence cannot be null");
        }
        for (int i = iotas.size() - 1; i >= 0; i--) {
            Iota iota = iotas.get(i);
            if (iota == null) {
                throw new IllegalArgumentException("Iota cannot be null");
            }
            continuation.addFirst(WorkItem.iota(iota));
        }
        return this;
    }

    public IHexCastingData getCastingData() {
        return castingData;
    }

    public void setCastingData(IHexCastingData castingData) {
        this.castingData = castingData;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public void setPlayer(EntityPlayer player) {
        this.player = player;
    }

    public CastingStack getStack() {
        return stack;
    }

    public int getOperationsConsumed() {
        return operationsConsumed;
    }

    /** Return the number of operations still available in a given budget. */
    public int getRemainingOperations(int maxOperations) {
        validateBudget(maxOperations);
        return Math.max(0, maxOperations - operationsConsumed);
    }

    public int getPendingCount() {
        return continuation.size();
    }

    public boolean isHalted() {
        return halted;
    }

    /** Whether the most recent nested evaluation stopped on a halt action. */
    public boolean wasLastNestedRunHalted() {
        return lastNestedRunHalted;
    }

    /** Stop this VM and discard all currently queued work. */
    public void halt() {
        halted = true;
        continuation.clear();
    }

    public int getParenDepth() {
        return parentheses.size();
    }

    public boolean isEscapeNext() {
        return escapeNext;
    }

    public void setEscapeNext() {
        escapeNext = true;
    }

    /** Clear runtime escape state at a meta-evaluation boundary. */
    public void resetEscape() {
        escapeNext = false;
    }

    public void openParen() {
        parentheses.push(new ParenFrame());
    }

    public void openParens(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Parenthesis count cannot be negative");
        }
        for (int i = 0; i < count; i++) {
            openParen();
        }
    }

    public void closeParen() throws CastingException {
        if (parentheses.isEmpty()) {
            throw new CastingException("Cannot close a parenthesis when none is open");
        }
        ParenFrame frame = parentheses.pop();
        ListIota result = new ListIota(frame.values);
        if (parentheses.isEmpty()) {
            stack.push(result);
        } else {
            parentheses.peek().values.add(result);
        }
    }

    public void closeAllParens() throws CastingException {
        while (!parentheses.isEmpty()) {
            closeParen();
        }
    }

    /** Move one stack value into the currently captured parenthesized list. */
    public void readIntoParen() throws CastingException {
        if (parentheses.isEmpty()) {
            throw new CastingException("Cannot read into parentheses when none is open");
        }
        parentheses.peek().values.add(stack.pop());
    }

    /** Undo the latest captured value, or the current empty parenthesis frame. */
    public void undo() throws CastingException {
        if (parentheses.isEmpty()) {
            throw new CastingException("Undo requires an open parenthesis");
        }
        ParenFrame frame = parentheses.peek();
        if (frame.values.isEmpty()) {
            parentheses.pop();
        } else {
            frame.values.remove(frame.values.size() - 1);
        }
    }

    /**
     * Serialize the complete resumable VM state for a player capability or
     * packaged casting item. Pending patterns are represented as PatternIotas
     * so they use the same versioned Iota codec as every other queued value.
     */
    public NBTTagCompound serializeState() {
        NBTTagCompound out = new NBTTagCompound();
        out.setTag("stack", stack.serializeState());
        out.setInteger("operationsConsumed", operationsConsumed);
        out.setBoolean("escapeNext", escapeNext);
        out.setBoolean("halted", halted);
        NBTTagList parenthesisTags = new NBTTagList();
        for (ParenFrame frame : parentheses) {
            NBTTagCompound frameTag = new NBTTagCompound();
            NBTTagList values = new NBTTagList();
            for (Iota value : frame.values) {
                values.appendTag(value.serialize());
            }
            frameTag.setTag("values", values);
            parenthesisTags.appendTag(frameTag);
        }
        out.setTag("parentheses", parenthesisTags);
        NBTTagList pending = new NBTTagList();
        for (WorkItem work : continuation) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("kind", work.pattern == null ? "iota" : "pattern");
            Iota value = work.pattern == null ? work.iota : new PatternIota(work.pattern);
            entry.setTag("iota", value.serialize());
            pending.appendTag(entry);
        }
        out.setTag("continuation", pending);
        return out;
    }

    /** Restore a VM snapshot produced by {@link #serializeState()}. */
    public static CastingVM deserializeState(NBTTagCompound serialized) throws CastingException {
        if (serialized == null || !serialized.hasKey("stack", 10)) {
            throw new CastingException("Missing casting VM stack state");
        }
        CastingVM vm = new CastingVM(
            CastingStack.deserializeState(serialized.getCompoundTag("stack")));
        vm.operationsConsumed = Math.max(0, serialized.getInteger("operationsConsumed"));
        vm.escapeNext = serialized.getBoolean("escapeNext");
        vm.halted = serialized.getBoolean("halted");
        if (serialized.hasKey("parentheses", 9)) {
            NBTTagList parenthesisTags = serialized.getTagList("parentheses", 10);
            if (parenthesisTags.tagCount() > Iota.MAX_SERIALIZATION_TOTAL) {
                throw new CastingException("Serialized parenthesis state exceeded its size limit");
            }
            for (int i = parenthesisTags.tagCount() - 1; i >= 0; i--) {
                NBTTagCompound frameTag = parenthesisTags.getCompoundTagAt(i);
                NBTTagList values = frameTag.getTagList("values", 10);
                ParenFrame frame = new ParenFrame();
                for (int j = 0; j < values.tagCount(); j++) {
                    frame.values.add(HexIotaTypes.deserialize(values.getCompoundTagAt(j)));
                }
                vm.parentheses.push(frame);
            }
        }
        if (!serialized.hasKey("continuation", 9)) {
            return vm;
        }
        NBTTagList pending = serialized.getTagList("continuation", 10);
        if (pending.tagCount() > Iota.MAX_SERIALIZATION_TOTAL) {
            throw new CastingException("Serialized casting continuation exceeded its size limit");
        }
        for (int i = 0; i < pending.tagCount(); i++) {
            NBTTagCompound entry = pending.getCompoundTagAt(i);
            if (!entry.hasKey("iota", 10)) {
                throw new CastingException("Serialized casting continuation entry is missing its Iota");
            }
            Iota value = HexIotaTypes.deserialize(entry.getCompoundTag("iota"));
            if ("pattern".equals(entry.getString("kind"))) {
                if (!(value instanceof PatternIota)) {
                    throw new CastingException("Serialized pattern continuation entry is not a PatternIota");
                }
                vm.continuation.addLast(WorkItem.iota(value));
            } else {
                vm.continuation.addLast(WorkItem.iota(value));
            }
        }
        return vm;
    }

    public boolean hasPendingWork() {
        return !halted && !continuation.isEmpty();
    }

    /** Remove all pending work while retaining the current stack state. */
    public void clearPendingWork() {
        continuation.clear();
    }

    /** Reset only the operation budget counter. */
    public void resetOperationCounter() {
        operationsConsumed = 0;
    }

    /** Execute one pending pattern using the default budget. */
    public boolean step() throws CastingException {
        return step(activeOperationLimit);
    }

    /**
     * Execute one pending pattern.
     *
     * @return false when no work remains
     */
    public boolean step(int maxOperations) throws CastingException {
        validateBudget(maxOperations);
        if (halted) {
            continuation.clear();
            return false;
        }
        if (continuation.isEmpty()) {
            return false;
        }
        if (operationsConsumed >= maxOperations) {
            throw new CastingException("Casting evaluation exceeded its operation limit of "
                + maxOperations);
        }

        WorkItem work = continuation.removeFirst();
        HexPattern pattern = work.pattern;
        if (pattern == null && work.iota instanceof PatternIota) {
            pattern = ((PatternIota) work.iota).getPattern();
        }
        HexAction action = pattern == null ? null : HexActionRegistry.get(pattern);
        if (pattern != null && action == null && parentheses.isEmpty() && !escapeNext) {
            throw new CastingException("No action is registered for pattern " + pattern.anglesSignature());
        }
        Iota value = pattern == null ? work.iota : new PatternIota(pattern);

        // Count before execution so a failing action cannot be retried
        // indefinitely by a caller resuming the VM. Nested actions inherit
        // this exact budget instead of silently falling back to 1024.
        operationsConsumed++;
        int previousLimit = activeOperationLimit;
        activeOperationLimit = maxOperations;
        try {
            if (escapeNext) {
                escapeNext = false;
                capture(value);
            } else if (!parentheses.isEmpty() && (action == null || !action.executesInParentheses())) {
                parentheses.peek().values.add(value);
            } else if (action != null) {
                action.execute(stack, this);
            } else {
                stack.push(value);
            }
        } finally {
            activeOperationLimit = previousLimit;
        }
        return true;
    }

    /** Drain all pending work using the default operation budget. */
    /** Consume persistent player media for a contextual spell action. */
    public void consumeMedia(long amount) throws CastingException {
        if (amount <= 0L) {
            return;
        }
        if (castingData == null) {
            throw new CastingException("hexcasting.error.no_media_context");
        }
        long available = castingData.getMedia();
        if (available < amount) {
            throw new CastingException("hexcasting.error.not_enough_media");
        }
        castingData.setMedia(available - amount);
    }

    public CastingStack run() throws CastingException {
        return run(activeOperationLimit);
    }

    /** Drain all pending work, failing deterministically if the budget is hit. */
    public CastingStack run(int maxOperations) throws CastingException {
        validateBudget(maxOperations);
        try {
            while (hasPendingWork()) {
                step(maxOperations);
            }
        } finally {
            // Halt is scoped to this evaluation and must not permanently
            // disable a staff when its state is saved afterward.
            halted = false;
        }
        return stack;
    }

    /**
     * Run a nested pattern sequence before the VM's existing pending work.
     * The nested sequence consumes the same operation counter and budget as
     * its caller, which prevents control-flow actions from bypassing limits.
     */
    public CastingStack runNested(List<HexPattern> patterns) throws CastingException {
        return runNested(patterns, activeOperationLimit);
    }

    public CastingStack runNested(List<HexPattern> patterns, int maxOperations)
        throws CastingException {
        validateBudget(maxOperations);
        if (patterns == null) {
            throw new IllegalArgumentException("Nested pattern sequence cannot be null");
        }
        ArrayDeque<WorkItem> outerContinuation = new ArrayDeque<>(continuation);
        boolean previousHalted = halted;
        halted = false;
        lastNestedRunHalted = false;
        continuation.clear();
        try {
            for (HexPattern pattern : patterns) {
                enqueue(pattern);
            }
            while (!continuation.isEmpty()) {
                step(maxOperations);
            }
        } finally {
            lastNestedRunHalted = halted;
            escapeNext = false;
            halted = previousHalted;
            continuation.addAll(outerContinuation);
        }
        return stack;
    }

    /** Run executable Iotas before the VM's existing pending work. */
    public CastingStack runNestedIotas(List<? extends Iota> iotas) throws CastingException {
        return runNestedIotas(iotas, activeOperationLimit);
    }

    public CastingStack runNestedIotas(List<? extends Iota> iotas, int maxOperations)
        throws CastingException {
        validateBudget(maxOperations);
        if (iotas == null) {
            throw new IllegalArgumentException("Nested Iota sequence cannot be null");
        }
        ArrayDeque<WorkItem> outerContinuation = new ArrayDeque<>(continuation);
        boolean previousHalted = halted;
        halted = false;
        lastNestedRunHalted = false;
        continuation.clear();
        try {
            enqueueIotas(iotas);
            while (!continuation.isEmpty()) {
                step(maxOperations);
            }
        } finally {
            lastNestedRunHalted = halted;
            escapeNext = false;
            halted = previousHalted;
            continuation.addAll(outerContinuation);
        }
        return stack;
    }

    private void capture(Iota value) throws CastingException {
        if (parentheses.isEmpty()) {
            stack.push(value);
        } else {
            parentheses.peek().values.add(value);
        }
    }

    private static void validateBudget(int maxOperations) {
        if (maxOperations <= 0) {
            throw new IllegalArgumentException("Operation limit must be positive");
        }
    }
}
