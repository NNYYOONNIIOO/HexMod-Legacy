package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.iota.BooleanIota;
import at.petra_k.hexcasting.api.casting.iota.DoubleIota;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.ListIota;

import java.util.ArrayList;
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
        Iota left = arguments.get(0);
        Iota right = arguments.get(1);
        if (left instanceof ListIota && right instanceof ListIota) {
            return listIntersection((ListIota) left, (ListIota) right);
        }
        return new BooleanIota(booleanValue(left, "and", 0)
            && booleanValue(right, "and", 1));
    }

    public static Iota or(List<Iota> arguments) throws CastingException {
        requireCount(arguments, 2, "or");
        Iota left = arguments.get(0);
        Iota right = arguments.get(1);
        if (left instanceof ListIota && right instanceof ListIota) {
            return listUnion((ListIota) left, (ListIota) right);
        }
        return new BooleanIota(booleanValue(left, "or", 0)
            || booleanValue(right, "or", 1));
    }

    /** Symmetric difference for lists; boolean xor is intentionally rejected. */
    public static Iota xor(List<Iota> arguments) throws CastingException {
        requireCount(arguments, 2, "xor");
        Iota left = arguments.get(0);
        Iota right = arguments.get(1);
        if (left instanceof BooleanIota && right instanceof BooleanIota) {
            return new BooleanIota(((BooleanIota) left).getValue()
                != ((BooleanIota) right).getValue());
        }
        if (!(left instanceof ListIota) || !(right instanceof ListIota)) {
            throw new CastingException("xor expects two booleans or two lists");
        }
        List<Iota> leftItems = ((ListIota) left).getItems();
        List<Iota> rightItems = ((ListIota) right).getItems();
        ArrayList<Iota> result = new ArrayList<>();
        for (Iota value : leftItems) {
            if (!containsIota(rightItems, value)) {
                result.add(value);
            }
        }
        for (Iota value : rightItems) {
            if (!containsIota(leftItems, value)) {
                result.add(value);
            }
        }
        return new ListIota(result);
    }

    private static Iota listIntersection(ListIota left, ListIota right) {
        ArrayList<Iota> result = new ArrayList<>();
        List<Iota> rightItems = right.getItems();
        for (Iota value : left.getItems()) {
            if (containsIota(rightItems, value)) {
                result.add(value);
            }
        }
        return new ListIota(result);
    }

    private static Iota listUnion(ListIota left, ListIota right) {
        ArrayList<Iota> result = new ArrayList<>();
        List<Iota> leftItems = left.getItems();
        List<Iota> rightItems = right.getItems();
        result.addAll(leftItems);
        for (Iota value : rightItems) {
            if (!containsIota(leftItems, value)) {
                result.add(value);
            }
        }
        return new ListIota(result);
    }

    private static boolean containsIota(List<Iota> values, Iota needle) {
        for (Iota value : values) {
            if (Iota.tolerates(value, needle)) {
                return true;
            }
        }
        return false;
    }

    public static Iota greater(List<Iota> arguments) throws CastingException {
        return compare(arguments, "greater", (left, right) -> left > right);
    }

    public static Iota less(List<Iota> arguments) throws CastingException {
        return compare(arguments, "less", (left, right) -> left < right);
    }

    public static Iota greaterEq(List<Iota> arguments) throws CastingException {
        return compare(arguments, "greater_eq", (left, right) ->
            left >= right || Math.abs(left - right) <= 1.0E-5D);
    }

    public static Iota lessEq(List<Iota> arguments) throws CastingException {
        return compare(arguments, "less_eq", (left, right) ->
            left <= right || Math.abs(left - right) <= 1.0E-5D);
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
