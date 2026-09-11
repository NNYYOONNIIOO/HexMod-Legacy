package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.ListIota;

import java.util.List;

/** Evaluate a code list behind a continuation boundary. */
public final class EvalBreakableAction implements HexAction {
    @Override
    public void execute(CastingStack stack) throws CastingException {
        execute(stack, new CastingVM(stack));
    }

    @Override
    public void execute(CastingStack stack, CastingVM vm) throws CastingException {
        if (vm == null) {
            throw new CastingException("eval/cc requires an active casting VM");
        }
        List<Iota> before = stack.snapshot();
        try {
            ListIota code = stack.pop(ListIota.class);
            vm.runNestedIotas(code.getItems());
        } catch (CastingException exception) {
            stack.restore(before);
            throw exception;
        } catch (RuntimeException exception) {
            stack.restore(before);
            throw exception;
        }
    }
}
