package at.petra_k.hexcasting.common.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraft.util.ResourceLocation;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Small 1.12.2 Forge bridge for server-side Hex settings. */
public final class HexConfig {
    private static final int DEFAULT_BREAK_HARVEST_LEVEL = 3;
    private static final int MIN_BREAK_HARVEST_LEVEL = 0;
    private static final int MAX_BREAK_HARVEST_LEVEL = 4;
    public static final int DEFAULT_MAX_OPERATIONS = 100000;
    public static final int DEFAULT_MAX_CIRCLE_LENGTH = 1024;
    private static final double DEFAULT_GLOBAL_COST_SCALING = 1.0D;
    private static final double DEFAULT_GRID_ZOOM = 1.0D;
    private static final double DEFAULT_GRID_SNAP_THRESHOLD = 0.5D;
    private static final double DEFAULT_SCRY_SIGHT = 0.0D;
    private static final double DEFAULT_FEEBLE_MIND = 0.0D;
    private static final double DEFAULT_MEDIA_CONSUMPTION = 1.0D;
    private static final double DEFAULT_AMBIT_RADIUS = 32.0D;
    private static final double DEFAULT_SENTINEL_RADIUS = 16.0D;
    private static final String[] DEFAULT_TELEPORT_DIMENSIONS =
        new String[] {"twilightforest:twilight_forest"};

    private static int breakHarvestLevel = DEFAULT_BREAK_HARVEST_LEVEL;
    private static double globalCostScaling = DEFAULT_GLOBAL_COST_SCALING;
    private static Map<ResourceLocation, Double> actionCostScaling = Collections.emptyMap();
    private static Set<String> teleportDimensionDenylist = Collections.emptySet();
    private static double gridZoom = DEFAULT_GRID_ZOOM;
    private static double gridSnapThreshold = DEFAULT_GRID_SNAP_THRESHOLD;
    private static double scrySight = DEFAULT_SCRY_SIGHT;
    private static double feebleMind = DEFAULT_FEEBLE_MIND;
    private static double mediaConsumption = DEFAULT_MEDIA_CONSUMPTION;
    private static double ambitRadius = DEFAULT_AMBIT_RADIUS;
    private static double sentinelRadius = DEFAULT_SENTINEL_RADIUS;
    private static int maxOperations = DEFAULT_MAX_OPERATIONS;
    private static int maxCircleLength = DEFAULT_MAX_CIRCLE_LENGTH;

    private HexConfig() {
    }

