package at.petra_k.hexcasting.api.casting.eval.vm;

import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.eval.Mishap;
import at.petra_k.hexcasting.api.casting.eval.sideeffects.EvalSound;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.ContinuationIota;
import at.petra_k.hexcasting.api.casting.iota.DoubleIota;
import at.petra_k.hexcasting.api.casting.iota.ListIota;
import at.petra_k.hexcasting.api.casting.iota.PatternIota;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.api.casting.circles.CircleExecutionState;
import at.petra_k.hexcasting.api.addldata.ADMediaHolder;
import at.petra_k.hexcasting.common.casting.IotaDataHolder;
import at.petra_k.hexcasting.common.casting.MediaInventoryHelper;
import at.petra_k.hexcasting.common.casting.MishapFeedback;
import at.petra_k.hexcasting.common.casting.OvercastHelper;
import at.petra_k.hexcasting.common.casting.SpecialPatternResolver;
import at.petra_k.hexcasting.common.lib.hex.HexActions;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import at.petra_k.hexcasting.common.lib.hex.HexEvalSounds;
import at.petra_k.hexcasting.common.lib.hex.HexIotaTypes;
import at.petra_k.hexcasting.common.world.PerWorldPatternData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.entity.Entity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import at.petra_k.hexcasting.api.capability.IHexCastingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;

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
        private final ArrayList<ParenEntry> values = new ArrayList<>();
    }

    /** One iota captured in parentheses, including Hex's escaped marker. */
    private static final class ParenEntry {
        private final Iota value;
        private final boolean escaped;

        private ParenEntry(Iota value, boolean escaped) {
            this.value = value;
            this.escaped = escaped;
        }
    }

    /** Immutable-in-practice snapshot used to abandon one failed cast. */
    private static final class VmSnapshot {
        private final CastingStack stack;
        private final ArrayDeque<WorkItem> continuation;
        private final ArrayDeque<ParenFrame> parentheses;
        private final int parenCount;
        private final boolean escapeNext;
        private final boolean halted;
        private final boolean continuationInvoked;
        private final boolean lastNestedRunHalted;
        private final int operationsConsumed;
        private final NBTTagCompound userData;

        private VmSnapshot(CastingStack stack, ArrayDeque<WorkItem> continuation,
                           ArrayDeque<ParenFrame> parentheses, int parenCount,
                           boolean escapeNext, boolean halted,
                           boolean continuationInvoked, boolean lastNestedRunHalted,
                           int operationsConsumed, NBTTagCompound userData) {
            this.stack = stack;
            this.continuation = continuation;
            this.parentheses = parentheses;
            this.parenCount = parenCount;
            this.escapeNext = escapeNext;
            this.halted = halted;
            this.continuationInvoked = continuationInvoked;
            this.lastNestedRunHalted = lastNestedRunHalted;
            this.operationsConsumed = operationsConsumed;
            this.userData = userData == null ? new NBTTagCompound() : userData;
        }
    }

    private final CastingStack stack;
    private final ArrayDeque<WorkItem> continuation = new ArrayDeque<>();
    private final ArrayDeque<ParenFrame> parentheses = new ArrayDeque<>();
    /** Number of currently open parentheses; the captured values live in one flat frame. */
    private int parenCount;
    private boolean escapeNext;
    private boolean halted;
    private boolean lastNestedRunHalted;
    private boolean continuationInvoked;
    private int operationsConsumed;
    /** Persistent casting-image data for action-local bookkeeping. */
    private NBTTagCompound userData = new NBTTagCompound();
    private int activeOperationLimit = DEFAULT_MAX_OPERATIONS;
    private IHexCastingData castingData;
    private EntityPlayer player;
    /** The hand containing the staff/focus that started this cast. */
    private EnumHand castingHand = EnumHand.MAIN_HAND;
    /** Runtime-only circle context; it is rebound after a persisted state loads. */
    private CircleExecutionState circleExecutionState;
    /** Runtime-only media source; circles bind this to their Impetus. */
    private ADMediaHolder mediaHolder;
    /** Whether an explicitly bound holder may fall back to player media. */
    private boolean allowMediaInventoryFallback;
    /** Player-cast environments do not spend media while in creative mode. */
    private boolean mediaConsumptionBypassed;
    /** Shared source transaction for one complete evaluation. */
    private MediaInventoryHelper.MediaTransaction mediaTransaction;
    /**
     * Mutations performed by legacy actions which are not represented by the
     * player's media transaction.  Modern Hex applies rendered-spell changes
     * only after evaluation succeeds; keeping these callbacks on the VM gives
     * the 1.12.2 actions the same failure boundary without making every action
     * invent its own evaluator transaction.
     */
    private final ArrayList<Runnable> rollbackActions = new ArrayList<>();
    private int evaluationDepth;
    private Mishap lastMishap;
    /** Transient context collected while the currently executing action runs. */
    private Entity mishapTarget;
    private boolean mishapLocationRecorded;
    private double mishapLocationX;
    private double mishapLocationY;
    private double mishapLocationZ;
    private int mishapLocationDimension = Integer.MIN_VALUE;
    private boolean mishapPermissionChecked;
    private boolean mishapPermissionAllowed = true;
    /** Highest-precedence sound produced by the current evaluation. */
    private EvalSound sound = HexEvalSounds.NOTHING;

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

    public EnumHand getCastingHand() {
        return castingHand;
    }

    /**
     * Set the physical hand from which this VM is being resumed.
     * The hand is runtime context rather than serialized VM state: a staff can
     * move between hands while its continuation remains on the item.
     */
    public void setCastingHand(EnumHand castingHand) {
        this.castingHand = castingHand == null ? EnumHand.MAIN_HAND : castingHand;
    }

    public CircleExecutionState getCircleExecutionState() {
        return circleExecutionState;
    }

    public void setCircleExecutionState(CircleExecutionState circleExecutionState) {
        this.circleExecutionState = circleExecutionState;
    }

    public ADMediaHolder getMediaHolder() {
        return mediaHolder;
    }

    public void setMediaHolder(ADMediaHolder mediaHolder) {
        setMediaHolder(mediaHolder, false);
    }

    /**
     * Bind a media source and optionally allow ordinary player sources after
     * the bound source is exhausted.  The latter is used only by packaged
     * artifacts; circles and direct holders stay isolated by default.
     */
    public void setMediaHolder(ADMediaHolder mediaHolder,
                               boolean allowMediaInventoryFallback) {
        this.mediaHolder = mediaHolder;
        this.allowMediaInventoryFallback = allowMediaInventoryFallback;
    }

    /**
     * Mark this VM as a player-cast environment whose media costs are free in
     * creative mode.  Circle VMs deliberately leave this disabled: an
     * Impetus is its own media environment and still has to be drained.
     */
    public void setMediaConsumptionBypassed(boolean bypassed) {
        this.mediaConsumptionBypassed = bypassed;
    }

    public boolean isMediaConsumptionBypassed() {
        return mediaConsumptionBypassed;
    }

    /**
     * Register an item/entity mutation to undo if the outer evaluation fails.
     * Callbacks run in reverse order, matching the order of ordinary
     * transactional writes.  They are intentionally runtime-only and are not
     * serialized with a resumable casting image.
     */
    public void addRollbackAction(Runnable rollback) {
        if (rollback != null) {
            rollbackActions.add(rollback);
        }
    }

    /** Return the media still available to this VM's current cast. */
    public long getAvailableMedia() {
        if (mediaConsumptionBypassed) {
            return Long.MAX_VALUE;
        }
        long available;
        if (mediaTransaction != null) {
            available = mediaTransaction.getAvailableMedia();
        } else {
            available = MediaInventoryHelper.begin(player, castingData, mediaHolder,
                    allowMediaInventoryFallback)
                .getAvailableMedia();
        }
        return saturatingAdd(available, canOvercast()
            ? OvercastHelper.availableMedia(player) : 0L);
    }

    /** The structured Mishap produced by the latest failed operation. */
    public Mishap getLastMishap() {
        return lastMishap;
    }

    /** Record a component-level failure that could not be thrown through the VM. */
    public void recordMishap(Mishap mishap) {
        if (mishap != null) {
            lastMishap = mishap;
            recordSound(HexEvalSounds.MISHAP);
        }
    }

    /** Record an entity an action is resolving or validating for a Mishap. */
    public void recordMishapTarget(Entity target) {
        if (target != null) {
            mishapTarget = target;
        }
    }

    /** Record the position an action is validating or editing for a Mishap. */
    public void recordMishapLocation(double x, double y, double z, int dimension) {
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
            return;
        }
        mishapLocationRecorded = true;
        mishapLocationX = x;
        mishapLocationY = y;
        mishapLocationZ = z;
        mishapLocationDimension = dimension;
    }

    /** Record the result of a permission check for a Mishap. */
    public void recordMishapPermission(boolean allowed) {
        mishapPermissionChecked = true;
        mishapPermissionAllowed = allowed;
    }

    private void clearMishapContext() {
        mishapTarget = null;
        mishapLocationRecorded = false;
        mishapLocationX = 0.0D;
        mishapLocationY = 0.0D;
        mishapLocationZ = 0.0D;
        mishapLocationDimension = Integer.MIN_VALUE;
        mishapPermissionChecked = false;
        mishapPermissionAllowed = true;
    }

    private Mishap attachMishapContext(Mishap mishap) {
        if (mishap == null) {
            return null;
        }
        if (mishapTarget != null) {
            mishap.withTarget(mishapTarget);
        }
        if (mishapLocationRecorded) {
            mishap.withLocation(mishapLocationX, mishapLocationY,
                mishapLocationZ, mishapLocationDimension);
        }
        if (mishapPermissionChecked) {
            mishap.withPermission(true, mishapPermissionAllowed);
        }
        return mishap;
    }

    /** Return the sound selected by the actions evaluated so far. */
    public EvalSound getSound() {
        return sound;
    }

    /** Clear the runtime sound accumulator before starting a fresh cast. */
    public void resetSound() {
        sound = HexEvalSounds.NOTHING;
    }

    private void recordSound(EvalSound candidate) {
        if (candidate != null) {
            sound = sound.greaterOf(candidate);
        }
    }

    public EnumHand getOtherHand() {
        return castingHand == EnumHand.MAIN_HAND
            ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
    }

    /**
     * Find an item on the caster's primary/secondary hands, checking the other
     * hand first like modern Hex's CastingEnvironment.
     */
    public ItemStack getHeldItemToOperateOn(Predicate<ItemStack> predicate)
        throws CastingException {
        if (player == null) {
            throw Mishap.legacy("hexcasting.error.read_context");
        }
        if (predicate == null) {
            throw new IllegalArgumentException("Held-item predicate cannot be null");
        }

        ItemStack secondary = player.getHeldItem(getOtherHand());
        if (predicate.test(secondary)) {
            return secondary;
        }

        ItemStack primary = player.getHeldItem(castingHand);
        return predicate.test(primary) ? primary : null;
    }

    /** Return the physical hand containing a stack returned by the helper. */
    public EnumHand getHandForHeldItem(ItemStack held) {
        if (player == null || held == null || held.isEmpty()) {
            return null;
        }
        if (player.getHeldItem(getOtherHand()) == held) {
            return getOtherHand();
        }
        if (player.getHeldItem(castingHand) == held) {
            return castingHand;
        }
        return null;
    }

    public CastingStack getStack() {
        return stack;
    }

    public int getOperationsConsumed() {
        return operationsConsumed;
    }

    /**
     * Return the mutable userdata associated with this casting image.  It is
     * serialized with the resumable VM state, just like the modern
     * CastingImage.userData compound.
     */
    public NBTTagCompound getUserData() {
        return userData;
    }

    /**
     * Mark an entity as having received motion in this casting image.  The
     * first impulse is charged normally; later impulses to the same entity
     * pay the additional one-dust-unit surcharge used by modern Hex.
     */
    public boolean checkAndMarkGivenMotion(Entity entity) {
        if (entity == null) {
            return false;
        }
        NBTTagCompound marked;
        if (userData.hasKey(at.petra_k.hexcasting.api.HexAPI.MARKED_MOVED_USERDATA, 10)) {
            marked = userData.getCompoundTag(
                at.petra_k.hexcasting.api.HexAPI.MARKED_MOVED_USERDATA);
        } else {
            marked = new NBTTagCompound();
        }
        String uuid = entity.getUniqueID().toString();
        boolean alreadyMarked = marked.hasKey(uuid);
        if (!alreadyMarked) {
            marked.setBoolean(uuid, true);
            userData.setTag(at.petra_k.hexcasting.api.HexAPI.MARKED_MOVED_USERDATA, marked);
        }
        return alreadyMarked;
    }

    /** Return the number of operations still available in a given budget. */
    public int getRemainingOperations(int maxOperations) {
        validateBudget(maxOperations);
        return Math.max(0, maxOperations - operationsConsumed);
    }

    /**
     * Return the operations remaining before the currently executing action
     * consumes its operation.  The VM increments {@link #operationsConsumed}
     * before dispatching an action, while the modern image exposes the count
     * from before that increment to the action itself.
     */
    public int getRemainingOperationsForCurrentAction() {
        return Math.max(0, activeOperationLimit - Math.max(0, operationsConsumed - 1));
    }

    public int getPendingCount() {
        return continuation.size();
    }

    /** Snapshot the currently pending work for an eval/cc continuation value. */
    public ContinuationIota captureContinuation() {
        ArrayList<Iota> pending = new ArrayList<>(continuation.size());
        for (WorkItem work : continuation) {
            pending.add(work.pattern == null ? work.iota : new PatternIota(work.pattern));
        }
        return new ContinuationIota(pending);
    }

    /** Replace the active work queue with a previously captured continuation. */
    public void invokeContinuation(ContinuationIota value) throws CastingException {
        if (value == null) {
            throw Mishap.invalidValue("hexcasting.error.continuation_invalid",
                "Cannot invoke a null continuation");
        }
        continuation.clear();
        for (Iota pending : value.getContinuation()) {
            if (pending == null) {
                throw Mishap.invalidValue("hexcasting.error.continuation_invalid",
                    "Continuation contains a null Iota");
            }
            continuation.addLast(WorkItem.iota(pending));
        }
        continuationInvoked = true;
    }

    /** Evaluate one supported meta-evaluation target in the active VM. */
    public CastingStack runNestedIota(Iota target) throws CastingException {
        if (target == null) {
            throw Mishap.invalidValue("hexcasting.error.invalid_iota",
                "Cannot evaluate a null Iota");
        }
        if (target instanceof ListIota) {
            return runNestedIotas(((ListIota) target).getItems());
        }
        if (target instanceof PatternIota) {
            return runNested(Collections.singletonList(((PatternIota) target).getPattern()));
        }
        if (target instanceof ContinuationIota) {
            invokeContinuation((ContinuationIota) target);
            return stack;
        }
        throw Mishap.invalidValue("hexcasting.error.invalid_iota",
            "Cannot evaluate Iota of type " + target.getType().getId());
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
        return parenCount;
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

    /**
     * Reset all transient escape state at a Thoth/meta-evaluation boundary.
     *
     * <p>Hex resets the open-parenthesis capture together with the escape
     * flag between {@code for_each} iterations. Keeping only the flag reset
     * leaks an unfinished body into the next iteration.</p>
     */
    public void resetMetaState() {
        escapeNext = false;
        parenCount = 0;
        parentheses.clear();
    }

    public void openParen() {
        if (parenCount == 0 || parentheses.isEmpty()) {
            parentheses.clear();
            parentheses.push(new ParenFrame());
        } else {
            // Nested open_paren is itself part of the parenthesized program.
            parentheses.peek().values.add(new ParenEntry(
                new PatternIota(HexActions.OPEN_PAREN_PATTERN), false));
        }
        parenCount++;
    }

    public void openParens(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Parenthesis count cannot be negative");
        }
        if (count == 0) {
            return;
        }
        if (parenCount == 0 || parentheses.isEmpty()) {
            // open_n_parens changes the counter as one operation. It must not
            // synthesize nested open_paren PatternIotas in the captured code.
            parentheses.clear();
            parentheses.push(new ParenFrame());
        }
        // open_n_parens sets the depth to the value read from the stack; it
        // does not add another set of layers to a capture already in flight.
        parenCount = count;
    }

    public void closeParen() throws CastingException {
        if (parenCount <= 0 || parentheses.isEmpty()) {
            throw Mishap.invalidValue("hexcasting.mishap.needs_parens",
                "Cannot close a parenthesis when none is open");
        }
        parenCount--;
        if (parenCount == 0) {
            ParenFrame frame = parentheses.pop();
            ArrayList<Iota> values = new ArrayList<>(frame.values.size());
            for (ParenEntry entry : frame.values) {
                values.add(entry.value);
            }
            stack.push(new ListIota(values));
            parentheses.clear();
        } else {
            // A close inside a larger parenthesized program is retained as code.
            parentheses.peek().values.add(new ParenEntry(
                new PatternIota(HexActions.CLOSE_PAREN_PATTERN), false));
        }
    }

    public void closeAllParens() throws CastingException {
        if (parenCount <= 0 || parentheses.isEmpty()) {
            throw Mishap.invalidValue("hexcasting.mishap.needs_parens",
                "Cannot close parentheses when none is open");
        }
        ParenFrame frame = parentheses.peek();
        ArrayList<Iota> values = new ArrayList<>(frame.values.size());
        for (ParenEntry entry : frame.values) {
            values.add(entry.value);
        }
        stack.push(new DoubleIota(parenCount));
        stack.push(new ListIota(values));
        parenCount = 0;
        parentheses.clear();
    }

    /** Read the off-hand data holder into the currently captured list. */
    public void readIntoParen() throws CastingException {
        if (parenCount <= 0 || parentheses.isEmpty()) {
            throw Mishap.invalidValue("hexcasting.mishap.needs_parens",
                "Cannot read into parentheses when none is open");
        }
        if (player == null) {
            throw Mishap.legacy("hexcasting.error.read_context");
        }
        ItemStack holder = getHeldItemToOperateOn(IotaDataHolder::canRead);
        if (holder == null || holder.isEmpty()) {
            throw Mishap.legacy("hexcasting.error.data_holder_missing");
        }
        Iota datum = IotaDataHolder.read(holder);
        parentheses.peek().values.add(new ParenEntry(datum, true));
    }

    /** Undo the latest captured value, or the current empty parenthesis frame. */
    public void undo() throws CastingException {
        if (parenCount <= 0 || parentheses.isEmpty()) {
            throw Mishap.invalidValue("hexcasting.mishap.needs_parens",
                "Undo requires an open parenthesis");
        }
        ParenFrame frame = parentheses.peek();
        if (frame.values.isEmpty()) {
            // Undoing the initial empty capture cancels all opens, including
            // opens created by open_n_parens.
            parenCount = 0;
            parentheses.clear();
        } else {
            ParenEntry last = frame.values.remove(frame.values.size() - 1);
            if (last.value instanceof PatternIota && !last.escaped) {
                HexPattern pattern = ((PatternIota) last.value).getPattern();
                if (HexActions.OPEN_PAREN_PATTERN.equals(pattern)) {
                    parenCount--;
                } else if (HexActions.CLOSE_PAREN_PATTERN.equals(pattern)) {
                    parenCount++;
                }
            }
            if (parenCount <= 0) {
                parenCount = 0;
                parentheses.clear();
            }
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
        out.setInteger("parenCount", parenCount);
        out.setBoolean("escapeNext", escapeNext);
        out.setBoolean("halted", halted);
        out.setTag("userData", userData.copy());
        NBTTagList parenthesisTags = new NBTTagList();
        for (ParenFrame frame : parentheses) {
            NBTTagCompound frameTag = new NBTTagCompound();
            NBTTagList values = new NBTTagList();
            NBTTagList escaped = new NBTTagList();
            for (ParenEntry entry : frame.values) {
                values.appendTag(entry.value.serialize());
                escaped.appendTag(new NBTTagByte(entry.escaped ? (byte) 1 : (byte) 0));
            }
            frameTag.setTag("values", values);
            frameTag.setTag("escaped", escaped);
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
            throw Mishap.invalidValue("hexcasting.error.vm_state_invalid",
                "Missing casting VM stack state");
        }
        CastingVM vm = new CastingVM(
            CastingStack.deserializeState(serialized.getCompoundTag("stack")));
        vm.operationsConsumed = Math.max(0, serialized.getInteger("operationsConsumed"));
        vm.parenCount = Math.max(0, serialized.getInteger("parenCount"));
        vm.escapeNext = serialized.getBoolean("escapeNext");
        vm.halted = serialized.getBoolean("halted");
        if (serialized.hasKey("userData", 10)) {
            vm.userData = serialized.getCompoundTag("userData").copy();
        }
        if (serialized.hasKey("parentheses", 9)) {
            NBTTagList parenthesisTags = serialized.getTagList("parentheses", 10);
            if (parenthesisTags.tagCount() > Iota.MAX_SERIALIZATION_TOTAL) {
                throw Mishap.invalidValue("hexcasting.error.vm_state_limit",
                    "Serialized parenthesis state exceeded its size limit");
            }
            ParenFrame frame = new ParenFrame();
            // Older snapshots stored one frame per nesting level. Flatten
            // those frames from outer to inner when loading them into the
            // current Hex-compatible representation.
            for (int i = parenthesisTags.tagCount() - 1; i >= 0; i--) {
                NBTTagCompound frameTag = parenthesisTags.getCompoundTagAt(i);
                NBTTagList values = frameTag.getTagList("values", 10);
                NBTTagList escaped = frameTag.getTagList("escaped", 1);
                for (int j = 0; j < values.tagCount(); j++) {
                    boolean isEscaped = false;
                    if (j < escaped.tagCount()) {
                        NBTBase escapedTag = escaped.get(j);
                        isEscaped = escapedTag instanceof NBTTagByte
                            && ((NBTTagByte) escapedTag).getByte() != 0;
                    }
                    frame.values.add(new ParenEntry(
                        HexIotaTypes.deserialize(values.getCompoundTagAt(j)), isEscaped));
                }
            }
            if (vm.parenCount == 0 && parenthesisTags.tagCount() > 0) {
                vm.parenCount = parenthesisTags.tagCount();
            }
            if (vm.parenCount > 0) {
                vm.parentheses.push(frame);
            }
        }
        if (!serialized.hasKey("continuation", 9)) {
            return vm;
        }
        NBTTagList pending = serialized.getTagList("continuation", 10);
        if (pending.tagCount() > Iota.MAX_SERIALIZATION_TOTAL) {
            throw Mishap.invalidValue("hexcasting.error.vm_state_limit",
                "Serialized casting continuation exceeded its size limit");
        }
        for (int i = 0; i < pending.tagCount(); i++) {
            NBTTagCompound entry = pending.getCompoundTagAt(i);
            if (!entry.hasKey("iota", 10)) {
                throw Mishap.invalidValue("hexcasting.error.vm_state_invalid",
                    "Serialized casting continuation entry is missing its Iota");
            }
            Iota value = HexIotaTypes.deserialize(entry.getCompoundTag("iota"));
            if ("pattern".equals(entry.getString("kind"))) {
                if (!(value instanceof PatternIota)) {
                    throw Mishap.invalidValue("hexcasting.error.vm_state_invalid",
                        "Serialized pattern continuation entry is not a PatternIota");
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
            throw Mishap.evaluationLimit(maxOperations);
        }

        WorkItem work = continuation.removeFirst();
        int stackSizeBefore = stack.size();
        HexPattern pattern = work.pattern;
        if (pattern == null && work.iota instanceof PatternIota) {
            pattern = ((PatternIota) work.iota).getPattern();
        }
        HexAction action = pattern == null ? null : HexActionRegistry.get(
            pattern, player == null ? null : player.world);
        SpecialPatternResolver.Match special = action == null
            ? SpecialPatternResolver.match(pattern) : null;
        ResourceLocation actionId = action == null
            ? null : HexActionRegistry.idFor(action);
        if (pattern != null && action == null && special == null
            && parenCount == 0 && !escapeNext) {
            recordSound(HexEvalSounds.MISHAP);
            Mishap mishap = Mishap.invalidPattern(pattern, player, parenCount,
                operationsConsumed);
            lastMishap = mishap;
            if (evaluationDepth == 0) {
                mishap.applyStackEffect(stack);
            }
            throw mishap;
        }
        Iota value = pattern == null ? work.iota : new PatternIota(pattern);

        // Count before execution so a failing action cannot be retried
        // indefinitely by a caller resuming the VM. Nested actions inherit
        // this exact budget instead of silently falling back to 1024.
        operationsConsumed++;
        int previousLimit = activeOperationLimit;
        activeOperationLimit = maxOperations;
        clearMishapContext();
        try {
            if (escapeNext) {
                escapeNext = false;
                capture(value, true);
            } else if (parenCount > 0 && (action == null || !action.executesInParentheses())) {
                parentheses.peek().values.add(new ParenEntry(value, false));
            } else if (work.iota instanceof ContinuationIota) {
                invokeContinuation((ContinuationIota) work.iota);
            } else if (action != null) {
                action.execute(stack, this);
            } else if (special != null) {
                special.execute(stack);
            } else {
                stack.push(value);
            }
            recordSound(HexEvalSounds.forAction(action, actionId));
        } catch (CastingException exception) {
            unlockOvercastForFailedGreatSpell(actionId);
            recordSound(HexEvalSounds.MISHAP);
            Mishap mishap = Mishap.from(exception, pattern, actionId, player,
                parenCount, operationsConsumed)
                .withExecutionContext(pattern, actionId, player,
                    parenCount, operationsConsumed);
            normalizeManualUnderflow(mishap, stackSizeBefore);
            mishap = attachMishapContext(mishap);
            lastMishap = mishap;
            if (evaluationDepth == 0) {
                mishap.applyStackEffect(stack);
            }
            // Nested evaluations must defer gameplay effects until the outer
            // VM has restored its snapshot and media transaction.
            if (evaluationDepth == 0) {
                MishapFeedback.applySideEffects(mishap);
            }
            throw mishap;
        } catch (RuntimeException exception) {
            unlockOvercastForFailedGreatSpell(actionId);
            recordSound(HexEvalSounds.MISHAP);
            Mishap mishap = Mishap.fromRuntime(exception, pattern, actionId,
                player, parenCount, operationsConsumed)
                .withExecutionContext(pattern, actionId, player,
                    parenCount, operationsConsumed);
            mishap = attachMishapContext(mishap);
            lastMishap = mishap;
            if (evaluationDepth == 0) {
                mishap.applyStackEffect(stack);
            }
            if (evaluationDepth == 0) {
                MishapFeedback.applySideEffects(mishap);
            }
            throw mishap;
        } finally {
            activeOperationLimit = previousLimit;
        }
        return true;
    }

    /**
     * Actions written as a sequence of plain pop() calls do not know their
     * arity when the final pop reaches an empty stack. Recover that arity from
     * the values removed by the failed action.
     */
    private void normalizeManualUnderflow(Mishap mishap, int stackSizeBefore) {
        if (mishap == null || mishap.getKind() != Mishap.Kind.NOT_ENOUGH_ARGUMENTS
            || mishap.getArgumentsExpected() != 1
            || stackSizeBefore <= mishap.getArgumentsGot()) {
            return;
        }
        int removed = Math.max(0, stackSizeBefore - stack.size());
        if (removed > 0) {
            mishap.withArguments(removed + 1, stackSizeBefore);
        }
    }

    /** Drain all pending work using the default operation budget. */
    /** Consume persistent player media for a contextual spell action. */
    public void consumeMedia(long amount) throws CastingException {
        if (amount <= 0L) {
            return;
        }
        if (mediaConsumptionBypassed) {
            return;
        }
        if (mediaHolder == null && player == null && castingData == null) {
            throw Mishap.legacy("hexcasting.error.no_media_context");
        }
        if (mediaTransaction == null) {
            mediaTransaction = MediaInventoryHelper.begin(player, castingData, mediaHolder,
                allowMediaInventoryFallback);
        }
        long extracted = mediaTransaction.consumeUpTo(amount);
        long generated = extracted < amount && canOvercast()
            ? OvercastHelper.consume(player, amount - extracted) : 0L;
        if (extracted + generated < amount) {
            mediaTransaction.rollback();
            throw Mishap.notEnoughMedia(amount, extracted + generated);
        }
    }

    public CastingStack run() throws CastingException {
        return run(activeOperationLimit);
    }

    /** Drain all pending work, failing deterministically if the budget is hit. */
    public CastingStack run(int maxOperations) throws CastingException {
        validateBudget(maxOperations);
        boolean outermost = evaluationDepth == 0;
        VmSnapshot before = null;
        if (outermost) {
            before = snapshotState();
            rollbackActions.clear();
            mediaTransaction = MediaInventoryHelper.begin(player, castingData, mediaHolder,
                allowMediaInventoryFallback);
            lastMishap = null;
        }
        evaluationDepth++;
        try {
            while (hasPendingWork()) {
                step(maxOperations);
            }
            if (outermost && mediaTransaction != null) {
                mediaTransaction.commit();
                rollbackActions.clear();
            }
            return stack;
        } catch (CastingException exception) {
            if (outermost) {
                rollbackEvaluation(before);
            }
            if (exception instanceof Mishap) {
                Mishap mishap = (Mishap) exception;
                try {
                    mishap.applyStackEffect(stack);
                } catch (CastingException ignored) {
                    // A malformed stack effect must not hide the original Mishap.
                }
                MishapFeedback.applySideEffects(mishap);
            }
            throw exception;
        } catch (RuntimeException exception) {
            if (outermost) {
                rollbackEvaluation(before);
            }
            Mishap mishap = Mishap.fromRuntime(exception, null, null, player,
                parenCount, operationsConsumed)
                .withExecutionContext(null, null, player,
                    parenCount, operationsConsumed);
            lastMishap = mishap;
            MishapFeedback.applySideEffects(mishap);
            throw mishap;
        } finally {
            evaluationDepth--;
            if (outermost) {
                mediaTransaction = null;
            }
            // Halt is scoped to this evaluation and must not permanently
            // disable a staff when its state is saved afterward.
            halted = false;
        }
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
        boolean previousContinuationInvoked = continuationInvoked;
        halted = false;
        lastNestedRunHalted = false;
        continuationInvoked = false;
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
            boolean invoked = continuationInvoked;
            halted = previousHalted;
            if (!invoked) {
                continuation.addAll(outerContinuation);
            }
            // Propagate a continuation jump through every synchronous nested
            // evaluator. Otherwise a parent runNestedIotas call restores its
            // stale queue after an inner eval/cc has already resumed the
            // captured continuation.
            continuationInvoked = previousContinuationInvoked || invoked;
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
        boolean previousContinuationInvoked = continuationInvoked;
        halted = false;
        lastNestedRunHalted = false;
        continuationInvoked = false;
        continuation.clear();
        try {
            enqueueIotas(iotas);
            while (!continuation.isEmpty()) {
                step(maxOperations);
            }
        } finally {
            lastNestedRunHalted = halted;
            boolean invoked = continuationInvoked;
            halted = previousHalted;
            if (!invoked) {
                continuation.addAll(outerContinuation);
            }
            // Preserve the jump signal for an enclosing nested evaluator.
            continuationInvoked = previousContinuationInvoked || invoked;
        }
        return stack;
    }

    private VmSnapshot snapshotState() throws CastingException {
        ArrayDeque<ParenFrame> parenthesisCopy = new ArrayDeque<>();
        for (ParenFrame frame : parentheses) {
            ParenFrame frameCopy = new ParenFrame();
            for (ParenEntry entry : frame.values) {
                frameCopy.values.add(new ParenEntry(entry.value, entry.escaped));
            }
            parenthesisCopy.addLast(frameCopy);
        }
        return new VmSnapshot(
            CastingStack.deserializeState(stack.serializeState()),
            new ArrayDeque<>(continuation), parenthesisCopy, parenCount,
            escapeNext, halted, continuationInvoked, lastNestedRunHalted,
            operationsConsumed, userData.copy());
    }

    private void rollbackEvaluation(VmSnapshot before) {
        for (int i = rollbackActions.size() - 1; i >= 0; i--) {
            try {
                rollbackActions.get(i).run();
            } catch (RuntimeException ignored) {
                // A failed cleanup must never hide the original Mishap.
            }
        }
        rollbackActions.clear();
        if (mediaTransaction != null) {
            mediaTransaction.rollback();
        }
        if (before != null) {
            try {
                stack.restore(before.stack.snapshot());
                stack.writeLocal(before.stack.readLocal());
            } catch (CastingException ignored) {
                // The snapshot came from this stack, so this is only a
                // defensive guard for malformed third-party Iotas.
            }
            continuation.clear();
            continuation.addAll(before.continuation);
            parentheses.clear();
            parentheses.addAll(before.parentheses);
            parenCount = before.parenCount;
            escapeNext = before.escapeNext;
            halted = before.halted;
            continuationInvoked = before.continuationInvoked;
            lastNestedRunHalted = before.lastNestedRunHalted;
            userData = before.userData.copy();
            operationsConsumed = before.operationsConsumed;
        }
    }

    private void capture(Iota value, boolean escaped) throws CastingException {
        if (parenCount <= 0 || parentheses.isEmpty()) {
            stack.push(value);
        } else {
            parentheses.peek().values.add(new ParenEntry(value, escaped));
        }
    }

    private static void validateBudget(int maxOperations) {
        if (maxOperations <= 0) {
            throw new IllegalArgumentException("Operation limit must be positive");
        }
    }

    private boolean canOvercast() {
        return player != null && !mediaConsumptionBypassed
            && (mediaHolder == null || allowMediaInventoryFallback)
            && OvercastHelper.canOvercast(player);
    }

    private void unlockOvercastForFailedGreatSpell(ResourceLocation actionId) {
        if (player != null && PerWorldPatternData.isPerWorldAction(actionId)) {
            OvercastHelper.unlock(player);
        }
    }

    private static long saturatingAdd(long left, long right) {
        if (right <= 0L) {
            return Math.max(0L, left);
        }
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }
}
