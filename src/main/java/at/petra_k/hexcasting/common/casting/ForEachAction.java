package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.ListIota;
import at.petra_k.hexcasting.api.casting.iota.PatternIota;
import at.petra_k.hexcasting.api.casting.math.HexPattern;

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
            ArrayList<HexPattern> patterns = new ArrayList<>();
            for (Iota codeIota : code.getItems()) {
                if (!(codeIota instanceof PatternIota)) {
                    throw new CastingException("for_each code must contain patterns");
                }
                patterns.add(((PatternIota) codeIota).getPattern());
            }

            List<Iota> baseStack = stack.snapshot();
            ArrayList<Iota> accumulator = new ArrayList<>();
            for (Iota datum : data.getItems()) {
                stack.restore(baseStack);
                stack.push(datum);
                vm.runNested(patterns);
                accumulator.addAll(stack.snapshot());
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
