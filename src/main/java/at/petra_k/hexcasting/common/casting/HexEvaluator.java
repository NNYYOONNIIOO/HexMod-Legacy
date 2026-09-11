package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;
import at.petra_k.hexcasting.api.casting.math.HexPattern;

import java.util.List;
import at.petra_k.hexcasting.api.capability.IHexCastingData;

/** Server-safe evaluator for a sequence of registered Hex patterns. */
public final class HexEvaluator {
    private HexEvaluator() {
    }

    public static CastingStack evaluate(List<HexPattern> patterns) throws CastingException {
        CastingStack stack = new CastingStack();
        evaluate(patterns, stack, CastingVM.DEFAULT_MAX_OPERATIONS);
        return stack;
    }

    public static void evaluate(List<HexPattern> patterns, CastingStack stack) throws CastingException {
        evaluate(patterns, stack, CastingVM.DEFAULT_MAX_OPERATIONS);
    }

    /** Evaluate with an explicit operation budget. */
    public static void evaluate(List<HexPattern> patterns, CastingStack stack, int maxOperations)
        throws CastingException {
        CastingVM vm = new CastingVM(stack).enqueue(patterns);
        vm.run(maxOperations);
    }
    /** Evaluate with player/environment state available to contextual actions. */
    public static void evaluate(List<HexPattern> patterns, CastingStack stack,
                                IHexCastingData castingData) throws CastingException {
        CastingVM vm = new CastingVM(stack).enqueue(patterns);
        vm.setCastingData(castingData);
        vm.run(CastingVM.DEFAULT_MAX_OPERATIONS);
    }

}
