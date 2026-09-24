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

    /** Create a localized value/type failure while retaining diagnostic detail. */
    public static Mishap invalidValue(String errorKey, String detail) {
        String key = errorKey == null || errorKey.isEmpty()
            ? "hexcasting.error.invalid_value" : errorKey;
        return new Mishap(Kind.INVALID_VALUE, key, null,
            null, null, null, 0, 0, detail);
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
        if ("hexcasting.error.brainsweep_recipe".equals(key)) {
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
            || "hexcasting.error.brainsweep_expected".equals(key)
            || "hexcasting.error.brainsweep_mob".equals(key)
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
        if (key.contains("operation_limit") || key.contains("too_many_patterns")
            || key.contains("evaluated_too_many")) {
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
