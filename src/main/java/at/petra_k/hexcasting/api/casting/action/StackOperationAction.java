package at.petra_k.hexcasting.api.casting.action;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.iota.Iota;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A transactional fixed-arity action with zero or more outputs. */
public final class StackOperationAction implements HexAction {
    private final int argumentCount;
    private final StackOperation operation;

    public StackOperationAction(int argumentCount, StackOperation operation) {
        if (argumentCount < 0) {
            throw new IllegalArgumentException("Operation argument count cannot be negative");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Operation implementation cannot be null");
        }
        this.argumentCount = argumentCount;
        this.operation = operation;
    }

    public int getArgumentCount() {
        return argumentCount;
    }

    @Override
    public void execute(CastingStack stack) throws CastingException {
        List<Iota> before = stack.snapshot();
        try {
            ArrayList<Iota> arguments = new ArrayList<>(argumentCount);
            for (int i = 0; i < argumentCount; i++) {
                arguments.add(0, stack.pop());
            }
            List<Iota> results = operation.apply(Collections.unmodifiableList(arguments));
            if (results == null) {
                throw new CastingException("A stack operation returned null results");
            }
            for (Iota result : results) {
                if (result == null) {
                    throw new CastingException("A stack operation returned a null Iota");
                }
                stack.push(result);
            }
        } catch (CastingException exception) {
            stack.restore(before);
            throw exception;
        } catch (RuntimeException exception) {
            stack.restore(before);
            throw exception;
        }
    }
}
