package at.petra_k.hexcasting.api.casting.eval;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.api.casting.iota.GarbageIota;
import at.petra_k.hexcasting.api.casting.iota.ContinuationIota;
import at.petra_k.hexcasting.api.casting.iota.EntityIota;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.ListIota;
import at.petra_k.hexcasting.api.casting.iota.PatternIota;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Structured casting failure used by the 1.12.2 evaluator.
 *
 * <p>The modern mod exposes a family of Mishap classes.  The legacy port has
 * one checked exception API for compatibility with its older actions, so this
 * class carries the same important information without forcing every action
 * to be rewritten at once: a stable category, the original translation key,
 * the pattern/action that failed, and the active caster context.</p>
 */
public class Mishap extends CastingException {
    public enum Kind {
        INVALID_PATTERN,
        NOT_ENOUGH_MEDIA,
        NOT_ENOUGH_ARGUMENTS,
        BAD_ENTITY,
        BAD_ITEM,
        BAD_BLOCK,
        BAD_BRAINSWEEP,
        ALREADY_BRAINSWEPT,
        OTHERS_NAME,
        NO_AKASHIC_RECORD,
        BAD_LOCATION,
        WRONG_DIMENSION,
        PERMISSION_DENIED,
        INVALID_VALUE,
        ARITHMETIC,
        EVALUATION_LIMIT,
        STACK_SIZE,
        INVALID_CONTEXT,
        INTERNAL,
        UNKNOWN
    }

    /** Stack mutation performed after the VM has restored the failed cast. */
    private enum StackEffect {
        NONE,
        PUSH_GARBAGE,
        REPLACE_WITH_GARBAGE,
        REPLACE_MANY_WITH_GARBAGE,
        CLEAR_AND_PUSH_GARBAGE,
        PUSH_PATTERN
    }

    private final Kind kind;
    private final String errorKey;
    private HexPattern pattern;
    private ResourceLocation actionId;
    private EntityPlayer caster;
    private int parenthesisDepth;
    private int operation;
    private final String detail;
    private String casterUuid;
    private String casterName;
    private int casterDimension;
    private double casterX;
    private double casterY;
    private double casterZ;
    private double targetDistance = Double.NaN;
    private int targetDimension = Integer.MIN_VALUE;
    private String targetUuid;
    /** Runtime target retained for the mishap side-effect phase. */
    private Entity targetEntity;
    private boolean locationRecorded;
    private double locationX = Double.NaN;
    private double locationY = Double.NaN;
    private double locationZ = Double.NaN;
    private int locationDimension = Integer.MIN_VALUE;
    private boolean permissionChecked;
    private boolean permissionAllowed = true;
    private long mediaRequired = -1L;
    private long mediaAvailable = -1L;
    private int argumentsExpected = -1;
    private int argumentsGot = -1;
    private boolean sideEffectsApplied;
    private StackEffect stackEffect = StackEffect.NONE;
    private int stackEffectCount;
    private int stackEffectReverseIndex;
    private HexPattern stackEffectPattern;
    private boolean stackEffectApplied;
    private String invalidExpected;
    private Iota invalidPerpetrator;
    private Object[] invalidExpectedArgs = new Object[0];
    private List<Iota> invalidOperatorPerpetrators = Collections.emptyList();
    private Iota arithmeticLeft;
    private Iota arithmeticRight;
    private String arithmeticSuffix;
    private Object[] displayArgs = new Object[0];
    private boolean executionContextAttached;

    public Mishap(Kind kind, String errorKey, Throwable cause, HexPattern pattern,
                  ResourceLocation actionId, EntityPlayer caster,
                  int parenthesisDepth, int operation) {
        this(kind, errorKey, cause, pattern, actionId, caster,
            parenthesisDepth, operation, null);
    }

