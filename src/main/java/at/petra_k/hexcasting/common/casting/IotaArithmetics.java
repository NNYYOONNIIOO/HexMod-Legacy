package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.iota.BooleanIota;
import at.petra_k.hexcasting.api.casting.iota.DoubleIota;
import at.petra_k.hexcasting.api.casting.iota.Iota;

import java.util.List;

/** Pure Boolean and numeric comparison operations for transactional actions. */
public final class IotaArithmetics {
    private interface Comparison {
        boolean test(double left, double right);
    }

    private IotaArithmetics() {
    }

    public static Iota and(List<Iota> arguments) throws CastingException {
        requireCount(arguments, 2, "and");
        return new BooleanIota(booleanValue(arguments.get(0), "and", 0)
            && booleanValue(arguments.get(1), "and", 1));
    }

    public static Iota or(List<Iota> arguments) throws CastingException {
        requireCount(arguments, 2, "or");
        return new BooleanIota(booleanValue(arguments.get(0), "or", 0)
            || booleanValue(arguments.get(1), "or", 1));
    }

    public static Iota greater(List<Iota> arguments) throws CastingException {
        return compare(arguments, "greater", (left, right) -> left > right);
    }

    public static Iota less(List<Iota> arguments) throws CastingException {
        return compare(arguments, "less", (left, right) -> left < right);
    }

    public static Iota greaterEq(List<Iota> arguments) throws CastingException {
        return compare(arguments, "greater_eq", (left, right) -> left >= right);
    }

    public static Iota lessEq(List<Iota> arguments) throws CastingException {
        return compare(arguments, "less_eq", (left, right) -> left <= right);
    }

    private static Iota compare(List<Iota> arguments, String name, Comparison comparison)
        throws CastingException {
        requireCount(arguments, 2, name);
        return new BooleanIota(comparison.test(
            number(arguments.get(0), name, 0), number(arguments.get(1), name, 1)));
    }

    private static boolean booleanValue(Iota value, String name, int index)
        throws CastingException {
        if (!(value instanceof BooleanIota)) {
            throw new CastingException(name + " expects Boolean argument " + index
                + " but found " + value.getClass().getSimpleName());
        }
        return ((BooleanIota) value).getValue();
    }

    private static double number(Iota value, String name, int index) throws CastingException {
        if (!(value instanceof DoubleIota)) {
            throw new CastingException(name + " expects numeric argument " + index
                + " but found " + value.getClass().getSimpleName());
        }
        return ((DoubleIota) value).getValue();
    }

    private static void requireCount(List<Iota> arguments, int expected, String name)
        throws CastingException {
        if (arguments.size() != expected) {
            throw new CastingException(name + " expects exactly " + expected + " arguments");
        }
    }
}
