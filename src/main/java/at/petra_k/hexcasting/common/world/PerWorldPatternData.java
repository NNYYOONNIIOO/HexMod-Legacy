package at.petra_k.hexcasting.common.world;

import at.petra_k.hexcasting.api.casting.math.HexAngle;
import at.petra_k.hexcasting.api.casting.math.HexCoord;
import at.petra_k.hexcasting.api.casting.math.HexDir;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The world-specific stroke order table used by Hex's great spells.
 *
 * <p>Modern Hex deliberately does not use the prototype stroke order for most
 * great spells.  It Euler-walks the prototype's line graph with a seeded
 * Kotlin {@code XorWowRandom}, then stores the result in the world.  Keeping
 * the generated table in {@link WorldSavedData} is important: a future change
 * to the generator must not silently change the spells in an existing world.</p>
 */
public final class PerWorldPatternData extends WorldSavedData {
    private static final String DATA_NAME = "hexcasting_per_world_patterns";
    private static final String DATA_VERSION = "1";
    private static final String KEY_VERSION = "version";
    private static final String KEY_ENTRIES = "entries";
    private static final String KEY_SIGNATURE = "signature";
    private static final String KEY_ACTION = "action";
    private static final String KEY_START_DIR = "start_dir";

    /** This is the same list tagged PER_WORLD_PATTERN by Hex 1.20.1. */
    private static final List<ResourceLocation> PER_WORLD_ACTIONS;

    /** Client-side copy sent by the server; a multiplayer client must not use its hidden seed. */
    private static final Map<String, Entry> CLIENT_ENTRIES = new LinkedHashMap<>();

    private final Map<String, Entry> bySignature = new LinkedHashMap<>();
    private final Map<ResourceLocation, Entry> byAction = new LinkedHashMap<>();

    static {
        ArrayList<ResourceLocation> ids = new ArrayList<>();
        for (String id : new String[] {
            "lightning",
            "flight",
            "create_lava",
            "teleport/great",
            "sentinel/create/great",
            "dispel_rain",
            "summon_rain",
            "brainsweep",
            "craft/battery",
            "potion/regeneration",
            "potion/night_vision",
            "potion/absorption",
            "potion/haste",
            "potion/strength"
        }) {
            ids.add(new ResourceLocation("hexcasting", id));
        }
        ids.sort(Comparator.comparing(ResourceLocation::toString));
        PER_WORLD_ACTIONS = Collections.unmodifiableList(ids);
    }

    public PerWorldPatternData() {
        super(DATA_NAME);
    }

    public PerWorldPatternData(String name) {
        super(name);
    }

    /** Return the persistent table for a server world, creating missing entries when needed. */
    public static synchronized PerWorldPatternData get(World world) {
        if (world == null || world.isRemote) {
            return null;
        }
        MapStorage storage = world.getPerWorldStorage();
        PerWorldPatternData data = (PerWorldPatternData) storage.getOrLoadData(
            PerWorldPatternData.class, DATA_NAME);
        if (data == null) {
            data = createFromScratch(world.getSeed());
            storage.setData(DATA_NAME, data);
        } else {
            data.ensureEntries(world.getSeed());
        }
        return data;
    }

    /** Return the action ids that need an entry in the scroll creative tab. */
    public static List<ResourceLocation> perWorldActionIds() {
        return PER_WORLD_ACTIONS;
    }

    public static boolean isPerWorldAction(ResourceLocation action) {
        return action != null && PER_WORLD_ACTIONS.contains(action);
    }

    /** Look up a world-specific action by the compact angle signature. */
    public static synchronized ResourceLocation actionFor(World world, String signature) {
        if (signature == null) {
            return null;
        }
        Entry entry;
        if (world != null && world.isRemote) {
            entry = CLIENT_ENTRIES.get(signature);
        } else {
            PerWorldPatternData data = get(world);
            entry = data == null ? null : data.bySignature.get(signature);
        }
        return entry == null ? null : entry.action;
    }

