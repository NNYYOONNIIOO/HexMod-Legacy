package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;

import java.util.List;

/** Server-safe evaluator for a sequence of registered Hex patterns. */
public final class HexEvaluator {
    private HexEvaluator() {
    }

    public static CastingStack evaluate(List<HexPattern> patterns) throws CastingException {
        CastingStack stack = new CastingStack();
        evaluate(patterns, stack);
        return stack;
    }

    public static void evaluate(List<HexPattern> patterns, CastingStack stack) throws CastingException {
        for (HexPattern pattern : patterns) {
            HexAction action = HexActionRegistry.get(pattern);
            if (action == null) {
                throw new CastingException("No action is registered for pattern " + pattern.signature());
            }
            action.execute(stack);
        }
    }
}
