package at.petra_k.hexcasting.api.casting.eval.vm;

import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;

import java.util.ArrayDeque;
import java.util.List;

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

    private final CastingStack stack;
    private final ArrayDeque<HexPattern> continuation = new ArrayDeque<>();
    private int operationsConsumed;

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
        continuation.addLast(pattern);
        return this;
    }

    /** Prepend one pattern before all currently pending work. */
    public CastingVM enqueueFront(HexPattern pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }
        continuation.addFirst(pattern);
        return this;
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

    public boolean hasPendingWork() {
        return !continuation.isEmpty();
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
        return step(DEFAULT_MAX_OPERATIONS);
    }

    /**
     * Execute one pending pattern.
     *
     * @return false when no work remains
     */
    public boolean step(int maxOperations) throws CastingException {
        validateBudget(maxOperations);
        if (continuation.isEmpty()) {
            return false;
        }
        if (operationsConsumed >= maxOperations) {
            throw new CastingException("Casting evaluation exceeded its operation limit of "
                + maxOperations);
        }

        HexPattern pattern = continuation.removeFirst();
        HexAction action = HexActionRegistry.get(pattern);
        if (action == null) {
            throw new CastingException("No action is registered for pattern " + pattern.signature());
        }

        // Count before execution so a failing action cannot be retried
        // indefinitely by a caller resuming the VM.
        operationsConsumed++;
        action.execute(stack, this);
        return true;
    }

    /** Drain all pending work using the default operation budget. */
    public CastingStack run() throws CastingException {
        return run(DEFAULT_MAX_OPERATIONS);
    }

    /** Drain all pending work, failing deterministically if the budget is hit. */
    public CastingStack run(int maxOperations) throws CastingException {
        validateBudget(maxOperations);
        while (hasPendingWork()) {
            step(maxOperations);
        }
        return stack;
    }

    /**
     * Run a nested pattern sequence before the VM's existing pending work.
     * The nested sequence consumes the same operation counter and budget as
     * its caller, which prevents control-flow actions from bypassing limits.
     */
    public CastingStack runNested(List<HexPattern> patterns) throws CastingException {
        return runNested(patterns, DEFAULT_MAX_OPERATIONS);
    }

    public CastingStack runNested(List<HexPattern> patterns, int maxOperations)
        throws CastingException {
        validateBudget(maxOperations);
        if (patterns == null) {
            throw new IllegalArgumentException("Nested pattern sequence cannot be null");
        }
        int outerPendingCount = continuation.size();
        enqueueFront(patterns);
        while (continuation.size() > outerPendingCount) {
            step(maxOperations);
        }
        return stack;
    }

    private static void validateBudget(int maxOperations) {
        if (maxOperations <= 0) {
            throw new IllegalArgumentException("Operation limit must be positive");
        }
    }
}