    /**
     * Resolve both the canonical per-world stroke order and the shape shown in
     * the guide.  Hex's guide intentionally hides stroke order for great
     * spells: it displays the prototype shape, while the world stores one
     * Euler traversal of that same line graph.  Matching only the compact turn
     * signature therefore made the guide drawing report "no action" even
     * though it was the correct great-spell shape.
     */
    public static synchronized ResourceLocation actionFor(World world, HexPattern pattern) {
        if (pattern == null) {
            return null;
        }
        ResourceLocation exact = actionFor(world, pattern.anglesSignature());
        if (exact != null) {
            return exact;
        }

        String shape = shapeKey(pattern);
        if (shape.isEmpty()) {
            return null;
        }
        for (ResourceLocation action : PER_WORLD_ACTIONS) {
            HexPattern prototype = HexActionRegistry.getPattern(action);
            if (prototype != null && shape.equals(shapeKey(prototype))) {
                return action;
            }
        }
        return null;
    }

    /** Return the canonical, world-specific pattern for an action id. */
    public static synchronized HexPattern patternFor(World world, ResourceLocation action) {
        if (!isPerWorldAction(action)) {
            return HexActionRegistry.getPattern(action);
        }

        Entry entry = null;
        if (world != null && world.isRemote) {
            for (Entry candidate : CLIENT_ENTRIES.values()) {
                if (action.equals(candidate.action)) {
                    entry = candidate;
                    break;
                }
            }
        } else {
            PerWorldPatternData data = get(world);
            entry = data == null ? null : data.byAction.get(action);
        }

        if (entry != null) {
            return entry.pattern();
        }
        return HexActionRegistry.getPattern(action);
    }

    /** Serialize the current server table for the client-side staff GUI. */
    public static synchronized NBTTagList snapshot(World world) {
        NBTTagList list = new NBTTagList();
        PerWorldPatternData data = get(world);
        if (data == null) {
            return list;
        }
        for (Entry entry : data.bySignature.values()) {
            list.appendTag(entry.serialize());
        }
        return list;
    }

