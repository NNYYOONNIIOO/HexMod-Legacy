package at.petra_k.hexcasting.api.advancements;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Compact inclusive bounds parser for legacy advancement criterion JSON. */
final class HexAdvancementBounds {
    private final double minimum;
    private final double maximum;

    private HexAdvancementBounds(double minimum, double maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
    }

    static HexAdvancementBounds any() {
        return new HexAdvancementBounds(Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY);
    }

    static HexAdvancementBounds read(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return any();
        }
        try {
            if (element.isJsonPrimitive()) {
                double value = element.getAsDouble();
                return new HexAdvancementBounds(value, value);
            }
            JsonObject object = element.getAsJsonObject();
            double minimum = object.has("min") ? object.get("min").getAsDouble()
                : Double.NEGATIVE_INFINITY;
            double maximum = object.has("max") ? object.get("max").getAsDouble()
                : Double.POSITIVE_INFINITY;
            return new HexAdvancementBounds(minimum, maximum);
        } catch (RuntimeException ignored) {
            return any();
        }
    }

    boolean matches(double value) {
        return value >= minimum && value <= maximum;
    }
}
