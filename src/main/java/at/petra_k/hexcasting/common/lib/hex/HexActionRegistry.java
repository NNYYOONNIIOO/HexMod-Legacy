package at.petra_k.hexcasting.common.lib.hex;

import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.math.HexAngle;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import at.petra_k.hexcasting.common.world.PerWorldPatternData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry mapping stable action ids and drawable patterns to actions. */
public final class HexActionRegistry {
    private static final Map<ResourceLocation, HexAction> BY_ID = new LinkedHashMap<>();
    private static final Map<HexPattern, HexAction> BY_PATTERN = new LinkedHashMap<>();
    /** Modern Hex resolves a drawn pattern by its relative turn sequence. */
    private static final Map<List<HexAngle>, HexAction> BY_SHAPE = new LinkedHashMap<>();
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
        BY_SHAPE.putIfAbsent(shapeOf(pattern), action);
        PATTERN_BY_ID.put(id, pattern);
        return action;
    }

    public static HexAction get(ResourceLocation id) {
        return BY_ID.get(id);
    }

    public static HexAction get(HexPattern pattern) {
        if (pattern == null) {
            return null;
        }
        HexAction exact = BY_PATTERN.get(pattern);
        return exact == null ? BY_SHAPE.get(shapeOf(pattern)) : exact;
    }

    /**
     * Resolve a pattern with the world context used by Hex's great spells.
     *
     * <p>The ordinary registry is intentionally checked first.  The fixed
     * prototype of a per-world action is retained as a compatibility fallback,
     * but in a loaded world the generated signature takes precedence over that
     * prototype.</p>
     */
    public static HexAction get(HexPattern pattern, World world) {
        if (pattern == null) {
            return null;
        }
        HexAction ordinary = get(pattern);
        ResourceLocation ordinaryId = idFor(ordinary);
        if (ordinary != null
            && !PerWorldPatternData.isPerWorldAction(ordinaryId)) {
            return ordinary;
        }

        ResourceLocation perWorldId = PerWorldPatternData.actionFor(world, pattern);
        if (perWorldId != null) {
            HexAction perWorld = get(perWorldId);
            if (perWorld != null) {
                return perWorld;
            }
        }
        return ordinary;
    }

    public static HexPattern getPattern(ResourceLocation id) {
        HexPattern pattern = PATTERN_BY_ID.get(id);
        if (pattern != null) {
            return pattern;
        }
        return getPattern(BY_ID.get(id));
    }

    /** Recover the pattern belonging to a registered action object. */
    public static HexPattern getPattern(HexAction action) {
        if (action == null) {
            return null;
        }
        for (Map.Entry<HexPattern, HexAction> entry : BY_PATTERN.entrySet()) {
            if (entry.getValue() == action) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Return a great spell's generated pattern when a world is available. */
    public static HexPattern getPattern(ResourceLocation id, World world) {
        if (id == null) {
            return null;
        }
        HexPattern worldPattern = PerWorldPatternData.patternFor(world, id);
        return worldPattern == null ? getPattern(id) : worldPattern;
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

    private static List<HexAngle> shapeOf(HexPattern pattern) {
        return Collections.unmodifiableList(new ArrayList<>(pattern.getAngles()));
    }

    public static void bootstrap() {
        HexActions.touch();
        // Sentinel actions live in their own source file because they also
        // own the 1.12.2 WorldSavedData implementation. Keep them in the
        // same bootstrap path as the core action table.
        SentinelActions.register();
    }
}