    public static void load(File file) {
        if (file == null) {
            breakHarvestLevel = DEFAULT_BREAK_HARVEST_LEVEL;
            globalCostScaling = DEFAULT_GLOBAL_COST_SCALING;
            actionCostScaling = Collections.emptyMap();
            teleportDimensionDenylist = Collections.emptySet();
            resetAttributeDefaults();
            maxOperations = DEFAULT_MAX_OPERATIONS;
            maxCircleLength = DEFAULT_MAX_CIRCLE_LENGTH;
            return;
        }
        Configuration configuration = new Configuration(file);
        try {
            configuration.load();
            breakHarvestLevel = configuration.getInt(
                "opBreakHarvestLevel", "spells",
                DEFAULT_BREAK_HARVEST_LEVEL,
                MIN_BREAK_HARVEST_LEVEL, MAX_BREAK_HARVEST_LEVEL,
                "The harvest level of the Break Block spell. "
                    + "0 = wood, 1 = stone, 2 = iron, 3 = diamond, 4 = netherite.");
            globalCostScaling = readNonNegativeDouble(configuration.get(
                "spells", "globalCostScaling", "1.0",
                "Multiply media costs by this value."));
            actionCostScaling = readActionCostScaling(configuration.getStringList(
                "actionCostScaling", "spells", new String[0],
                "Entries use '<namespace:path> <multiplier>' or '<namespace:path>=<multiplier>'."));
            teleportDimensionDenylist = readTeleportDimensionDenylist(
                configuration.getStringList(
                    "teleportDimensionDenylist", "spells",
                    DEFAULT_TELEPORT_DIMENSIONS,
                    "Dimension ids or dimension type names where Blink and Greater Teleport are disabled."));
            gridZoom = readClampedDouble(configuration.get(
                "gridZoom", "attributes", 1.0D, "Base grid zoom multiplier.",
                0.5D, 4.0D), DEFAULT_GRID_ZOOM, 0.5D, 4.0D);
            gridSnapThreshold = readClampedDouble(configuration.get(
                "gridSnapThreshold", "client", DEFAULT_GRID_SNAP_THRESHOLD,
                "Distance from one staff grid point required to snap to the next point.",
                0.5D, 1.0D), DEFAULT_GRID_SNAP_THRESHOLD, 0.5D, 1.0D);
            scrySight = readClampedDouble(configuration.get(
                "scrySight", "attributes", 0.0D, "Base scrying sight value.",
                0.0D, 1.0D), DEFAULT_SCRY_SIGHT, 0.0D, 1.0D);
            feebleMind = readClampedDouble(configuration.get(
                "feebleMind", "attributes", 0.0D, "Base feeble mind value.",
                0.0D, 1.0D), DEFAULT_FEEBLE_MIND, 0.0D, 1.0D);
            mediaConsumption = readClampedDouble(configuration.get(
                "mediaConsumption", "attributes", 1.0D, "Base media consumption multiplier.",
                0.0D, Double.MAX_VALUE), DEFAULT_MEDIA_CONSUMPTION, 0.0D, Double.MAX_VALUE);
            ambitRadius = readClampedDouble(configuration.get(
                "ambitRadius", "attributes", 32.0D, "Base casting range.",
                0.0D, Double.MAX_VALUE), DEFAULT_AMBIT_RADIUS, 0.0D, Double.MAX_VALUE);
            sentinelRadius = readClampedDouble(configuration.get(
                "sentinelRadius", "attributes", 16.0D, "Base extended sentinel range.",
                0.0D, Double.MAX_VALUE), DEFAULT_SENTINEL_RADIUS, 0.0D, Double.MAX_VALUE);
            maxOperations = readClampedInt(configuration.get(
                "limits", "maxOperations", DEFAULT_MAX_OPERATIONS,
                "Maximum VM operations per evaluation.", 1, Integer.MAX_VALUE),
                DEFAULT_MAX_OPERATIONS, 1, Integer.MAX_VALUE);
            maxCircleLength = readClampedInt(configuration.get(
                "limits", "maxCircleLength", DEFAULT_MAX_CIRCLE_LENGTH,
                "Maximum visited components in a spell circle.", 1, Integer.MAX_VALUE),
                DEFAULT_MAX_CIRCLE_LENGTH, 1, Integer.MAX_VALUE);
        } finally {
            if (configuration.hasChanged()) {
                configuration.save();
            }
        }
    }

    public static int opBreakHarvestLevel() {
        return breakHarvestLevel;
    }

    public static double gridZoom() { return gridZoom; }
    public static double gridSnapThreshold() { return gridSnapThreshold; }
    public static double scrySight() { return scrySight; }
    public static double feebleMind() { return feebleMind; }
    public static double mediaConsumption() { return mediaConsumption; }
    public static double ambitRadius() { return ambitRadius; }
    public static double sentinelRadius() { return sentinelRadius; }
    public static int maxOperations() { return maxOperations; }
    public static int maxCircleLength() { return maxCircleLength; }

    /**
     * Return the configured media multiplier for one registered action.
     * Invalid or non-finite configuration values are treated as the default.
     */
    public static double mediaCostMultiplier(ResourceLocation actionId) {
        double action = actionId == null ? DEFAULT_GLOBAL_COST_SCALING
            : actionCostScaling.getOrDefault(actionId, DEFAULT_GLOBAL_COST_SCALING);
        double result = globalCostScaling * action;
        return Double.isNaN(result) || Double.isInfinite(result) || result < 0.0D
            ? DEFAULT_GLOBAL_COST_SCALING : result;
    }

