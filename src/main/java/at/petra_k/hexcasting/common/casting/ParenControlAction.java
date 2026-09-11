package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;
import at.petra_k.hexcasting.api.casting.iota.DoubleIota;
import at.petra_k.hexcasting.api.casting.iota.Iota;

/** VM-aware meta actions for escaping and parenthesized code capture. */
public final class ParenControlAction implements HexAction {
    public enum Kind {
        ESCAPE, OPEN, CLOSE, OPEN_N, CLOSE_ALL
    }

    private final Kind kind;

    public ParenControlAction(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("Parenthesis action kind cannot be null");
        }
        this.kind = kind;
    }

    @Override
    public void execute(CastingStack stack) throws CastingException {
        execute(stack, new CastingVM(stack));
    }

    @Override
    public void execute(CastingStack stack, CastingVM vm) throws CastingException {
        CastingVM activeVm = vm == null ? new CastingVM(stack) : vm;
        switch (kind) {
            case ESCAPE:
                activeVm.setEscapeNext();
                break;
            case OPEN:
                activeVm.openParen();
                break;
            case CLOSE:
                activeVm.closeParen();
                break;
            case OPEN_N:
                activeVm.openParens(readCount(stack));
                break;
            case CLOSE_ALL:
                activeVm.closeAllParens();
                break;
            default:
                throw new IllegalStateException("Unknown parenthesis action kind: " + kind);
        }
    }

    @Override
    public boolean executesInParentheses() {
        return true;
    }

    private static int readCount(CastingStack stack) throws CastingException {
        DoubleIota value = stack.pop(DoubleIota.class);
        double raw = value.getValue();
        if (Double.isNaN(raw) || Double.isInfinite(raw) || raw != Math.rint(raw)
            || raw < 0.0D || raw > Iota.MAX_SERIALIZATION_TOTAL) {
            stack.push(value);
            throw new CastingException("Expected a non-negative integer parenthesis count but found " + raw);
        }
        return (int) raw;
    }
}
