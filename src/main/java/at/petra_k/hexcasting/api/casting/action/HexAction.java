package at.petra_k.hexcasting.api.casting.action;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;

/** A single operation that consumes and/or produces Iotas. */
@FunctionalInterface
public interface HexAction {
    void execute(CastingStack stack) throws CastingException;

    /**
     * Execute with access to the active VM. Simple stack actions keep the
     * original functional API; contextual actions can override this method
     * to schedule nested work without creating a second evaluator.
     */
    default void execute(CastingStack stack, CastingVM vm) throws CastingException {
        execute(stack);
    }

    /** Whether this action is allowed to run while a parenthesized list is captured. */
    default boolean executesInParentheses() {
        return false;
    }
}
