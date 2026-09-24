package at.petra_k.hexcasting.api.casting.eval;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

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
        NO_AKASHIC_RECORD,
        BAD_LOCATION,
        PERMISSION_DENIED,
        INVALID_VALUE,
        EVALUATION_LIMIT,
        STACK_SIZE,
        INVALID_CONTEXT,
        INTERNAL,
        UNKNOWN
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

    /** Attach execution data to a Mishap created before the VM knew the action. */
    public Mishap withExecutionContext(HexPattern pattern, ResourceLocation actionId,
                                      EntityPlayer caster, int parenthesisDepth,
                                      int operation) {
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
        if (target == null) {
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
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
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
            case NO_AKASHIC_RECORD:
                return 0x8B5CC7;
            case BAD_LOCATION:
                return 0xE97AC1;
            case PERMISSION_DENIED:
                return 0x303030;
            case NOT_ENOUGH_MEDIA:
                return 0xE05252;
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
        return mishap;
    }

    /** Construct the black-spark stack-size mishap used by the modern VM. */
    public static Mishap stackSize() {
        return new Mishap(Kind.STACK_SIZE, "hexcasting.mishap.stack_size",
            null, null, null, null, 0, 0, null);
    }

    public static Mishap invalidPattern(HexPattern pattern, EntityPlayer caster,
                                        int parenthesisDepth, int operation) {
        return new Mishap(Kind.INVALID_PATTERN,
            "hexcasting.error.invalid_pattern", null,
            pattern, null, caster, parenthesisDepth, operation,
            "No action is registered for pattern " + String.valueOf(pattern));
    }

    private static Kind classify(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Kind.UNKNOWN;
        }
        String message = raw.toLowerCase(Locale.ROOT);
        // The legacy action table reports stable translation keys. Prefer
        // those keys over broad English substring matching so a future
        // translation or an item named "entity" cannot change the category.
        if (message.contains("not_enough_media")) {
            return Kind.NOT_ENOUGH_MEDIA;
        }
        if (message.contains("invalid_pattern")
            || message.contains("no action is registered")) {
            return Kind.INVALID_PATTERN;
        }
        if (message.contains("brainsweep_already")) {
            return Kind.ALREADY_BRAINSWEPT;
        }
        if (message.contains("brainsweep_recipe")) {
            return Kind.BAD_BRAINSWEEP;
        }
        if (message.contains("no_akashic_record")) {
            return Kind.NO_AKASHIC_RECORD;
        }
        if (message.contains("stack_underflow") || message.contains("not_enough_args")
            || message.contains("no_args")) {
            return Kind.NOT_ENOUGH_ARGUMENTS;
        }
        if (message.contains("permission_denied") || message.contains("_forbidden")
            || message.contains("disallowed")) {
            return Kind.PERMISSION_DENIED;
        }
        if (message.contains("_context") || message.contains("no_media_context")) {
            return Kind.INVALID_CONTEXT;
        }
        if (message.contains("operation limit") || message.contains("too many patterns")
            || message.contains("evaluated too many") || message.contains("size limit")) {
            if (message.contains("stack") && message.contains("size")) {
                return Kind.STACK_SIZE;
            }
            return Kind.EVALUATION_LIMIT;
        }
        if (message.contains("_range") || message.contains("out_of_range")
            || message.contains("wrong_dimension") || message.contains("_position")
            || message.contains("location")) {
            return Kind.BAD_LOCATION;
        }
        if (message.contains("entity_unavailable") || message.contains("_entity")
            || message.contains("_mob") || message.contains("_living")
            || message.contains("potion_target") || message.contains("brainsweep_mob")) {
            return Kind.BAD_ENTITY;
        }
        if (message.contains("_item") || message.contains("_holder")
            || message.contains("_bottle") || message.contains("_dye")
            || message.contains("place_block_item") || message.contains("media_item")) {
            return Kind.BAD_ITEM;
        }
        if (message.contains("_block") || message.contains("_sapling")
            || message.contains("_recipe") || message.contains("akashic")) {
            return Kind.BAD_BLOCK;
        }
        if (message.contains("_args") || message.contains("_duration")
            || message.contains("_potency") || message.contains("_cost")
            || message.contains("finite") || message.contains("bounded")
            || message.contains("_zero") || message.contains("invalid")) {
            return Kind.INVALID_VALUE;
        }
        if (message.contains("_context") || message.contains("no_media_context")) {
            return Kind.INVALID_CONTEXT;
        }
        if (message.contains("no action is registered")
            || message.contains("invalid pattern")) {
            return Kind.INVALID_PATTERN;
        }
        if (message.contains("operation limit") || message.contains("too many patterns")
            || message.contains("evaluated too many")) {
            return Kind.EVALUATION_LIMIT;
        }
        if (message.contains("not_enough_media") || message.contains("not enough media")) {
            return Kind.NOT_ENOUGH_MEDIA;
        }
        if (message.contains("forbidden") || message.contains("permission")
            || message.contains("disallowed")) {
            return Kind.PERMISSION_DENIED;
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
        if (message.contains("context") || message.contains("requires")) {
            return Kind.INVALID_CONTEXT;
        }
        if (message.contains("expected") || message.contains("finite")
            || message.contains("invalid")) {
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
