package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.eval.Mishap;
import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;
import at.petra_k.hexcasting.api.casting.iota.BooleanIota;
import at.petra_k.hexcasting.api.casting.iota.Iota;

import java.util.List;

/** Select one of two Iotas based on a Boolean Iota. */
public final class BranchAction implements HexAction {
    @Override
    public void execute(CastingStack stack) throws CastingException {
        execute(stack, new CastingVM(stack));
    }

    @Override
    public void execute(CastingStack stack, CastingVM vm) throws CastingException {
        List<Iota> before = stack.snapshot();
        try {
            Iota falseValue = stack.pop();
            Iota trueValue = stack.pop();
            boolean condition = stack.pop(BooleanIota.class, 2).getValue();
            stack.push(condition ? trueValue : falseValue);
        } catch (CastingException exception) {
            stack.restore(before);
            throw exception;
        } catch (RuntimeException exception) {
            stack.restore(before);
            throw Mishap.fromRuntime(exception, null,
                vm.getActiveActionId(), vm.getPlayer(), vm.getParenDepth(),
                vm.getOperationsConsumed());
        }
    }
}
