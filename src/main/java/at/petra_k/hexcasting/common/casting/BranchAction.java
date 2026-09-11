package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;
import at.petra_k.hexcasting.api.casting.iota.BooleanIota;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.ListIota;

import java.util.List;

/** Select and execute one of two code lists based on a Boolean Iota. */
public final class BranchAction implements HexAction {
    @Override
    public void execute(CastingStack stack) throws CastingException {
        execute(stack, new CastingVM(stack));
    }

    @Override
    public void execute(CastingStack stack, CastingVM vm) throws CastingException {
        List<Iota> before = stack.snapshot();
        try {
            ListIota falseBranch = stack.pop(ListIota.class);
            ListIota trueBranch = stack.pop(ListIota.class);
            boolean condition = stack.pop(BooleanIota.class).getValue();
            List<Iota> selected = condition ? trueBranch.getItems() : falseBranch.getItems();
            vm.runNestedIotas(selected);
        } catch (CastingException exception) {
            stack.restore(before);
            throw exception;
        } catch (RuntimeException exception) {
            stack.restore(before);
            throw exception;
        }
    }
}