    private Mishap(Kind kind, String errorKey, Throwable cause, HexPattern pattern,
                   ResourceLocation actionId, EntityPlayer caster,
                   int parenthesisDepth, int operation, String detail) {
        super(errorKey == null || errorKey.isEmpty() ? "hexcasting.error.unknown" : errorKey,
            cause);
        this.kind = kind == null ? Kind.UNKNOWN : kind;
        this.errorKey = errorKey == null || errorKey.isEmpty()
            ? "hexcasting.error.unknown" : errorKey;
        this.pattern = pattern;
        this.actionId = actionId;
        this.caster = caster;
        this.parenthesisDepth = Math.max(0, parenthesisDepth);
        this.operation = Math.max(0, operation);
        this.detail = detail;
        this.casterUuid = caster == null || caster.getUniqueID() == null
            ? null : caster.getUniqueID().toString();
        this.casterName = caster == null ? null : caster.getName();
        this.casterDimension = caster == null ? Integer.MIN_VALUE : caster.dimension;
        this.casterX = caster == null ? Double.NaN : caster.posX;
        this.casterY = caster == null ? Double.NaN : caster.posY;
        this.casterZ = caster == null ? Double.NaN : caster.posZ;
        this.executionContextAttached = pattern != null || actionId != null || caster != null;
    }

    public Kind getKind() {
        return kind;
    }

    /** The original lang key emitted by the action, when one was available. */
    public String getErrorKey() {
        return errorKey;
    }

    public HexPattern getPattern() {
        return pattern;
    }

    public ResourceLocation getActionId() {
        return actionId;
    }

    public EntityPlayer getCaster() {
        return caster;
    }

    public int getParenthesisDepth() {
        return parenthesisDepth;
    }

    public int getOperation() {
        return operation;
    }

    /** Human-readable detail retained separately from the stable error key. */
    public String getDetail() {
        return detail;
    }

    /** Arguments for a display key that needs formatted localization. */
    public Object[] getDisplayArgs() {
        return displayArgs.clone();
    }

    /** The registered action path, without the mod namespace. */
    public String getActionName() {
        return actionId == null ? null : actionId.toString();
    }

    /** Translation key for the action name shown before the mishap text. */
    public String getActionDisplayKey() {
        return actionId == null ? null
            : "hexcasting.action." + actionId.getResourcePath();
    }

    public String getCasterUuid() {
        return casterUuid;
    }

    public String getCasterName() {
        return casterName;
    }

    public int getCasterDimension() {
        return casterDimension;
    }

    public double getCasterX() {
        return casterX;
    }

    public double getCasterY() {
        return casterY;
    }

    public double getCasterZ() {
        return casterZ;
    }

    public boolean hasTargetContext() {
        return !Double.isNaN(targetDistance);
    }

    public double getTargetDistance() {
        return targetDistance;
    }

    public int getTargetDimension() {
        return targetDimension;
    }

    public String getTargetUuid() {
        return targetUuid;
    }

    /** The live target, when the action resolved one before failing. */
    public Entity getTargetEntity() {
        return targetEntity;
    }

    public boolean hasLocationContext() {
        return locationRecorded;
    }

    public double getLocationX() {
        return locationX;
    }

    public double getLocationY() {
        return locationY;
    }

    public double getLocationZ() {
        return locationZ;
    }

    public int getLocationDimension() {
        return locationDimension;
    }

    public boolean isPermissionChecked() {
        return permissionChecked;
    }

    public boolean isPermissionAllowed() {
        return permissionAllowed;
    }

    public boolean hasMediaContext() {
        return mediaRequired >= 0L || mediaAvailable >= 0L;
    }

    public long getMediaRequired() {
        return mediaRequired;
    }

    public long getMediaAvailable() {
        return mediaAvailable;
    }

    /** Expected argument count for a stack-underflow mishap, when known. */
    public int getArgumentsExpected() {
        return argumentsExpected;
    }

    /** Actual stack height for a stack-underflow mishap, when known. */
    public int getArgumentsGot() {
        return argumentsGot;
    }

    /** Replace the argument counts after the VM has recovered a manual pop chain. */
    public Mishap withArguments(int expected, int got) {
        argumentsExpected = Math.max(0, expected);
        argumentsGot = Math.max(0, got);
        stackEffect = StackEffect.PUSH_GARBAGE;
        stackEffectCount = Math.max(0, argumentsExpected - argumentsGot);
        return this;
    }

    /** Whether this Mishap has a modern Hex stack-resolution effect. */
    public boolean hasStackEffect() {
        return stackEffect != StackEffect.NONE;
    }

    /** Expected type key used by the structured invalid-Iota message. */
    public String getInvalidExpected() {
        return invalidExpected;
    }

    /** Optional format arguments for a structured invalid-Iota expectation. */
    public Object[] getInvalidExpectedArgs() {
        return invalidExpectedArgs.clone();
    }

