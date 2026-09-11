package at.petra_k.hexcasting.api.casting.math;

/** The six absolute directions of the hex grid. */
public enum HexDir {
    NORTH_EAST,
    EAST,
    SOUTH_EAST,
    SOUTH_WEST,
    WEST,
    NORTH_WEST;

    public HexDir rotatedBy(HexAngle angle) {
        return values()[mod(ordinal() + angle.ordinal(), values().length)];
    }

    /** Kotlin's {@code operator fun times(HexAngle)} equivalent. */
    public HexDir times(HexAngle angle) {
        return rotatedBy(angle);
    }

    public HexAngle angleFrom(HexDir other) {
        return HexAngle.fromOrdinal(ordinal() - other.ordinal());
    }

    /** Kotlin's {@code operator fun minus(HexDir)} equivalent. */
    public HexAngle minus(HexDir other) {
        return angleFrom(other);
    }

    public HexCoord asDelta() {
        switch (this) {
            case NORTH_EAST:
                return new HexCoord(1, -1);
            case EAST:
                return new HexCoord(1, 0);
            case SOUTH_EAST:
                return new HexCoord(0, 1);
            case SOUTH_WEST:
                return new HexCoord(-1, 1);
            case WEST:
                return new HexCoord(-1, 0);
            case NORTH_WEST:
                return new HexCoord(0, -1);
            default:
                throw new AssertionError(this);
        }
    }

    public static HexDir fromString(String key) {
        if (key == null) {
            return WEST;
        }
        String normalized = key.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        for (HexDir value : values()) {
            if (value.name().equals(normalized)) {
                return value;
            }
        }
        return WEST;
    }

    private static int mod(int value, int divisor) {
        int result = value % divisor;
        return result < 0 ? result + divisor : result;
    }
}
