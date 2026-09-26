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
    private static final double DEFAULT_GLOBAL_COST_SCALING = 1.0D;
    private static final String[] DEFAULT_TELEPORT_DIMENSIONS =
        new String[] {"twilightforest:twilight_forest"};

    private static int breakHarvestLevel = DEFAULT_BREAK_HARVEST_LEVEL;
    private static double globalCostScaling = DEFAULT_GLOBAL_COST_SCALING;
    private static Map<ResourceLocation, Double> actionCostScaling = Collections.emptyMap();
    private static Set<String> teleportDimensionDenylist = Collections.emptySet();

    private HexConfig() {
    }

    public static void load(File file) {
        if (file == null) {
            breakHarvestLevel = DEFAULT_BREAK_HARVEST_LEVEL;
            globalCostScaling = DEFAULT_GLOBAL_COST_SCALING;
            actionCostScaling = Collections.emptyMap();
            teleportDimensionDenylist = Collections.emptySet();
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
        } finally {
            if (configuration.hasChanged()) {
                configuration.save();
            }
        }
    }

    public static int opBreakHarvestLevel() {
        return breakHarvestLevel;
    }

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