    /** Replace the client copy received from the authoritative server. */
    public static synchronized void applyClientSnapshot(NBTTagList list) {
        CLIENT_ENTRIES.clear();
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.tagCount(); i++) {
            try {
                Entry entry = Entry.fromNBT(list.getCompoundTagAt(i));
                if (entry != null) {
                    CLIENT_ENTRIES.put(entry.signature, entry);
                }
            } catch (RuntimeException ignored) {
                // Ignore one malformed entry without losing the rest of the table.
            }
        }
    }

    /** Clear the previous server's table when a client world is unloaded. */
    public static synchronized void clearClientSnapshot() {
        CLIENT_ENTRIES.clear();
    }

    private static PerWorldPatternData createFromScratch(long seed) {
        HexActionRegistry.bootstrap();
        PerWorldPatternData out = new PerWorldPatternData();
        for (ResourceLocation action : PER_WORLD_ACTIONS) {
            out.addGenerated(action, seed);
        }
        out.markDirty();
        return out;
    }

    private void ensureEntries(long seed) {
        HexActionRegistry.bootstrap();
        boolean changed = false;
        for (ResourceLocation action : PER_WORLD_ACTIONS) {
            if (!byAction.containsKey(action)) {
                changed |= addGenerated(action, seed);
            }
        }
        if (changed) {
            markDirty();
        }
    }

    private boolean addGenerated(ResourceLocation action, long seed) {
        HexPattern prototype = HexActionRegistry.getPattern(action);
        if (prototype == null) {
            return false;
        }
        HexPattern generated = EulerPathFinder.findAltDrawing(prototype, seed);
        Entry entry = new Entry(generated.anglesSignature(), action, generated.getStartDir());
        Entry oldByAction = byAction.put(action, entry);
        Entry oldBySignature = bySignature.put(entry.signature, entry);
        if (oldBySignature != null && !oldBySignature.action.equals(action)) {
            // Match upstream behavior: the latest entry wins in the signature map.
            byAction.remove(oldBySignature.action);
        }
        return oldByAction == null || oldBySignature == null
            || !oldByAction.equals(entry) || !oldBySignature.equals(entry);
    }

    /**
     * Canonical, translation-independent representation of the undirected
     * line graph traced by a pattern.  Different Euler stroke orders then
     * produce the same key, while rotations remain distinct as in Hex.
     */
    private static String shapeKey(HexPattern pattern) {
        if (pattern == null || pattern.directions().isEmpty()) {
            return "";
        }
        ArrayList<HexCoord> points = new ArrayList<>();
        HexCoord cursor = HexCoord.Origin;
        points.add(cursor);
        for (HexDir direction : pattern.directions()) {
            cursor = cursor.plus(direction);
            points.add(cursor);
        }

        int minQ = Integer.MAX_VALUE;
        int minR = Integer.MAX_VALUE;
        for (HexCoord point : points) {
            minQ = Math.min(minQ, point.getQ());
            minR = Math.min(minR, point.getR());
        }

        ArrayList<String> edges = new ArrayList<>(points.size() - 1);
        for (int i = 0; i + 1 < points.size(); i++) {
            HexCoord first = points.get(i);
            HexCoord second = points.get(i + 1);
            String firstKey = (first.getQ() - minQ) + ":" + (first.getR() - minR);
            String secondKey = (second.getQ() - minQ) + ":" + (second.getR() - minR);
            if (firstKey.compareTo(secondKey) > 0) {
                String swap = firstKey;
                firstKey = secondKey;
                secondKey = swap;
            }
            edges.add(firstKey + "-" + secondKey);
        }
        Collections.sort(edges);
        return String.join(",", edges);
    }

    @Override
    public synchronized void readFromNBT(NBTTagCompound nbt) {
        bySignature.clear();
        byAction.clear();
        NBTTagList entries = nbt == null ? new NBTTagList() : nbt.getTagList(KEY_ENTRIES, 10);
        for (int i = 0; i < entries.tagCount(); i++) {
            try {
                Entry entry = Entry.fromNBT(entries.getCompoundTagAt(i));
                if (entry != null && isPerWorldAction(entry.action)) {
                    bySignature.put(entry.signature, entry);
                    byAction.put(entry.action, entry);
                }
            } catch (RuntimeException ignored) {
                // Keep valid saved patterns when one entry has been corrupted.
            }
        }
    }

    @Override
    public synchronized NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setString(KEY_VERSION, DATA_VERSION);
        NBTTagList entries = new NBTTagList();
        for (Entry entry : bySignature.values()) {
            entries.appendTag(entry.serialize());
        }
        nbt.setTag(KEY_ENTRIES, entries);
        return nbt;
    }

    private static final class Entry {
        private final String signature;
        private final ResourceLocation action;
        private final HexDir startDir;

        private Entry(String signature, ResourceLocation action, HexDir startDir) {
            this.signature = signature;
            this.action = action;
            this.startDir = startDir;
        }

        private HexPattern pattern() {
            return HexPattern.fromAngles(signature, startDir);
        }

        private NBTTagCompound serialize() {
            NBTTagCompound out = new NBTTagCompound();
            out.setString(KEY_SIGNATURE, signature);
            out.setString(KEY_ACTION, action.toString());
            out.setByte(KEY_START_DIR, (byte) startDir.ordinal());
            return out;
        }

        private static Entry fromNBT(NBTTagCompound nbt) {
            if (nbt == null || !nbt.hasKey(KEY_SIGNATURE, 8)
                || !nbt.hasKey(KEY_ACTION, 8) || !nbt.hasKey(KEY_START_DIR, 1)) {
                return null;
            }
            ResourceLocation action = new ResourceLocation(nbt.getString(KEY_ACTION));
            int ordinal = Byte.toUnsignedInt(nbt.getByte(KEY_START_DIR));
            if (ordinal >= HexDir.values().length) {
                return null;
            }
            String signature = nbt.getString(KEY_SIGNATURE);
            // Parsing validates all six compact angle symbols before accepting saved data.
            HexPattern.fromAngles(signature, HexDir.values()[ordinal]);
            return new Entry(signature, action, HexDir.values()[ordinal]);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Entry)) {
                return false;
            }
            Entry entry = (Entry) other;
            return signature.equals(entry.signature)
                && action.equals(entry.action) && startDir == entry.startDir;
        }

        @Override
        public int hashCode() {
            int result = signature.hashCode();
            result = 31 * result + action.hashCode();
            return 31 * result + startDir.hashCode();
        }
    }

    /** Exact port of Kotlin's deterministic XorWowRandom-backed Euler walker. */
    private static final class EulerPathFinder {
        private static final int MAX_ITERATIONS = 100;

        private static HexPattern findAltDrawing(HexPattern original, long seed) {
            KotlinRandom random = new KotlinRandom(seed);
            for (int i = 0; i < MAX_ITERATIONS; i++) {
                HexPattern path = walkPath(original, random);
                if (path != null) {
                    return path;
                }
            }
            return original;
        }

        private static HexPattern walkPath(HexPattern original, KotlinRandom random) {
            HashMap<Coord, EnumSet<HexDir>> graph = toGraph(original);
            ArrayList<Coord> oddNodes = new ArrayList<>();
            for (Map.Entry<Coord, EnumSet<HexDir>> entry : graph.entrySet()) {
                if ((entry.getValue().size() & 1) == 1) {
                    oddNodes.add(entry.getKey());
                }
            }
            if (oddNodes.size() != 0 && oddNodes.size() != 2) {
                throw new IllegalStateException("Pattern graph is not Euler-walkable");
            }

            ArrayList<Coord> candidates = oddNodes.isEmpty()
                ? new ArrayList<>(graph.keySet()) : oddNodes;
            Coord current = candidates.get(random.nextInt(candidates.size()));
            ArrayDeque<Coord> stack = new ArrayDeque<>();
            ArrayList<Coord> out = new ArrayList<>();
            do {
                EnumSet<HexDir> exits = graph.get(current);
                if (exits == null) {
                    return null;
                }
                if (exits.isEmpty()) {
                    out.add(current);
                    if (stack.isEmpty()) {
                        break;
                    }
                    current = stack.pop();
                } else {
                    stack.push(current);
                    ArrayList<HexDir> choices = new ArrayList<>(exits);
                    HexDir burnDir = choices.get(random.nextInt(choices.size()));
                    exits.remove(burnDir);
                    EnumSet<HexDir> reverse = graph.get(current.plus(burnDir));
                    if (reverse != null) {
                        reverse.remove(burnDir.rotatedBy(HexAngle.BACK));
                    }
                    current = current.plus(burnDir);
                }
            } while ((graph.get(current) != null && !graph.get(current).isEmpty())
                || !stack.isEmpty());
            out.add(current);

            ArrayList<HexDir> directions = new ArrayList<>(out.size() - 1);
            for (int i = 0; i + 1 < out.size(); i++) {
                HexDir direction = out.get(i).immediateDelta(out.get(i + 1));
                if (direction == null) {
                    return null;
                }
                directions.add(direction);
            }
            if (directions.isEmpty()) {
                return original;
            }
            ArrayList<HexAngle> angles = new ArrayList<>(Math.max(0, directions.size() - 1));
            for (int i = 0; i + 1 < directions.size(); i++) {
                angles.add(directions.get(i + 1).angleFrom(directions.get(i)));
            }
            return new HexPattern(directions.get(0), angles);
        }

        private static HashMap<Coord, EnumSet<HexDir>> toGraph(HexPattern pattern) {
            HashMap<Coord, EnumSet<HexDir>> graph = new HashMap<>();
            HexDir compass = pattern.getStartDir();
            Coord cursor = Coord.ORIGIN;
            for (HexAngle angle : pattern.getAngles()) {
                graph.computeIfAbsent(cursor, ignored -> EnumSet.noneOf(HexDir.class))
                    .add(compass);
                graph.computeIfAbsent(cursor.plus(compass), ignored ->
                    EnumSet.noneOf(HexDir.class)).add(compass.rotatedBy(HexAngle.BACK));
                cursor = cursor.plus(compass);
                compass = compass.times(angle);
            }
            graph.computeIfAbsent(cursor, ignored -> EnumSet.noneOf(HexDir.class))
                .add(compass);
            graph.computeIfAbsent(cursor.plus(compass), ignored ->
                EnumSet.noneOf(HexDir.class)).add(compass.rotatedBy(HexAngle.BACK));
            return graph;
        }
    }

    /** Coordinate key with Kotlin data-class hash semantics (q * 31 + r). */
    private static final class Coord {
        private static final Coord ORIGIN = new Coord(0, 0);
        private final int q;
        private final int r;

        private Coord(int q, int r) {
            this.q = q;
            this.r = r;
        }

        private Coord plus(HexDir direction) {
            switch (direction) {
                case NORTH_EAST: return new Coord(q + 1, r - 1);
                case EAST: return new Coord(q + 1, r);
                case SOUTH_EAST: return new Coord(q, r + 1);
                case SOUTH_WEST: return new Coord(q - 1, r + 1);
                case WEST: return new Coord(q - 1, r);
                case NORTH_WEST: return new Coord(q, r - 1);
                default: throw new AssertionError(direction);
            }
        }

        private HexDir immediateDelta(Coord next) {
            int dq = next.q - q;
            int dr = next.r - r;
            if (dq == 1 && dr == -1) return HexDir.NORTH_EAST;
            if (dq == 1 && dr == 0) return HexDir.EAST;
            if (dq == 0 && dr == 1) return HexDir.SOUTH_EAST;
            if (dq == -1 && dr == 1) return HexDir.SOUTH_WEST;
            if (dq == -1 && dr == 0) return HexDir.WEST;
            if (dq == 0 && dr == -1) return HexDir.NORTH_WEST;
            return null;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Coord && q == ((Coord) other).q && r == ((Coord) other).r;
        }

        @Override
        public int hashCode() {
            return 31 * q + r;
        }
    }

    /** Kotlin 1.20.x's kotlin.random.Random(Long) implementation. */
    private static final class KotlinRandom {
        private int x;
        private int y;
        private int z;
        private int w;
        private int v;
        private int addend;

        private KotlinRandom(long seed) {
            this((int) seed, (int) (seed >>> 32));
        }

        private KotlinRandom(int seedLow, int seedHigh) {
            x = seedLow;
            y = seedHigh;
            z = 0;
            w = 0;
            v = seedLow;
            addend = (~seedLow) ^ (seedLow << 10) ^ (seedHigh >>> 4);
            for (int i = 0; i < 64; i++) {
                nextInt();
            }
        }

        private int nextInt() {
            int t = x ^ (x >>> 2);
            x = y;
            y = z;
            z = w;
            w = v;
            t = t ^ (t << 1) ^ v ^ (v << 4);
            v = t;
            addend += 362437;
            return t + addend;
        }

        private int nextBits(int bitCount) {
            if (bitCount == 0) {
                return 0;
            }
            return nextInt() >>> (32 - bitCount);
        }

        private int nextInt(int until) {
            if (until <= 0) {
                throw new IllegalArgumentException("Random range is empty");
            }
            int difference = until;
            if ((difference & -difference) == difference) {
                int bits = 31 - Integer.numberOfLeadingZeros(difference);
                return nextBits(bits);
            }
            int candidate;
            int remainder;
            do {
                candidate = nextInt() >>> 1;
                remainder = candidate % difference;
            } while (candidate - remainder + (difference - 1) < 0);
            return remainder;
        }
    }
}
