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
        BAD_LOCATION,
        PERMISSION_DENIED,
        INVALID_VALUE,
        EVALUATION_LIMIT,
        INVALID_CONTEXT,
        INTERNAL,
        UNKNOWN
    }

    private final Kind kind;
    private final String errorKey;
    private final HexPattern pattern;
    private final ResourceLocation actionId;
    private final EntityPlayer caster;
    private final int parenthesisDepth;
    private final int operation;
    private final String detail;
    private final String casterUuid;
    private final String casterName;
    private final int casterDimension;
    private final double casterX;
    private final double casterY;
    private final double casterZ;
    private double targetDistance = Double.NaN;
    private int targetDimension = Integer.MIN_VALUE;
    private String targetUuid;
    private boolean permissionChecked;
    private boolean permissionAllowed = true;

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

    public boolean isPermissionChecked() {
        return permissionChecked;
    }

    public boolean isPermissionAllowed() {
        return permissionAllowed;
    }

    /** Attach target data without losing the original exception context. */
    public Mishap withTarget(Entity target) {
        if (target == null) {
            return this;
        }
        targetDistance = caster == null ? Double.NaN : caster.getDistance(target);
        targetDimension = target.dimension;
        targetUuid = target.getUniqueID() == null ? null : target.getUniqueID().toString();
        return this;
    }

    /** Record whether a world/permission check was performed for this mishap. */
    public Mishap withPermission(boolean checked, boolean allowed) {
        permissionChecked = checked;
        permissionAllowed = allowed;
        return this;
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
            case BAD_LOCATION:
                return 0xE97AC1;
            case PERMISSION_DENIED:
                return 0x303030;
            case NOT_ENOUGH_MEDIA:
                return 0xE05252;
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
        return new Mishap(classify(message), message, exception, pattern,
            actionId, caster,
            parenthesisDepth, operation);
    }

    /** Convert an unexpected action exception into an internal Mishap. */
    public static Mishap fromRuntime(RuntimeException exception, HexPattern pattern,
                                     net.minecraft.util.ResourceLocation actionId,
                                     EntityPlayer caster,
                                     int parenthesisDepth, int operation) {
        String detail = exception == null ? "unknown" : exception.getClass().getSimpleName();
        return new Mishap(Kind.INTERNAL, "hexcasting.error.unknown", exception, pattern,
            actionId, caster,
            parenthesisDepth, operation);
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
}
