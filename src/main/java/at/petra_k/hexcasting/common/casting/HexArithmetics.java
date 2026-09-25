package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.Mishap;
import at.petra_k.hexcasting.api.casting.iota.BooleanIota;
import at.petra_k.hexcasting.api.casting.iota.DoubleIota;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.ListIota;
import at.petra_k.hexcasting.api.casting.iota.Vec3Iota;
import net.minecraft.util.math.Vec3d;

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
                throw Mishap.invalidOperatorArgs(arguments,
                    "add received incompatible overloaded arguments");
            }
            ArrayList<Iota> result = new ArrayList<>();
            result.addAll(((ListIota) left).getItems());
            result.addAll(((ListIota) right).getItems());
            return new ListIota(result);
        }

        if (left instanceof Vec3Iota || right instanceof Vec3Iota) {
            return vectorBinary(arguments, "add", true);
        }

        return NumericArithmetics.add(arguments);
    }

    public static Iota subtract(List<Iota> arguments) throws CastingException {
        requireBinary(arguments, "subtract");
        Iota left = arguments.get(0);
        Iota right = arguments.get(1);
        if (left instanceof Vec3Iota || right instanceof Vec3Iota) {
            return vectorBinary(arguments, "subtract", false);
        }
        return NumericArithmetics.subtract(arguments);
    }

    private static Iota vectorBinary(List<Iota> arguments, String name, boolean add)
        throws CastingException {
        return vectorComponentwise(arguments, name, (left, right) ->
            add ? left + right : left - right);
    }

    public static Iota sine(List<Iota> arguments) throws CastingException {
        return NumericArithmetics.sine(arguments);
    }

    public static Iota cosine(List<Iota> arguments) throws CastingException {
        return NumericArithmetics.cosine(arguments);
    }

    public static Iota tangent(List<Iota> arguments) throws CastingException {
        return NumericArithmetics.tangent(arguments);
    }

    public static Iota arcsine(List<Iota> arguments) throws CastingException {
        return NumericArithmetics.arcsine(arguments);
    }

    public static Iota arccosine(List<Iota> arguments) throws CastingException {
        return NumericArithmetics.arccosine(arguments);
    }

    public static Iota arctangent(List<Iota> arguments) throws CastingException {
        return NumericArithmetics.arctangent(arguments);
    }

    public static Iota arctangent2(List<Iota> arguments) throws CastingException {
        return NumericArithmetics.arctangent2(arguments);
    }

    public static Iota logarithm(List<Iota> arguments) throws CastingException {
        return NumericArithmetics.logarithm(arguments);
    }

    public static Iota multiply(List<Iota> arguments) throws CastingException {
        requireBinary(arguments, "multiply");
        Iota left = arguments.get(0);
        Iota right = arguments.get(1);
        if (left instanceof Vec3Iota || right instanceof Vec3Iota) {
            if (left instanceof Vec3Iota && right instanceof Vec3Iota) {
                Vec3d leftVector = ((Vec3Iota) left).getValue();
                Vec3d rightVector = ((Vec3Iota) right).getValue();
                return new DoubleIota(leftVector.dotProduct(rightVector));
            }
            return vectorComponentwise(arguments, "multiply", (a, b) -> a * b);
        }
        return NumericArithmetics.multiply(arguments);
    }

    public static Iota divide(List<Iota> arguments) throws CastingException {
        requireBinary(arguments, "divide");
        Iota left = arguments.get(0);
        Iota right = arguments.get(1);
        if (left instanceof Vec3Iota || right instanceof Vec3Iota) {
            if (left instanceof Vec3Iota && right instanceof Vec3Iota) {
                return new Vec3Iota(((Vec3Iota) left).getValue()
                    .crossProduct(((Vec3Iota) right).getValue()));
            }
            return vectorComponentwise(arguments, "divide", (a, b) -> a / b);
        }
        return NumericArithmetics.divide(arguments);
    }

    public static Iota modulo(List<Iota> arguments) throws CastingException {
        requireBinary(arguments, "modulo");
            if (arguments.get(0) instanceof Vec3Iota || arguments.get(1) instanceof Vec3Iota) {
            return vectorComponentwise(arguments, "modulo", (left, right) -> left % right);
        }
        return NumericArithmetics.modulo(arguments);
    }

    public static Iota power(List<Iota> arguments) throws CastingException {
        requireBinary(arguments, "power");
        Iota left = arguments.get(0);
        Iota right = arguments.get(1);
        if (left instanceof Vec3Iota || right instanceof Vec3Iota) {
            if (!(left instanceof Vec3Iota) || !(right instanceof Vec3Iota)) {
                return vectorComponentwise(arguments, "power", (a, b) -> {
                    if (a < 0.0D && Math.abs(b - Math.rint(b)) > 1.0E-5D) {
                        throw Mishap.divideByZero(left, right, "exponent");
                    }
                    return Math.pow(a, b);
                });
            }
            Vec3d base = ((Vec3Iota) left).getValue();
            Vec3d direction = ((Vec3Iota) right).getValue();
            Vec3d normalized = direction.normalize();
            return new Vec3Iota(normalized.scale(base.dotProduct(normalized)));
        }
        return NumericArithmetics.power(arguments);
    }

    public static Iota absolute(List<Iota> arguments) throws CastingException {
        if (arguments.size() == 1 && arguments.get(0) instanceof Vec3Iota) {
            return new DoubleIota(((Vec3Iota) arguments.get(0)).getValue().lengthVector());
        }
        if (arguments.size() == 1 && arguments.get(0) instanceof ListIota) {
            return new DoubleIota(((ListIota) arguments.get(0)).getItems().size());
        }
        if (arguments.size() == 1 && arguments.get(0) instanceof BooleanIota) {
            return new DoubleIota(((BooleanIota) arguments.get(0)).getValue() ? 1.0D : 0.0D);
        }
        return NumericArithmetics.absolute(arguments);
    }

    public static Iota floor(List<Iota> arguments) throws CastingException {
        if (arguments.size() == 1 && arguments.get(0) instanceof Vec3Iota) {
            Vec3d value = ((Vec3Iota) arguments.get(0)).getValue();
            return new Vec3Iota(new Vec3d(Math.floor(value.x), Math.floor(value.y), Math.floor(value.z)));
        }
        return NumericArithmetics.floor(arguments);
    }

    public static Iota ceil(List<Iota> arguments) throws CastingException {
        if (arguments.size() == 1 && arguments.get(0) instanceof Vec3Iota) {
            Vec3d value = ((Vec3Iota) arguments.get(0)).getValue();
            return new Vec3Iota(new Vec3d(Math.ceil(value.x), Math.ceil(value.y), Math.ceil(value.z)));
        }
        return NumericArithmetics.ceil(arguments);
    }

    private interface ComponentOperation {
        double apply(double left, double right) throws CastingException;
    }

    private static Iota vectorComponentwise(List<Iota> arguments, String name,
        ComponentOperation operation) throws CastingException {
        Iota left = arguments.get(0);
        Iota right = arguments.get(1);
        if (!(left instanceof Vec3Iota || left instanceof DoubleIota)
            || !(right instanceof Vec3Iota || right instanceof DoubleIota)) {
            throw Mishap.invalidOperatorArgs(arguments,
                name + " received incompatible overloaded arguments");
        }
        Vec3d a = left instanceof Vec3Iota
            ? ((Vec3Iota) left).getValue()
            : new Vec3d(((DoubleIota) left).getValue(),
                ((DoubleIota) left).getValue(), ((DoubleIota) left).getValue());
        Vec3d b = right instanceof Vec3Iota
            ? ((Vec3Iota) right).getValue()
            : new Vec3d(((DoubleIota) right).getValue(),
                ((DoubleIota) right).getValue(), ((DoubleIota) right).getValue());
        if (("divide".equals(name) || "modulo".equals(name))
            && (b.x == 0.0D || b.y == 0.0D || b.z == 0.0D)) {
            throw Mishap.divideByZero(left, right, "divide");
        }
        if ("power".equals(name)
            && ((a.x < 0.0D && Math.abs(b.x - Math.rint(b.x)) > 1.0E-5D)
                || (a.y < 0.0D && Math.abs(b.y - Math.rint(b.y)) > 1.0E-5D)
                || (a.z < 0.0D && Math.abs(b.z - Math.rint(b.z)) > 1.0E-5D))) {
            throw Mishap.divideByZero(left, right, "exponent");
        }
        return new Vec3Iota(new Vec3d(
            operation.apply(a.x, b.x), operation.apply(a.y, b.y), operation.apply(a.z, b.z)));
    }

    private static void requireBinary(List<Iota> arguments, String name)
        throws CastingException {
        if (arguments.size() != 2) {
            throw Mishap.invalidValue("hexcasting.error.arithmetic_arity",
                name + " expects exactly two arguments");
        }
    }
}
