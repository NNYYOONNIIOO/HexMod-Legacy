package at.petra_k.hexcasting.api.casting.math;

import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A sequence of relative turns describing one hex pattern.
 *
 * <p>The geometry and the NBT shape intentionally stay close to the modern
 * Hex Casting API so that migrated actions and network messages can share the
 * same conceptual representation.</p>
 */
public final class HexPattern {
    public static final String TAG_START_DIR = "start_dir";
    public static final String TAG_ANGLES = "angles";

    private final HexDir startDir;
    private final ArrayList<HexAngle> angles;

    public HexPattern(HexDir startDir) {
        this(startDir, Collections.<HexAngle>emptyList());
    }

    public HexPattern(HexDir startDir, List<HexAngle> angles) {
        this.startDir = Objects.requireNonNull(startDir, "startDir");
        this.angles = new ArrayList<>(angles);
    }

    /** Parse Hex Casting keyboard notation (w/e/d/s/a/q) into a pattern. */
    public static HexPattern fromAngles(String signature, HexDir startDir) {
        Objects.requireNonNull(signature, "signature");
        Objects.requireNonNull(startDir, "startDir");
        ArrayList<HexAngle> parsed = new ArrayList<>(signature.length());
        for (int i = 0; i < signature.length(); i++) {
            switch (signature.charAt(i)) {
                case 'w':
                    parsed.add(HexAngle.FORWARD);
                    break;
                case 'e':
                    parsed.add(HexAngle.RIGHT);
                    break;
                case 'd':
                    parsed.add(HexAngle.RIGHT_BACK);
                    break;
                case 's':
                    parsed.add(HexAngle.BACK);
                    break;
                case 'a':
                    parsed.add(HexAngle.LEFT_BACK);
                    break;
                case 'q':
                    parsed.add(HexAngle.LEFT);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown Hex angle character: "
                        + signature.charAt(i));
            }
        }
        return new HexPattern(startDir, parsed);
    }

    /** Parse the serialized human-readable form produced by toString(). */
    public static HexPattern fromSignature(String signature) {
        if (signature == null) {
            throw new IllegalArgumentException("Pattern signature cannot be null");
        }
        String normalized = signature.trim();
        if (normalized.startsWith("HexPattern(") && normalized.endsWith(")")) {
            normalized = normalized.substring("HexPattern(".length(), normalized.length() - 1);
        }
        String[] parts = normalized.split("/");
        if (parts.length == 0 || parts[0].isEmpty()) {
            throw new IllegalArgumentException("Pattern signature is empty");
        }
        HexDir start = HexDir.fromString(parts[0]);
        ArrayList<HexAngle> parsed = new ArrayList<>(Math.max(0, parts.length - 1));
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            parsed.add(HexAngle.valueOf(parts[i].trim().toUpperCase()));
        }
        return new HexPattern(start, parsed);
    }

    public HexDir getStartDir() {
        return startDir;
    }

    /** Returns the mutable angle sequence used while drawing a pattern. */
    public ArrayList<HexAngle> getAngles() {
        return angles;
    }

    /**
     * Appends a direction unless it would backtrack or reuse an existing line.
     */
    public boolean tryAppendDir(HexDir newDir) {
        ArrayList<Line> linesSeen = new ArrayList<>();

        HexDir compass = startDir;
        HexCoord cursor = HexCoord.Origin;
        for (HexAngle angle : angles) {
            linesSeen.add(new Line(cursor, compass));
            linesSeen.add(new Line(cursor.plus(compass), compass.rotatedBy(HexAngle.BACK)));
            cursor = cursor.plus(compass);
            compass = compass.times(angle);
        }
        cursor = cursor.plus(compass);

        Line potentialNewLine = new Line(cursor, newDir);
        if (linesSeen.contains(potentialNewLine)) {
            return false;
        }
        HexAngle nextAngle = newDir.minus(compass);
        if (nextAngle == HexAngle.BACK) {
            return false;
        }

        angles.add(nextAngle);
        return true;
    }

    public List<HexCoord> positions() {
        return positions(HexCoord.Origin);
    }

    public List<HexCoord> positions(HexCoord start) {
        ArrayList<HexCoord> out = new ArrayList<>(angles.size() + 2);
        out.add(start);
        HexDir compass = startDir;
        HexCoord cursor = start;
        for (HexAngle angle : angles) {
            cursor = cursor.plus(compass);
            out.add(cursor);
            compass = compass.times(angle);
        }
        out.add(cursor.plus(compass));
        return out;
    }

    public List<HexDir> directions() {
        ArrayList<HexDir> out = new ArrayList<>(angles.size() + 1);
        out.add(startDir);
        HexDir compass = startDir;
        for (HexAngle angle : angles) {
            compass = compass.times(angle);
            out.add(compass);
        }
        return out;
    }

    public HexDir finalDir() {
        HexDir out = startDir;
        for (HexAngle angle : angles) {
            out = out.times(angle);
        }
        return out;
    }

    /** A stable human-readable signature useful in logs and registry diagnostics. */
    public String signature() {
        StringBuilder out = new StringBuilder(angles.size() + 1);
        out.append(startDir.name());
        for (HexAngle angle : angles) {
            out.append('/').append(angle.name());
        }
        return out.toString();
    }

    /** The compact relative-angle notation used by Hex in tooltips and errors. */
    public String anglesSignature() {
        final char[] symbols = {'w', 'e', 'd', 's', 'a', 'q'};
        StringBuilder out = new StringBuilder(angles.size());
        for (HexAngle angle : angles) {
            int ordinal = angle.ordinal();
            if (ordinal >= 0 && ordinal < symbols.length) {
                out.append(symbols[ordinal]);
            }
        }
        return out.toString();
    }

    public NBTTagCompound serializeToNBT() {
        NBTTagCompound out = new NBTTagCompound();
        out.setByte(TAG_START_DIR, (byte) startDir.ordinal());
        byte[] serializedAngles = new byte[angles.size()];
        for (int i = 0; i < angles.size(); i++) {
            serializedAngles[i] = (byte) angles.get(i).ordinal();
        }
        out.setByteArray(TAG_ANGLES, serializedAngles);
        return out;
    }

    public static HexPattern fromNBT(NBTTagCompound nbt) {
        HexDir start = HexDir.values()[Byte.toUnsignedInt(nbt.getByte(TAG_START_DIR)) % HexDir.values().length];
        byte[] serializedAngles = nbt.getByteArray(TAG_ANGLES);
        ArrayList<HexAngle> angles = new ArrayList<>(serializedAngles.length);
        for (byte serializedAngle : serializedAngles) {
            angles.add(HexAngle.fromOrdinal(Byte.toUnsignedInt(serializedAngle)));
        }
        return new HexPattern(start, angles);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof HexPattern)) {
            return false;
        }
        HexPattern pattern = (HexPattern) other;
        return startDir == pattern.startDir && angles.equals(pattern.angles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startDir, angles);
    }

    @Override
    public String toString() {
        return "HexPattern(" + signature() + ")";
    }

    private static final class Line {
        private final HexCoord origin;
        private final HexDir direction;

        private Line(HexCoord origin, HexDir direction) {
            this.origin = origin;
            this.direction = direction;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Line)) {
                return false;
            }
            Line line = (Line) other;
            return origin.equals(line.origin) && direction == line.direction;
        }

        @Override
        public int hashCode() {
            return Objects.hash(origin, direction);
        }
    }
}
