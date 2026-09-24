package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.Mishap;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.interop.inline.HexInline;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;

import java.util.Locale;

/**
 * One message boundary for every 1.12.2 casting entry point.
 *
 * <p>The modern evaluator prefixes a mishap with the name of the action that
 * caused it.  Older port callers used a mixture of raw translation keys,
 * exception messages, and the generic staff error.  Keeping that policy in
 * one small server-safe helper makes scrolls, packaged spells, thought knots,
 * and the staff agree without coupling the evaluator to a particular GUI.</p>
 */
public final class MishapFeedback {
    private MishapFeedback() {
    }

    public static void send(EntityPlayer player, CastingException exception) {
        if (player != null) {
            player.sendMessage(new TextComponentString(localize(exception)));
        }
    }

    /** Convert an exception at an item/effect boundary and retain its context. */
    public static Mishap asMishap(CastingException exception, HexPattern pattern,
                                  ResourceLocation actionId, EntityPlayer player,
                                  int parenthesisDepth, int operation) {
        Mishap mishap = Mishap.from(exception, pattern, actionId, player,
            parenthesisDepth, operation);
        if (mishap.getPattern() == null || mishap.getActionId() == null
            || mishap.getCaster() == null) {
            mishap.withExecutionContext(
                mishap.getPattern() == null ? pattern : null,
                mishap.getActionId() == null ? actionId : null,
                mishap.getCaster() == null ? player : null,
                parenthesisDepth, operation);
        }
        return mishap;
    }

    public static String localize(CastingException exception) {
        if (exception instanceof Mishap) {
            return localizeMishap((Mishap) exception);
        }
        return localizeRaw(exception == null ? null : exception.getMessage());
    }

    private static String localizeMishap(Mishap mishap) {
        if (mishap.getKind() == Mishap.Kind.INVALID_PATTERN) {
            String pattern = mishap.getPattern() == null ? "?"
                : HexInline.formatPattern(mishap.getPattern());
            return I18n.translateToLocalFormatted(
                "hexcasting.message.pattern_unregistered", pattern);
        }

        String detail = localizeKey(mishap.getDisplayKey());
        String action = localizeAction(mishap.getActionDisplayKey(),
            mishap.getActionName());
        if (action == null || action.isEmpty()) {
            return detail;
        }
        String wrapper = I18n.translateToLocal("hexcasting.mishap");
        if ("hexcasting.mishap".equals(wrapper)) {
            return action + ": " + detail;
        }
        return I18n.translateToLocalFormatted("hexcasting.mishap", action, detail);
    }

    private static String localizeRaw(String message) {
        if (message == null || message.isEmpty()) {
            return I18n.translateToLocal("hexcasting.message.staff_error");
        }
        String lower = message.toLowerCase(Locale.ROOT);
        String marker = "no action is registered for pattern";
        int markerIndex = lower.indexOf(marker);
        if (markerIndex >= 0) {
            String signature = message.substring(markerIndex + marker.length()).trim();
            try {
                signature = HexInline.formatPattern(
                    at.petra_k.hexcasting.api.casting.math.HexPattern
                        .fromSignature(signature));
            } catch (IllegalArgumentException ignored) {
                // Keep compatibility with older saved/error messages.
            }
            return I18n.translateToLocalFormatted(
                "hexcasting.message.pattern_unregistered", signature);
        }
        return localizeKey(message);
    }

    private static String localizeKey(String key) {
        if (key == null || key.isEmpty()) {
            return I18n.translateToLocal("hexcasting.message.staff_error");
        }
        String translated = I18n.translateToLocal(key);
        return key.equals(translated)
            ? I18n.translateToLocal("hexcasting.message.staff_error") : translated;
    }

    private static String localizeAction(String displayKey, String fallbackId) {
        if (displayKey != null) {
            String translated = I18n.translateToLocal(displayKey);
            if (!displayKey.equals(translated)) {
                return translated;
            }
        }
        if (fallbackId == null || fallbackId.isEmpty()) {
            return null;
        }
        try {
            ResourceLocation id = new ResourceLocation(fallbackId);
            return id.getResourcePath();
        } catch (RuntimeException ignored) {
            return fallbackId;
        }
    }
}