    /** The value that failed an invalid-Iota predicate. */
    public Iota getInvalidPerpetrator() {
        return invalidPerpetrator;
    }

    /** Stack index counted from the top for an invalid-Iota replacement. */
    public int getInvalidReverseIndex() {
        return stackEffectReverseIndex;
    }

    /** The arguments rejected by an overloaded arithmetic operator. */
    public List<Iota> getInvalidOperatorPerpetrators() {
        return invalidOperatorPerpetrators;
    }

    /** Left and right operands retained for a divide-by-zero Mishap. */
    public Iota getArithmeticLeft() {
        return arithmeticLeft;
    }

    public Iota getArithmeticRight() {
        return arithmeticRight;
    }

    /** Modern divide-by-zero suffix, such as divide, exponent, or logarithm. */
    public String getArithmeticSuffix() {
        return arithmeticSuffix;
    }

    /**
     * Apply the stack portion of this Mishap once.  The caller must invoke it
     * after restoring the VM snapshot, matching the modern side-effect phase.
     */
    public void applyStackEffect(CastingStack stack) throws CastingException {
        if (stack == null || stackEffectApplied || stackEffect == StackEffect.NONE) {
            return;
        }
        switch (stackEffect) {
            case PUSH_GARBAGE:
                stack.pushGarbage(stackEffectCount);
                break;
            case REPLACE_WITH_GARBAGE:
                stack.replaceFromTop(stackEffectReverseIndex, new GarbageIota());
                break;
            case REPLACE_MANY_WITH_GARBAGE:
                for (int i = 0; i < stackEffectCount; i++) {
                    stack.replaceFromTop(i, new GarbageIota());
                }
                break;
            case CLEAR_AND_PUSH_GARBAGE:
                stack.clearAndPushGarbage();
                break;
            case PUSH_PATTERN:
                if (stackEffectPattern != null) {
                    stack.push(new PatternIota(stackEffectPattern));
                }
                break;
            default:
                break;
        }
        stackEffectApplied = true;
    }

    /** Attach execution data to a Mishap created before the VM knew the action. */
    public Mishap withExecutionContext(HexPattern pattern, ResourceLocation actionId,
                                      EntityPlayer caster, int parenthesisDepth,
                                      int operation) {
        if (executionContextAttached) {
            return this;
        }
        if (pattern != null) {
            this.pattern = pattern;
        }
        if (actionId != null) {
            this.actionId = actionId;
        }
        if (caster != null) {
            this.caster = caster;
            this.casterUuid = caster.getUniqueID() == null
                ? null : caster.getUniqueID().toString();
            this.casterName = caster.getName();
            this.casterDimension = caster.dimension;
            this.casterX = caster.posX;
            this.casterY = caster.posY;
            this.casterZ = caster.posZ;
        }
        if (targetEntity != null && caster != null) {
            targetDistance = caster.getDistance(targetEntity);
        }
        this.parenthesisDepth = Math.max(0, parenthesisDepth);
        this.operation = Math.max(0, operation);
        this.executionContextAttached = true;
        if ("hexcasting.mishap.needs_parens".equals(errorKey)
            && pattern != null) {
            stackEffect = StackEffect.PUSH_PATTERN;
            stackEffectPattern = pattern;
        }
        return this;
    }

    /** Record the amount involved in a media-shortage Mishap. */
    public Mishap withMedia(long required, long available) {
        mediaRequired = Math.max(0L, required);
        mediaAvailable = Math.max(0L, available);
        return this;
    }

    /** Attach target data without losing the original exception context. */
    public Mishap withTarget(Entity target) {
        if (target == null || targetEntity != null) {
            return this;
        }
        targetDistance = caster == null ? Double.NaN : caster.getDistance(target);
        targetDimension = target.dimension;
        targetUuid = target.getUniqueID() == null ? null : target.getUniqueID().toString();
        targetEntity = target;
        return this;
    }

    /** Attach the world position that an action was validating or editing. */
    public Mishap withLocation(double x, double y, double z, int dimension) {
        if (locationRecorded || Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
            return this;
        }
        locationRecorded = true;
        locationX = x;
        locationY = y;
        locationZ = z;
        locationDimension = dimension;
        return this;
    }

