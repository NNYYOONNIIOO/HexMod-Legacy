package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.iota.DoubleIota;
import at.petra_k.hexcasting.api.casting.iota.Iota;

import java.util.List;

/** Numeric operation implementations shared by stack actions. */
public final class NumericArithmetics {
    private interface Binary {
        double apply(double left, double right);
    }

    private interface Unary {
        double apply(double value);
    }

    private NumericArithmetics() {
    }

    public static Iota add(List<Iota> arguments) throws CastingException {
        return binary(arguments, "add", (left, right) -> left + right);
    }

    public static Iota subtract(List<Iota> arguments) throws CastingException {
        return binary(arguments, "subtract", (left, right) -> left - right);
    }

    public static Iota multiply(List<Iota> arguments) throws CastingException {
        return binary(arguments, "multiply", (left, right) -> left * right);
    }

    public static Iota divide(List<Iota> arguments) throws CastingException {
        double[] values = numbers(arguments, "divide");
        if (values[1] == 0.0D) {
            throw new CastingException("Cannot divide by zero");
        }
        return new DoubleIota(values[0] / values[1]);
    }

    public static Iota modulo(List<Iota> arguments) throws CastingException {
        double[] values = numbers(arguments, "modulo");
        if (values[1] == 0.0D) {
            throw new CastingException("Cannot take modulo by zero");
        }
        return new DoubleIota(values[0] % values[1]);
    }

    public static Iota power(List<Iota> arguments) throws CastingException {
        return binary(arguments, "power", Math::pow);
    }

    public static Iota absolute(List<Iota> arguments) throws CastingException {
        return unary(arguments, "absolute", Math::abs);
    }

    public static Iota floor(List<Iota> arguments) throws CastingException {
        return unary(arguments, "floor", Math::floor);
    }

    public static Iota ceil(List<Iota> arguments) throws CastingException {
        return unary(arguments, "ceil", Math::ceil);
    }

    private static Iota binary(List<Iota> arguments, String name, Binary operation)
        throws CastingException {
        double[] values = numbers(arguments, name);
        return new DoubleIota(operation.apply(values[0], values[1]));
    }

    private static Iota unary(List<Iota> arguments, String name, Unary operation)
        throws CastingException {
        if (arguments.size() != 1) {
            throw new CastingException(name + " expects exactly one argument");
        }
        return new DoubleIota(operation.apply(number(arguments.get(0), name, 0)));
    }

    private static double[] numbers(List<Iota> arguments, String name) throws CastingException {
        if (arguments.size() != 2) {
            throw new CastingException(name + " expects exactly two arguments");
        }
        return new double[] {
            number(arguments.get(0), name, 0),
            number(arguments.get(1), name, 1)
        };
    }

    private static double number(Iota value, String name, int index) throws CastingException {
        if (!(value instanceof DoubleIota)) {
            throw new CastingException(name + " expects numeric argument " + index
                + " but found " + value.getType().getId());
        }
        return ((DoubleIota) value).getValue();
    }
}
