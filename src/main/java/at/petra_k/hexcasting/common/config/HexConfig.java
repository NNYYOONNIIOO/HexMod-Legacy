package at.petra_k.hexcasting.common.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraft.util.ResourceLocation;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small 1.12.2 Forge bridge for server-side Hex settings. */
public final class HexConfig {
    private static final int DEFAULT_BREAK_HARVEST_LEVEL = 3;
    private static final int MIN_BREAK_HARVEST_LEVEL = 0;
    private static final int MAX_BREAK_HARVEST_LEVEL = 4;
    private static final double DEFAULT_GLOBAL_COST_SCALING = 1.0D;

    private static int breakHarvestLevel = DEFAULT_BREAK_HARVEST_LEVEL;
    private static double globalCostScaling = DEFAULT_GLOBAL_COST_SCALING;
    private static Map<ResourceLocation, Double> actionCostScaling = Collections.emptyMap();

    private HexConfig() {
    }

    public static void load(File file) {
        if (file == null) {
            breakHarvestLevel = DEFAULT_BREAK_HARVEST_LEVEL;
            globalCostScaling = DEFAULT_GLOBAL_COST_SCALING;
            actionCostScaling = Collections.emptyMap();
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
}
