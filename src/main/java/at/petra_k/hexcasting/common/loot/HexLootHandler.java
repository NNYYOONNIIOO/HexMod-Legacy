package at.petra_k.hexcasting.common.loot;

import java.util.Random;

/** Shared loot calculations kept equivalent to the modern Hex loot helpers. */
public final class HexLootHandler {
    private HexLootHandler() {
    }

    /**
     * Return a non-negative value from the inclusive range [-range, range].
     * Kotlin's Random.nextInt(from, until) is inclusive on both ends in the
     * upstream helper, while java.util.Random only exposes a zero-based bound.
     */
    public static int getScrollCount(int range, Random random) {
        if (random == null || range <= 0) {
            return 0;
        }
        return Math.max(random.nextInt(range * 2 + 1) - range, 0);
    }
}
