package at.petra_k.hexcasting.common.lib.hex;

import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry mapping stable action ids and drawable patterns to actions. */
public final class HexActionRegistry {
    private static final Map<ResourceLocation, HexAction> BY_ID = new LinkedHashMap<>();
    private static final Map<HexPattern, HexAction> BY_PATTERN = new LinkedHashMap<>();
    private static final Map<ResourceLocation, HexPattern> PATTERN_BY_ID = new LinkedHashMap<>();

    private HexActionRegistry() {
    }

    public static HexAction register(ResourceLocation id, HexPattern pattern, HexAction action) {
        if (BY_ID.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate Hex action id: " + id);
        }
        if (BY_PATTERN.containsKey(pattern)) {
            throw new IllegalArgumentException("Duplicate Hex action pattern: " + pattern);
        }
        BY_ID.put(id, action);
        BY_PATTERN.put(pattern, action);
        PATTERN_BY_ID.put(id, pattern);
        return action;
    }

    public static HexAction get(ResourceLocation id) {
        return BY_ID.get(id);
    }

    public static HexAction get(HexPattern pattern) {
        return BY_PATTERN.get(pattern);
    }

    public static HexPattern getPattern(ResourceLocation id) {
        return PATTERN_BY_ID.get(id);
    }

    public static ResourceLocation idFor(HexAction action) {
        for (Map.Entry<ResourceLocation, HexAction> entry : BY_ID.entrySet()) {
            if (entry.getValue() == action) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static ResourceLocation firstId() {
        return BY_ID.isEmpty() ? null : BY_ID.keySet().iterator().next();
    }

    public static ResourceLocation nextId(ResourceLocation current) {
        List<ResourceLocation> ids = new ArrayList<>(BY_ID.keySet());
        if (ids.isEmpty()) {
            return null;
        }
        int index = ids.indexOf(current);
        return ids.get((index + 1 + ids.size()) % ids.size());
    }

    public static Map<ResourceLocation, HexAction> byId() {
        return Collections.unmodifiableMap(BY_ID);
    }

    public static Map<HexPattern, HexAction> byPattern() {
        return Collections.unmodifiableMap(BY_PATTERN);
    }

    public static void bootstrap() {
        HexActions.touch();
    }
}
