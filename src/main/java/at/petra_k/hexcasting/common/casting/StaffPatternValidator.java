package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.casting.math.HexDir;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.item.ItemHexStaff;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.HashSet;
import java.util.Set;

/** Server-side validation for the complete layout submitted by the staff GUI. */
public final class StaffPatternValidator {
    private static final String KEY_ORIGIN_Q = "origin_q";
    private static final String KEY_ORIGIN_R = "origin_r";

    private StaffPatternValidator() {
    }

    /**
     * Returns a localization key when the submitted layout is invalid, or
     * {@code null} when it is a valid Hex-style set of resolved patterns.
     * Points may repeat inside one closed pattern; they may not overlap a
     * point used by an earlier pattern, matching modern StaffCastEnv.
     */
    public static String validate(NBTTagList patterns) {
        if (patterns == null || patterns.tagCount() > ItemHexStaff.MAX_PROGRAM_SIZE) {
            return "hexcasting.message.program_full";
        }

        Set<GridPoint> previousPoints = new HashSet<>();
        for (int i = 0; i < patterns.tagCount(); i++) {
            final NBTTagCompound entry;
            final HexPattern pattern;
            try {
                entry = patterns.getCompoundTagAt(i);
                pattern = HexPattern.fromNBT(entry);
            } catch (RuntimeException ignored) {
                return "hexcasting.message.pattern_invalid";
            }

            Set<GridPoint> currentPoints = new HashSet<>();
            GridPoint cursor = new GridPoint(
                entry.getInteger(KEY_ORIGIN_Q), entry.getInteger(KEY_ORIGIN_R));
            currentPoints.add(cursor);
            for (HexDir direction : pattern.directions()) {
                cursor = cursor.add(direction);
                currentPoints.add(cursor);
            }
            for (GridPoint point : currentPoints) {
                if (previousPoints.contains(point)) {
                    return "hexcasting.message.pattern_overlap";
                }
            }
            previousPoints.addAll(currentPoints);
        }
        return null;
    }

    private static final class GridPoint {
        private final int q;
        private final int r;

        private GridPoint(int q, int r) {
            this.q = q;
            this.r = r;
        }

        private GridPoint add(HexDir direction) {
            switch (direction) {
                case NORTH_EAST: return new GridPoint(q + 1, r - 1);
                case EAST: return new GridPoint(q + 1, r);
                case SOUTH_EAST: return new GridPoint(q, r + 1);
                case SOUTH_WEST: return new GridPoint(q - 1, r + 1);
                case WEST: return new GridPoint(q - 1, r);
                case NORTH_WEST: return new GridPoint(q, r - 1);
                default: throw new AssertionError(direction);
            }
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof GridPoint
                && q == ((GridPoint) other).q && r == ((GridPoint) other).r;
        }

        @Override
        public int hashCode() {
            return 31 * q + r;
        }
    }
}
