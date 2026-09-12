package at.petra_k.hexcasting.interop.inline;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import com.samsthenerd.inline.api.InlineData;
import com.samsthenerd.inline.api.InlineDataType;
import net.minecraft.util.ResourceLocation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Inline-style typed pattern payload for the 1.12.2 port. */
public final class InlinePatternData implements InlineData<InlinePatternData> {
    public static final InlineDataType<InlinePatternData> TYPE =
        new InlineDataType<InlinePatternData>() {
            @Override
            public ResourceLocation getId() {
                return new ResourceLocation("hexcasting", "pattern");
            }

            @Override
            public JsonObject serialize(InlinePatternData data) {
                return encodePattern(data.pattern);
            }

            @Override
            public InlinePatternData deserialize(JsonObject data) {
                return new InlinePatternData(decodePattern(data));
            }
        };
    private final HexPattern pattern;

    public InlinePatternData(HexPattern pattern) {
        this.pattern = Objects.requireNonNull(pattern, "pattern");
    }

    public HexPattern getPattern() {
        return pattern;
    }

    @Override
    public InlineDataType<InlinePatternData> getType() {
        return TYPE;
    }

    @Override
    public ResourceLocation getRendererId() {
        return new ResourceLocation("hexcasting", "pattern");
    }

    @Override
    public InlinePatternData copy() {
        return new InlinePatternData(pattern);
    }

    public String asText() {
        return InlinePatternRenderer.render(pattern);
    }

    public String asPlainText() {
        return pattern.signature();
    }

    @Override
    public String toString() {
        return asText();
    }

    private static JsonObject encodePattern(HexPattern pattern) {
        JsonObject result = new JsonObject();
        Object start = invokeAccessor(pattern, "getStartDir");
        if (start == null) {
            start = invokeAccessor(pattern, "getStart");
        }
        if (start instanceof Enum<?>) {
            result.addProperty("start", ((Enum<?>) start).name());
        }

        Object angleValue = invokeAccessor(pattern, "getAngles");
        if (angleValue instanceof Iterable<?>) {
            JsonArray angles = new JsonArray();
            for (Object angle : (Iterable<?>) angleValue) {
                if (angle instanceof Enum<?>) {
                    angles.add(((Enum<?>) angle).name());
                } else if (angle != null) {
                    angles.add(angle.toString());
                }
            }
            result.add("angles", angles);
        }
        result.addProperty("signature", pattern.signature());
        return result;
    }

    private static HexPattern decodePattern(JsonObject data) {
        if (data.has("start") && data.has("angles")) {
            try {
                Object start = enumValue(
                    "at.petra_k.hexcasting.api.casting.math.HexDir",
                    data.get("start").getAsString());
                Class<?> angleClass = Class.forName(
                    "at.petra_k.hexcasting.api.casting.math.HexAngle");
                List<Object> angles = new ArrayList<>();
                for (JsonElement element : data.getAsJsonArray("angles")) {
                    angles.add(Enum.valueOf((Class) angleClass, element.getAsString()));
                }
                HexPattern decoded = invokePatternFactory(start, angles);
                if (decoded != null) {
                    return decoded;
                }
            } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
                // Fall through to the signature parser used by older payloads.
            }
        }

        if (data.has("signature")) {
            HexPattern decoded = parseSignature(data.get("signature").getAsString());
            if (decoded != null) {
                return decoded;
            }
        }
        throw new IllegalArgumentException("Cannot decode Hex pattern Inline payload");
    }

    private static Object invokeAccessor(Object target, String name) {
        try {
            Method method;
            try {
                method = target.getClass().getMethod(name);
            } catch (NoSuchMethodException ignored) {
                method = target.getClass().getDeclaredMethod(name);
                method.setAccessible(true);
            }
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object enumValue(String className, String name)
        throws ReflectiveOperationException {
        Class<?> enumClass = Class.forName(className);
        return Enum.valueOf((Class) enumClass, name);
    }

    private static HexPattern invokePatternFactory(Object start, List<Object> angles)
        throws ReflectiveOperationException {
        for (Method method : HexPattern.class.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())
                || !HexPattern.class.isAssignableFrom(method.getReturnType())
                || method.getParameterTypes().length != 2) {
                continue;
            }
            Object[] arguments = patternArguments(method.getParameterTypes(), start, angles);
            if (arguments == null) {
                continue;
            }
            method.setAccessible(true);
            return (HexPattern) method.invoke(null, arguments);
        }
        for (Constructor<?> constructor : HexPattern.class.getDeclaredConstructors()) {
            Object[] arguments = patternArguments(constructor.getParameterTypes(), start, angles);
            if (arguments == null) {
                continue;
            }
            constructor.setAccessible(true);
            return (HexPattern) constructor.newInstance(arguments);
        }
        return null;
    }

    private static Object[] patternArguments(Class<?>[] parameterTypes,
                                             Object start,
                                             List<Object> angles) {
        if (parameterTypes.length != 2) {
            return null;
        }
        Object[] result = new Object[2];
        boolean hasStart = false;
        boolean hasAngles = false;
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (!hasStart && start != null && parameterType.isInstance(start)) {
                result[i] = start;
                hasStart = true;
            } else if (!hasAngles && parameterType.isAssignableFrom(angles.getClass())) {
                result[i] = angles;
                hasAngles = true;
            } else {
                return null;
            }
        }
        return hasStart && hasAngles ? result : null;
    }

    private static HexPattern parseSignature(String signature) {
        for (String name : new String[] {"fromSignature", "parse", "fromString"}) {
            try {
                Method method = HexPattern.class.getDeclaredMethod(name, String.class);
                if (!Modifier.isStatic(method.getModifiers())
                    || !HexPattern.class.isAssignableFrom(method.getReturnType())) {
                    continue;
                }
                method.setAccessible(true);
                return (HexPattern) method.invoke(null, signature);
            } catch (ReflectiveOperationException ignored) {
                // Try the next legacy factory name.
            }
        }
        return null;
    }
}
