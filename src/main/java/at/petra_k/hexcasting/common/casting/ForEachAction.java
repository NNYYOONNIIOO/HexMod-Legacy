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
                // Hex resets runtime escape at the start of every Thoth
                // iteration, so an escape at the end of one body cannot
                // affect the first Iota of the next body.
                vm.resetEscape();
                vm.runNestedIotas(code.getItems());
                boolean halted = vm.wasLastNestedRunHalted();
                List<Iota> iterationStack = stack.snapshot();
                // The modern FrameForEach appends the complete stack state
                // produced by the body, not only its top value. This keeps
                // multi-result bodies and the surrounding stack layout
                // compatible with Hex's list semantics.
                accumulator.addAll(iterationStack);
                if (halted) {
                    break;
                }
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