    /** Record whether a world/permission check was performed for this mishap. */
    public Mishap withPermission(boolean checked, boolean allowed) {
        if (permissionChecked) {
            return this;
        }
        permissionChecked = checked;
        permissionAllowed = allowed;
        return this;
    }

    /**
     * Mark the gameplay side-effect phase as complete.  Mishaps can cross
     * several legacy entry points (VM, item, and feedback), so this small
     * guard prevents a bad-entity throw or brainsweep damage from happening
     * twice while still allowing each entry point to call the common helper.
     */
    public boolean beginSideEffects() {
        if (sideEffectsApplied) {
            return false;
        }
        sideEffectsApplied = true;
        return true;
    }

    /** Stable accent color used by common mishap feedback. */
    public int getAccentColor() {
        switch (kind) {
            case INVALID_PATTERN:
                return 0xE5C84B;
            case NOT_ENOUGH_ARGUMENTS:
                return 0xD8D8D8;
            case BAD_ITEM:
                return 0xA06B3C;
            case BAD_BLOCK:
                return 0x86D65A;
            case BAD_ENTITY:
                return 0x78A8E8;
            case BAD_BRAINSWEEP:
            case ALREADY_BRAINSWEPT:
                return 0x62B64A;
            case OTHERS_NAME:
                return 0x202020;
            case INVALID_CONTEXT:
                return 0xD87F33;
            case NO_AKASHIC_RECORD:
                return 0x8B5CC7;
            case BAD_LOCATION:
            case WRONG_DIMENSION:
                return 0xE97AC1;
            case PERMISSION_DENIED:
                return 0x303030;
            case NOT_ENOUGH_MEDIA:
                return 0xE05252;
            case ARITHMETIC:
                return 0xE05252;
            case EVALUATION_LIMIT:
                return 0x5C86D6;
            case STACK_SIZE:
                return 0x202020;
            default:
                return 0xB04040;
        }
    }

    /** Return a stable translation key for UI and logs. */
    public String getDisplayKey() {
        if (errorKey.startsWith("hexcasting.")) {
            return errorKey;
        }
        switch (kind) {
            case INVALID_PATTERN:
                return "hexcasting.mishap.invalid_pattern_generic";
            case NOT_ENOUGH_MEDIA:
                return "hexcasting.error.not_enough_media";
            case NOT_ENOUGH_ARGUMENTS:
                return "hexcasting.error.stack_underflow";
            case PERMISSION_DENIED:
                return "hexcasting.error.permission_denied";
            default:
                return "hexcasting.error.unknown";
        }
    }

    /** Convert an old string exception into a typed Mishap. */
    public static Mishap from(CastingException exception, HexPattern pattern,
                              net.minecraft.util.ResourceLocation actionId,
                              EntityPlayer caster,
                              int parenthesisDepth, int operation) {
        if (exception instanceof Mishap) {
            return (Mishap) exception;
        }
        String message = exception == null ? null : exception.getMessage();
        Mishap mishap = new Mishap(classify(message), message, exception, pattern,
            actionId, caster,
            parenthesisDepth, operation);
        mishap.inferPermissionFailure(message);
        return mishap;
    }

    /** Convert an unexpected action exception into an internal Mishap. */
    public static Mishap fromRuntime(RuntimeException exception, HexPattern pattern,
                                     net.minecraft.util.ResourceLocation actionId,
                                     EntityPlayer caster,
                                     int parenthesisDepth, int operation) {
        String detail = exception == null ? "unknown" : exception.getClass().getSimpleName();
        return new Mishap(Kind.INTERNAL, "hexcasting.error.unknown", exception, pattern,
            actionId, caster,
            parenthesisDepth, operation, detail);
    }

    /** Construct the common media-shortage Mishap with quantitative context. */
    public static Mishap notEnoughMedia(long required, long available) {
        return new Mishap(Kind.NOT_ENOUGH_MEDIA,
            "hexcasting.error.not_enough_media", null, null, null, null,
            0, 0, "required=" + Math.max(0L, required)
                + ", available=" + Math.max(0L, available))
            .withMedia(required, available);
    }

    /** Construct the true-name Mishap used by permanent Iota storage. */
    public static Mishap othersName(EntityPlayer confidant) {
        Mishap mishap = new Mishap(Kind.OTHERS_NAME,
            "hexcasting.mishap.others_name", null, null, null, null,
            0, 0, null);
        mishap.withTarget(confidant);
        return mishap;
    }

