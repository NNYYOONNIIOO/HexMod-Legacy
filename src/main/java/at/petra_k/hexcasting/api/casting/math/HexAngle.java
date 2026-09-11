package at.petra_k.hexcasting.api.casting.math;

/**
 * Relative turns used by a hex pattern.
 *
 * <p>This is the 1.12.2 Java adaptation of Hex Casting's platform-independent
 * pattern mathematics. The ordinal order is part of the serialized format,
 * so it must remain stable.</p>
 */
public enum HexAngle {
    FORWARD,
    RIGHT,
    RIGHT_BACK,
    BACK,
    LEFT_BACK,
    LEFT;

    public static HexAngle fromOrdinal(int ordinal) {
        HexAngle[] values = values();
        int normalized = ordinal % values.length;
        if (normalized < 0) {
            normalized += values.length;
        }
        return values[normalized];
    }

    public HexAngle opposite() {
        return fromOrdinal(ordinal() + BACK.ordinal());
    }
}
