package at.petra_k.hexcasting.api.casting.action;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.iota.Iota;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A stack action backed by a fixed-arity Iota operation.
 *
 * <p>Arguments are passed bottom-to-top, matching the order in which a
 * binary operation is written: the first argument is the value below the
 * second argument on the casting stack. The stack is restored if argument
 * validation or the operation fails, which gives the 1.12.2 evaluator the
 * same transactional behavior expected from an operation frame.</p>
 */
public final class OperationAction implements HexAction {
    private final int argumentCount;
    private final IotaOperation operation;

    public OperationAction(int argumentCount, IotaOperation operation) {
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
            Iota result = operation.apply(Collections.unmodifiableList(arguments));
            if (result == null) {
                throw new CastingException("A casting operation returned a null Iota");
            }
            stack.push(result);
        } catch (CastingException exception) {
            stack.restore(before);
            throw exception;
        } catch (RuntimeException exception) {
            stack.restore(before);
            throw exception;
        }
    }
}
