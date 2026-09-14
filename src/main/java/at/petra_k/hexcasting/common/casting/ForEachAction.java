package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.ListIota;

import java.util.ArrayList;
import java.util.List;

/** Implements the list-producing for_each control-flow action. */
public final class ForEachAction implements HexAction {
    @Override
    public void execute(CastingStack stack) throws CastingException {
        execute(stack, new CastingVM(stack));
    }

    @Override
    public void execute(CastingStack stack, CastingVM vm) throws CastingException {
        List<Iota> before = stack.snapshot();
        try {
            ListIota code = stack.pop(ListIota.class);
            ListIota data = stack.pop(ListIota.class);

            List<Iota> baseStack = stack.snapshot();
            ArrayList<Iota> accumulator = new ArrayList<>();
            for (Iota datum : data.getItems()) {
                stack.restore(baseStack);
                stack.push(datum);
                vm.runNestedIotas(code.getItems());
                List<Iota> iterationStack = stack.snapshot();
                if (iterationStack.size() <= baseStack.size()) {
                    throw new CastingException("hexcasting.error.for_each_no_result");
                }
                accumulator.add(iterationStack.get(iterationStack.size() - 1));
            }
            stack.restore(baseStack);
            stack.push(new ListIota(accumulator));
        } catch (CastingException exception) {
            stack.restore(before);
            throw exception;
        } catch (RuntimeException exception) {
            stack.restore(before);
            throw exception;
        }
    }
}
