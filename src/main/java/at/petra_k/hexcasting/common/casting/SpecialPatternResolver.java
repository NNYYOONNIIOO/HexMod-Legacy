package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.iota.DoubleIota;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.math.HexAngle;
import at.petra_k.hexcasting.api.casting.math.HexDir;
import at.petra_k.hexcasting.api.casting.math.HexPattern;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves Hex's two shape-based actions which do not have one fixed pattern
 * in the action registry.
 *
 * <p>Modern Hex checks the normal action table first and then asks special
 * handlers about the remaining shape.  Number literals and Bookkeeper's
 * Gambit are the two built-in handlers.  Keeping this resolver separate from
 * the registry preserves that precedence while making the behavior available
 * to the 1.12.2 VM.</p>
 */
public final class SpecialPatternResolver {
    private static final int MAX_HANDLER_ANGLES = 1024;

    private SpecialPatternResolver() {
    }

    public enum Kind {
        NUMBER,
        MASK
    }

    public static Match match(HexPattern pattern) {
        if (pattern == null || pattern.getAngles().size() > MAX_HANDLER_ANGLES) {
            return null;
        }

        Match number = matchNumber(pattern);
        if (number != null) {
            return number;
        }
        return matchMask(pattern);
    }

    public static boolean isSpecial(HexPattern pattern) {
        return match(pattern) != null;
    }

    private static Match matchNumber(HexPattern pattern) {
        String signature = pattern.anglesSignature();
        boolean negative;
        if (signature.startsWith("aqaa")) {
            negative = false;
        } else if (signature.startsWith("dedd")) {
            negative = true;
        } else {
            return null;
        }

        double value = 0.0D;
        for (int i = 4; i < signature.length(); i++) {
            switch (signature.charAt(i)) {
                case 'w':
                    value += 1.0D;
                    break;
                case 'q':
                    value += 5.0D;
                    break;
                case 'e':
                    value += 10.0D;
                    break;
                case 'a':
                    value *= 2.0D;
                    break;
                case 'd':
                    value /= 2.0D;
                    break;
                case 's':
                    // The BACK turn is a legal no-op in the number alphabet.
                    break;
                default:
                    return null;
            }
        }
        return Match.number(negative ? -value : value);
    }

    private static Match matchMask(HexPattern pattern) {
        List<HexDir> directions = pattern.directions();
        if (directions.isEmpty()) {
            return null;
        }

        HexDir flatDirection = pattern.getStartDir();
        List<HexAngle> angles = pattern.getAngles();
        if (!angles.isEmpty() && angles.get(0) == HexAngle.LEFT_BACK) {
            flatDirection = directions.get(0).rotatedBy(HexAngle.LEFT);
        }

        ArrayList<Boolean> mask = new ArrayList<>();
        int index = 0;
        while (index < directions.size()) {
            HexAngle angle = directions.get(index).angleFrom(flatDirection);
            if (angle == HexAngle.FORWARD) {
                mask.add(Boolean.TRUE);
                index++;
                continue;
            }
            if (index >= directions.size() - 1) {
                return null;
            }
            HexAngle next = directions.get(index + 1).angleFrom(flatDirection);
            if (angle == HexAngle.RIGHT && next == HexAngle.LEFT) {
                mask.add(Boolean.FALSE);
                index += 2;
                continue;
            }
            return null;
        }

        boolean[] result = new boolean[mask.size()];
        for (int i = 0; i < mask.size(); i++) {
            result[i] = mask.get(i);
        }
        return Match.mask(result);
    }

    public static final class Match {
        private final Kind kind;
        private final double number;
        private final boolean[] mask;

        private Match(Kind kind, double number, boolean[] mask) {
            this.kind = kind;
            this.number = number;
            this.mask = mask;
        }

        private static Match number(double value) {
            return new Match(Kind.NUMBER, value, null);
        }

        private static Match mask(boolean[] value) {
            return new Match(Kind.MASK, 0.0D, value);
        }

        public Kind getKind() {
            return kind;
        }

        public double getNumber() {
            return number;
        }

        public int getArgumentCount() {
            return mask == null ? 0 : mask.length;
        }

        public String maskSignature() {
            if (mask == null) {
                return "";
            }
            StringBuilder result = new StringBuilder(mask.length);
            for (boolean include : mask) {
                result.append(include ? '-' : 'v');
            }
            return result.toString();
        }

        /** Execute the dynamically resolved action transactionally. */
        public void execute(CastingStack stack) throws CastingException {
            if (stack == null) {
                throw new CastingException("Casting stack cannot be null");
            }
            if (kind == Kind.NUMBER) {
                stack.push(new DoubleIota(number));
                return;
            }

            List<Iota> before = stack.snapshot();
            try {
                if (stack.size() < mask.length) {
                    throw new CastingException("Not enough Iotas on the casting stack");
                }
                ArrayList<Iota> arguments = new ArrayList<>(mask.length);
                for (int i = 0; i < mask.length; i++) {
                    arguments.add(0, stack.pop());
                }
                for (int i = 0; i < mask.length; i++) {
                    if (mask[i]) {
                        stack.push(arguments.get(i));
                    }
                }
            } catch (CastingException exception) {
                stack.restore(before);
                throw exception;
            } catch (RuntimeException exception) {
                stack.restore(before);
                throw exception;
            }
        }
    }
}