    /**
     * Find a player reference anywhere in an Iota tree.  Lists and
     * continuations are the two recursive containers in the 1.12 port; using
     * a work queue keeps deeply nested spell lists from overflowing the Java
     * call stack and mirrors modern true-name traversal.
     */
    public static EntityPlayer findOtherPlayer(Iota value, EntityPlayer caster) {
        if (value == null) {
            return null;
        }
        ArrayDeque<Iota> pending = new ArrayDeque<>();
        pending.add(value);
        int visited = 0;
        while (!pending.isEmpty() && visited++ < Iota.MAX_SERIALIZATION_TOTAL) {
            Iota current = pending.removeFirst();
            if (current instanceof EntityIota) {
                Entity entity = ((EntityIota) current).getEntity();
                if (entity instanceof EntityPlayer && entity != caster) {
                    return (EntityPlayer) entity;
                }
            } else if (current instanceof ListIota) {
                pending.addAll(((ListIota) current).getItems());
            } else if (current instanceof ContinuationIota) {
                pending.addAll(((ContinuationIota) current).getContinuation());
            }
        }
        return null;
    }

    /** Find the first other-player reference in an ordered collection. */
    public static EntityPlayer findOtherPlayer(List<? extends Iota> values,
                                               EntityPlayer caster) {
        if (values == null) {
            return null;
        }
        for (Iota value : values) {
            EntityPlayer found = findOtherPlayer(value, caster);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** Construct the typed equivalent of modern MishapNotEnoughArgs. */
    public static Mishap notEnoughArguments(int expected, int got) {
        int normalizedExpected = Math.max(0, expected);
        int normalizedGot = Math.max(0, got);
        String key = normalizedGot == 0
            ? "hexcasting.mishap.no_args"
            : "hexcasting.mishap.not_enough_args";
        Mishap mishap = new Mishap(Kind.NOT_ENOUGH_ARGUMENTS, key, null,
            null, null, null, 0, 0,
            "expected=" + normalizedExpected + ", got=" + normalizedGot);
        mishap.argumentsExpected = normalizedExpected;
        mishap.argumentsGot = normalizedGot;
        mishap.stackEffect = StackEffect.PUSH_GARBAGE;
        mishap.stackEffectCount = Math.max(0, normalizedExpected - normalizedGot);
        return mishap;
    }

    /** Construct the black-spark stack-size mishap used by the modern VM. */
    public static Mishap stackSize() {
        Mishap mishap = new Mishap(Kind.STACK_SIZE, "hexcasting.mishap.stack_size",
            null, null, null, null, 0, 0, null);
        mishap.stackEffect = StackEffect.CLEAR_AND_PUSH_GARBAGE;
        return mishap;
    }

    /** Construct the operation-limit Mishap used by the evaluator boundary. */
    public static Mishap evaluationLimit(int maxOperations) {
        return new Mishap(Kind.EVALUATION_LIMIT,
            "hexcasting.error.evaluation_limit", null, null, null, null,
            0, 0, "limit=" + Math.max(0, maxOperations));
    }

    /** Create a localized value/type failure while retaining diagnostic detail. */
    public static Mishap invalidValue(String errorKey, String detail) {
        String key = errorKey == null || errorKey.isEmpty()
            ? "hexcasting.error.invalid_value" : errorKey;
        return new Mishap(Kind.INVALID_VALUE, key, null,
            null, null, null, 0, 0, detail);
    }

    /** Construct the orange-spark failure used by actions that require open parentheses. */
    public static Mishap needsParens(String detail) {
        return new Mishap(Kind.INVALID_CONTEXT, "hexcasting.mishap.needs_parens",
            null, null, null, null, 0, 0, detail);
    }

    /** Construct the modern wrong-dimension Mishap and its garbage result. */
    public static Mishap wrongDimension(String properDimension,
                                        String currentDimension) {
        Mishap mishap = new Mishap(Kind.WRONG_DIMENSION,
            "hexcasting.mishap.wrong_dimension", null,
            null, null, null, 0, 0, null);
        mishap.displayArgs = new Object[] {
            properDimension == null ? "?" : properDimension,
            currentDimension == null ? "?" : currentDimension
        };
        mishap.stackEffect = StackEffect.PUSH_GARBAGE;
        mishap.stackEffectCount = 1;
        return mishap;
    }

    /**
     * Promote one of the remaining legacy translation-key exceptions into a
     * typed Mishap at the point where it is created.  This keeps action
     * context and category information intact when the exception crosses a
     * nested eval/cc or continuation boundary.
     */
    public static Mishap legacy(String errorKey) {
        String key = errorKey == null || errorKey.isEmpty()
            ? "hexcasting.error.unknown" : errorKey;
        return new Mishap(classify(key), key, null,
            null, null, null, 0, 0, null);
    }

    public static Mishap invalidPattern(HexPattern pattern, EntityPlayer caster,
                                        int parenthesisDepth, int operation) {
        Mishap mishap = new Mishap(Kind.INVALID_PATTERN,
            "hexcasting.error.invalid_pattern", null,
            pattern, null, caster, parenthesisDepth, operation,
            "No action is registered for pattern " + String.valueOf(pattern));
        mishap.stackEffect = StackEffect.PUSH_GARBAGE;
        mishap.stackEffectCount = 1;
        return mishap;
    }

    /** Construct the modern invalid-Iota Mishap and its GarbageIota replacement. */
    public static Mishap invalidIota(Iota perpetrator, int reverseIndex,
                                     String expected, Object... expectedArgs) {
        Mishap mishap = new Mishap(Kind.INVALID_VALUE,
            "hexcasting.mishap.invalid_value", null,
            null, null, null, 0, 0,
            "expected=" + String.valueOf(expected) + ", got="
                + (perpetrator == null ? "null" : perpetrator.display()));
        mishap.stackEffect = StackEffect.REPLACE_WITH_GARBAGE;
        mishap.stackEffectReverseIndex = Math.max(0, reverseIndex);
        mishap.invalidExpected = expected == null || expected.isEmpty()
            ? "unknown" : expected;
        mishap.invalidPerpetrator = perpetrator;
        mishap.invalidExpectedArgs = expectedArgs == null
            ? new Object[0] : expectedArgs.clone();
        return mishap;
    }

    /** Construct the modern invalid-operator-arguments replacement effect. */
    public static Mishap invalidOperatorArgs(java.util.List<Iota> perpetrators,
                                             String detail) {
        List<Iota> safePerpetrators = perpetrators == null
            ? Collections.<Iota>emptyList()
            : new ArrayList<>(perpetrators);
        int count = perpetrators == null ? 0 : perpetrators.size();
        Mishap mishap = new Mishap(Kind.INVALID_VALUE,
            "hexcasting.error.invalid_operator_args", null,
            null, null, null, 0, 0, detail);
        mishap.stackEffect = StackEffect.REPLACE_MANY_WITH_GARBAGE;
        mishap.stackEffectCount = count;
        mishap.invalidOperatorPerpetrators = Collections.unmodifiableList(safePerpetrators);
        return mishap;
    }

    /** Construct the red-spark arithmetic Mishap used by modern Hex. */
    public static Mishap divideByZero(Iota left, Iota right, String suffix) {
        String safeSuffix = suffix == null || suffix.isEmpty() ? "divide" : suffix;
        Mishap mishap = new Mishap(Kind.ARITHMETIC,
            "hexcasting.mishap.divide_by_zero." + safeSuffix, null,
            null, null, null, 0, 0, null);
        mishap.arithmeticLeft = left;
        mishap.arithmeticRight = right;
        mishap.arithmeticSuffix = safeSuffix;
        mishap.stackEffect = StackEffect.PUSH_GARBAGE;
        mishap.stackEffectCount = 1;
        return mishap;
    }

    /** Construct the tangent form, whose operands are sine and cosine text. */
    public static Mishap tangentDivideByZero(Iota angle) {
        Mishap mishap = divideByZero(angle, angle, "divide");
        mishap.arithmeticSuffix = "tan";
        return mishap;
    }

    private static Kind classify(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Kind.UNKNOWN;
        }
        String message = raw.toLowerCase(Locale.ROOT);
        // Actions emit stable translation keys. Classify those keys first so
        // a future translation, or a word such as "entity" in an item name,
        // cannot change the gameplay category or its side effects.
        if (message.startsWith("hexcasting.")) {
            return classifyTranslationKey(message);
        }

        // Keep compatibility with old callers that still throw prose rather
        // than a translation key. This branch is intentionally conservative;
        // new actions should use a stable key above.
        if (message.contains("not enough media") || message.contains("not_enough_media")) {
            return Kind.NOT_ENOUGH_MEDIA;
        }
        if (message.contains("no action is registered") || message.contains("invalid pattern")) {
            return Kind.INVALID_PATTERN;
        }
        if (message.contains("operation limit") || message.contains("too many patterns")
            || message.contains("evaluated too many")) {
            return Kind.EVALUATION_LIMIT;
        }
        if (message.contains("permission") || message.contains("forbidden")
            || message.contains("disallowed")) {
            return Kind.PERMISSION_DENIED;
        }
        if (message.contains("context") || message.contains("requires")) {
            return Kind.INVALID_CONTEXT;
        }
        if (message.contains("stack") || message.contains("argument")
            || message.contains("args") || message.contains("expects")) {
            return Kind.NOT_ENOUGH_ARGUMENTS;
        }
        if (message.contains("entity") || message.contains("mob")
            || message.contains("living")) {
            return Kind.BAD_ENTITY;
        }
        if (message.contains("item") || message.contains("holder")
            || message.contains("bottle") || message.contains("media")) {
            return Kind.BAD_ITEM;
        }
        if (message.contains("block") || message.contains("akashic")) {
            return Kind.BAD_BLOCK;
        }
        if (message.contains("location") || message.contains("range")
            || message.contains("dimension") || message.contains("position")) {
            return Kind.BAD_LOCATION;
        }
        if (message.contains("expected") || message.contains("finite")
            || message.contains("invalid")) {
            return Kind.INVALID_VALUE;
        }
        return Kind.UNKNOWN;
    }

    private static Kind classifyTranslationKey(String key) {
        if ("hexcasting.error.not_enough_media".equals(key)) {
            return Kind.NOT_ENOUGH_MEDIA;
        }
        if ("hexcasting.error.invalid_pattern".equals(key)
            || "hexcasting.mishap.invalid_pattern_generic".equals(key)) {
            return Kind.INVALID_PATTERN;
        }
        if ("hexcasting.error.brainsweep_already".equals(key)) {
            return Kind.ALREADY_BRAINSWEPT;
        }
        if ("hexcasting.error.brainsweep_expected".equals(key)
            || "hexcasting.error.brainsweep_mob".equals(key)
            || "hexcasting.error.brainsweep_recipe".equals(key)) {
            return Kind.BAD_BRAINSWEEP;
        }
        if ("hexcasting.error.no_akashic_record".equals(key)) {
            return Kind.NO_AKASHIC_RECORD;
        }
        if ("hexcasting.mishap.no_args".equals(key)
            || "hexcasting.mishap.not_enough_args".equals(key)
            || "hexcasting.error.stack_underflow".equals(key)) {
            return Kind.NOT_ENOUGH_ARGUMENTS;
        }
        if ("hexcasting.mishap.stack_size".equals(key)) {
            return Kind.STACK_SIZE;
        }
        if ("hexcasting.mishap.needs_parens".equals(key)) {
            return Kind.INVALID_CONTEXT;
        }
        if ("hexcasting.mishap.wrong_dimension".equals(key)) {
            return Kind.WRONG_DIMENSION;
        }
        if (key.startsWith("hexcasting.mishap.divide_by_zero.")) {
            return Kind.ARITHMETIC;
        }
        if ("hexcasting.error.invalid_value".equals(key)
            || "hexcasting.error.invalid_iota".equals(key)
            || "hexcasting.error.invalid_operator_args".equals(key)
            || key.startsWith("hexcasting.error.arithmetic_")) {
            return Kind.INVALID_VALUE;
        }

        // Action-specific keys must be classified before the suffix rules
        // below.  A generic "target" or "position" suffix is not enough to
        // tell an entity mishap from a location or block mishap.
        if ("hexcasting.error.ignite_target".equals(key)
            || "hexcasting.error.potion_target".equals(key)
            || "hexcasting.error.flight_target".equals(key)
            || "hexcasting.error.recharge_entity".equals(key)
            || "hexcasting.error.entity_unavailable".equals(key)
            || "hexcasting.error.entity_data_expected".equals(key)
            || "hexcasting.error.entity_data_range".equals(key)
            || "hexcasting.error.entity_data_target".equals(key)
            || "hexcasting.error.blink_immune".equals(key)) {
            return Kind.BAD_ENTITY;
        }
        if ("hexcasting.error.craft_battery_media_item".equals(key)
            || "hexcasting.error.recharge_holder".equals(key)
            || "hexcasting.error.recharge_item".equals(key)
            || "hexcasting.error.recharge_full".equals(key)
            || "hexcasting.error.craft_battery_base".equals(key)
            || "hexcasting.error.craft_battery_media".equals(key)
             || "hexcasting.error.place_block_item".equals(key)
             || key.endsWith("_media")
             || "hexcasting.error.erase_holder".equals(key)
            || "hexcasting.error.data_holder_missing".equals(key)
            || "hexcasting.error.colorize_dye".equals(key)) {
            return Kind.BAD_ITEM;
        }
        if ("hexcasting.error.akashic_duplicate".equals(key)
            || "hexcasting.error.place_block_target".equals(key)
            || "hexcasting.error.place_block_failed".equals(key)
            || "hexcasting.error.conjure_block_target".equals(key)
            || "hexcasting.error.conjure_block_missing".equals(key)
            || "hexcasting.error.conjure_light_target".equals(key)
            || "hexcasting.error.conjure_light_missing".equals(key)
            || "hexcasting.error.edify_sapling".equals(key)
            || "hexcasting.error.edify_failed".equals(key)
            || "hexcasting.error.compare_block_expected".equals(key)) {
            return Kind.BAD_BLOCK;
        }
        if ("hexcasting.error.fluid_position".equals(key)
            || "hexcasting.error.teleport_great_position".equals(key)
            || "hexcasting.error.brainsweep_location".equals(key)) {
            return Kind.BAD_LOCATION;
        }

        if ("hexcasting.error.permission_denied".equals(key)
            || key.endsWith("_forbidden") || key.endsWith("_disallowed")) {
            return Kind.PERMISSION_DENIED;
        }
        if (key.endsWith("_context") || "hexcasting.error.no_media_context".equals(key)) {
            return Kind.INVALID_CONTEXT;
        }
        if (key.contains("operation_limit") || key.contains("evaluation_limit")
            || key.contains("too_many_patterns") || key.contains("evaluated_too_many")) {
            return Kind.EVALUATION_LIMIT;
        }
        if (key.endsWith("_range") || key.endsWith("_out_of_range")
            || key.endsWith("_wrong_dimension") || key.endsWith("_location")
            || key.endsWith("_dimension")) {
            return Kind.BAD_LOCATION;
        }
        if (key.endsWith("_entity") || key.endsWith("_mob")) {
            return Kind.BAD_ENTITY;
        }
        if ("hexcasting.error.compare_item_expected".equals(key)
            || key.endsWith("_item") || key.endsWith("_holder")
            || key.endsWith("_bottle") || key.endsWith("_dye")
            || key.contains("media_item") || key.startsWith("hexcasting.error.data_holder")) {
            return Kind.BAD_ITEM;
        }
        if (key.endsWith("_block") || key.endsWith("_sapling")
            || key.endsWith("_recipe") || key.endsWith("_target")
            || key.endsWith("_missing") || key.endsWith("_failed")) {
            return Kind.BAD_BLOCK;
        }
        if (key.endsWith("_position")) {
            return Kind.BAD_LOCATION;
        }
        if (key.endsWith("_args") || key.endsWith("_expected")
            || key.endsWith("_duration") || key.endsWith("_potency")
            || key.endsWith("_cost") || key.endsWith("_position")
            || key.endsWith("_zero") || key.endsWith("_radius")
            || key.endsWith("_vector")
            || key.endsWith("_out_of_bounds") || key.contains("bounded_integer")
            || key.contains("state_limit")
            || key.contains("finite") || key.contains("invalid")) {
            return Kind.INVALID_VALUE;
        }
        return Kind.UNKNOWN;
    }

    private void inferPermissionFailure(String raw) {
        if (raw == null) {
            return;
        }
        String message = raw.toLowerCase(Locale.ROOT);
        if (message.contains("_forbidden") || message.contains("permission_denied")
            || message.contains("disallowed")) {
            permissionChecked = true;
            permissionAllowed = false;
        }
    }
}
