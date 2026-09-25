package at.petra_k.hexcasting.common.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

/** Small 1.12.2 Forge bridge for server-side Hex settings. */
public final class HexConfig {
    private static final int DEFAULT_BREAK_HARVEST_LEVEL = 3;
    private static final int MIN_BREAK_HARVEST_LEVEL = 0;
    private static final int MAX_BREAK_HARVEST_LEVEL = 4;

    private static int breakHarvestLevel = DEFAULT_BREAK_HARVEST_LEVEL;

    private HexConfig() {
    }

    public static void load(File file) {
        if (file == null) {
            breakHarvestLevel = DEFAULT_BREAK_HARVEST_LEVEL;
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
        } finally {
            if (configuration.hasChanged()) {
                configuration.save();
            }
        }
    }

    public static int opBreakHarvestLevel() {
        return breakHarvestLevel;
    }
}
