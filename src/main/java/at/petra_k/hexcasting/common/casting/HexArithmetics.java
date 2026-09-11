package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.ListIota;

import java.util.ArrayList;
import java.util.List;

/**
 * Dispatches operator actions by Iota type.
 *
 * <p>The 1.20.1 implementation exposes arithmetic as a type-directed
 * registry.  This class is the 1.12.2-compatible seam for that behavior:
 * numeric operations retain the existing numeric semantics, while operations
 * that have a natural non-numeric meaning can be added without changing the
 * action registry or evaluator.</p>
 */
public final class HexArithmetics {
    private HexArithmetics() {
    }

    /** Add two numeric Iotas or concatenate two ListIotas. */
    public static Iota add(List<Iota> arguments) throws CastingException {
        requireBinary(arguments, "add");
        Iota left = arguments.get(0);
        Iota right = arguments.get(1);

        if (left instanceof ListIota || right instanceof ListIota) {
            if (!(left instanceof ListIota) || !(right instanceof ListIota)) {
                throw new CastingException("add expects either two numeric Iotas or two lists");
            }
            ArrayList<Iota> result = new ArrayList<>();
            result.addAll(((ListIota) left).getItems());
            result.addAll(((ListIota) right).getItems());
            return new ListIota(result);
        }

        return NumericArithmetics.add(arguments);
    }

    public static Iota subtract(List<Iota> arguments) throws CastingException {
        return NumericArithmetics.subtract(arguments);
    }

    public static Iota multiply(List<Iota> arguments) throws CastingException {
        return NumericArithmetics.multiply(arguments);
    }

    public static Iota divide(List<Iota> arguments) throws CastingException {
        return NumericArithmetics.divide(arguments);
    }

    public static Iota modulo(List<Iota> arguments) throws CastingException {
        return NumericArithmetics.modulo(arguments);
    }

    public static Iota power(List<Iota> arguments) throws CastingException {
        return NumericArithmetics.power(arguments);
    }

    public static Iota absolute(List<Iota> arguments) throws CastingException {
        return NumericArithmetics.absolute(arguments);
    }

    public static Iota floor(List<Iota> arguments) throws CastingException {
        return NumericArithmetics.floor(arguments);
    }

    public static Iota ceil(List<Iota> arguments) throws CastingException {
        return NumericArithmetics.ceil(arguments);
    }

    private static void requireBinary(List<Iota> arguments, String name)
        throws CastingException {
        if (arguments.size() != 2) {
            throw new CastingException(name + " expects exactly two arguments");
        }
    }
}