    /**
     * Check the teleport denylist using the identifiers available in 1.12.2.
     * Forge dimensions are numeric at runtime, while their provider type still
     * exposes a stable human-readable name.
     */
    public static boolean canTeleportInDimension(
        int dimensionId, String dimensionName, String providerClassName) {
        if (teleportDimensionDenylist.contains(Integer.toString(dimensionId))) {
            return false;
        }
        String normalizedName = normalizeDimensionIdentifier(dimensionName);
        String normalizedProvider = normalizeDimensionIdentifier(providerClassName);
        String compactName = compactDimensionIdentifier(normalizedName);
        for (String denied : teleportDimensionDenylist) {
            if (denied.equals(normalizedName) || denied.equals(compactName)
                || (!normalizedProvider.isEmpty() && normalizedProvider.contains(denied))) {
                return false;
            }
            int separator = denied.indexOf(':');
            if (separator >= 0 && separator + 1 < denied.length()) {
                String path = denied.substring(separator + 1);
                if (path.equals(normalizedName)
                    || path.equals(compactName)
                    || (!normalizedProvider.isEmpty()
                        && normalizedProvider.contains(
                            compactDimensionIdentifier(path)))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static double readNonNegativeDouble(Property property) {
        if (property == null) {
            return DEFAULT_GLOBAL_COST_SCALING;
        }
        try {
            double value = Double.parseDouble(property.getString());
            return Double.isNaN(value) || Double.isInfinite(value) || value < 0.0D
                ? DEFAULT_GLOBAL_COST_SCALING : value;
        } catch (NumberFormatException ignored) {
            return DEFAULT_GLOBAL_COST_SCALING;
        }
    }

    private static double readClampedDouble(Property property, double fallback,
                                            double minimum, double maximum) {
        double value = readNonNegativeDouble(property);
        if (Double.isNaN(value) || Double.isInfinite(value)
            || value < minimum || value > maximum) {
            return fallback;
        }
        return value;
    }

    private static void resetAttributeDefaults() {
        gridZoom = DEFAULT_GRID_ZOOM;
        gridSnapThreshold = DEFAULT_GRID_SNAP_THRESHOLD;
        scrySight = DEFAULT_SCRY_SIGHT;
        feebleMind = DEFAULT_FEEBLE_MIND;
        mediaConsumption = DEFAULT_MEDIA_CONSUMPTION;
        ambitRadius = DEFAULT_AMBIT_RADIUS;
        sentinelRadius = DEFAULT_SENTINEL_RADIUS;
        maxOperations = DEFAULT_MAX_OPERATIONS;
        maxCircleLength = DEFAULT_MAX_CIRCLE_LENGTH;
    }

    private static int readClampedInt(Property property, int fallback,
                                      int minimum, int maximum) {
        if (property == null) {
            return fallback;
        }
        int value = property.getInt(fallback);
        return value < minimum || value > maximum ? fallback : value;
    }

    private static Map<ResourceLocation, Double> readActionCostScaling(String[] entries) {
        if (entries == null || entries.length == 0) {
            return Collections.emptyMap();
        }
        Map<ResourceLocation, Double> result = new LinkedHashMap<>();
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            String value = entry.trim();
            if (value.isEmpty()) {
                continue;
            }
            String[] parts = value.split("\\s*=\\s*", 2);
            if (parts.length != 2) {
                parts = value.split("\\s+", 2);
            }
            if (parts.length != 2) {
                continue;
            }
            try {
                ResourceLocation actionId = new ResourceLocation(parts[0].trim());
                double multiplier = Double.parseDouble(parts[1].trim());
                if (!Double.isNaN(multiplier) && !Double.isInfinite(multiplier)
                    && multiplier >= 0.0D) {
                    result.put(actionId, multiplier);
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed optional entries and keep valid mappings.
            }
        }
        return result.isEmpty() ? Collections.emptyMap() : result;
    }

    private static Set<String> readTeleportDimensionDenylist(String[] entries) {
        if (entries == null || entries.length == 0) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>();
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            String normalized = normalizeDimensionIdentifier(entry);
            if (!normalized.isEmpty()) {
                result.add(normalized);
            }
        }
        return result.isEmpty()
            ? Collections.emptySet() : Collections.unmodifiableSet(result);
    }

    private static String normalizeDimensionIdentifier(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
            .replace(' ', '_');
    }

    private static String compactDimensionIdentifier(String value) {
        return value == null ? "" : value.replaceAll("[^a-z0-9]", "");
    }
}
