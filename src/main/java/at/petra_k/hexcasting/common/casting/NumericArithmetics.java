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
        double[] values = numbers(arguments, "power");
        if (values[0] < 0.0D
            && Math.abs(values[1] - Math.rint(values[1])) > 1.0E-5D) {
            throw new CastingException("Cannot raise a negative number to a fractional power");
        }
        return new DoubleIota(Math.pow(values[0], values[1]));
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

    public static Iota sine(List<Iota> arguments) throws CastingException {
        return unary(arguments, "sin", Math::sin);
    }

    public static Iota cosine(List<Iota> arguments) throws CastingException {
        return unary(arguments, "cos", Math::cos);
    }

    public static Iota tangent(List<Iota> arguments) throws CastingException {
        if (arguments.size() != 1) {
            throw new CastingException("tan expects exactly one argument");
        }
        double value = number(arguments.get(0), "tan", 0);
        if (Math.abs(Math.cos(value)) < 1.0E-12D) {
            throw new CastingException("Tangent is undefined at this angle");
        }
        return new DoubleIota(Math.tan(value));
    }

    public static Iota arcsine(List<Iota> arguments) throws CastingException {
        return boundedUnary(arguments, "arcsin", -1.0D, 1.0D, Math::asin);
    }

    public static Iota arccosine(List<Iota> arguments) throws CastingException {
        return boundedUnary(arguments, "arccos", -1.0D, 1.0D, Math::acos);
    }

    public static Iota arctangent(List<Iota> arguments) throws CastingException {
        return unary(arguments, "arctan", Math::atan);
    }

    public static Iota arctangent2(List<Iota> arguments) throws CastingException {
        return binary(arguments, "arctan2", Math::atan2);
    }

    public static Iota logarithm(List<Iota> arguments) throws CastingException {
        if (arguments.size() != 2) {
            throw new CastingException("log expects exactly two arguments");
        }
        double value = number(arguments.get(0), "log", 0);
        double base = number(arguments.get(1), "log", 1);
        if (value <= 0.0D || base <= 0.0D || base == 1.0D) {
            throw new CastingException("log expects a positive value and a positive base other than one");
        }
        return new DoubleIota(Math.log(value) / Math.log(base));
    }

    private static Iota binary(List<Iota> arguments, String name, Binary operation)
        throws CastingException {
        double[] values = numbers(arguments, name);
        return new DoubleIota(operation.apply(values[0], values[1]));
    }

    private static Iota boundedUnary(List<Iota> arguments, String name, double minimum,
        double maximum, Unary operation) throws CastingException {
        if (arguments.size() != 1) {
            throw new CastingException(name + " expects exactly one argument");
        }
        double value = number(arguments.get(0), name, 0);
        if (value < minimum || value > maximum) {
            throw new CastingException(name + " expects an argument in [" + minimum + ", " + maximum + "]");
        }
        return new DoubleIota(operation.apply(value));
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
