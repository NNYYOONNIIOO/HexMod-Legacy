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
        ESCAPE, RUNTIME_ESCAPE, OPEN, CLOSE, OPEN_N, CLOSE_ALL, READ_INTO, UNDO
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
            case RUNTIME_ESCAPE:
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
            case READ_INTO:
                activeVm.readIntoParen();
                break;
            case UNDO:
                activeVm.undo();
                break;
            default:
                throw new IllegalStateException("Unknown parenthesis action kind: " + kind);
        }
    }

    @Override
    public boolean executesInParentheses() {
        // Only actions with an explicit operateInParens implementation run
        // while a parenthesized program is being captured. In particular,
        // open_n_parens and close_all_parens are ordinary captured patterns
        // in Hex; executing them here corrupts the captured program and its
        // parenthesis count.
        switch (kind) {
            case ESCAPE:
            case RUNTIME_ESCAPE:
            case OPEN:
            case CLOSE:
            case READ_INTO:
            case UNDO:
                return true;
            case OPEN_N:
            case CLOSE_ALL:
            default:
                return false;
        }
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
