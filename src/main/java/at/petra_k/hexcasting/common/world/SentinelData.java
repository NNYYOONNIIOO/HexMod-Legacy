package at.petra_k.hexcasting.common.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persistent per-player sentinel anchors for the 1.12.2 port. */
public final class SentinelData extends WorldSavedData {
    private static final String DATA_NAME = "hexcasting_sentinels";
    private static final String LIST_KEY = "sentinels";

    private final Map<UUID, State> states = new HashMap<>();

    public SentinelData() {
        super(DATA_NAME);
    }

    public SentinelData(String name) {
        super(name);
    }

    public static SentinelData get(World world) {
        MapStorage storage = world.getMapStorage();
        if (world instanceof WorldServer) {
            net.minecraft.server.MinecraftServer server =
                ((WorldServer) world).getMinecraftServer();
            if (server != null && server.getWorld(0) != null) {
                // Sentinel state is attached to the player, not to the
                // dimension in which the player happens to be standing.
                // Store it in the overworld's shared MapStorage so a
                // dimension change can report the proper wrong-dimension
                // Mishap instead of silently losing the sentinel.
                storage = server.getWorld(0).getMapStorage();
            }
        }
        SentinelData data = (SentinelData) storage.getOrLoadData(SentinelData.class, DATA_NAME);
        if (data == null) {
            data = new SentinelData();
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public synchronized State get(UUID player) {
        return states.get(player);
    }

    public synchronized void set(UUID player, boolean extendedRange, double x, double y, double z,
                                  int dimension) {
        states.put(player, new State(extendedRange, x, y, z, dimension));
        markDirty();
    }

    public synchronized void clear(UUID player) {
        if (states.remove(player) != null) {
            markDirty();
        }
    }

    @Override
    public synchronized void readFromNBT(NBTTagCompound nbt) {
        states.clear();
        NBTTagList list = nbt.getTagList(LIST_KEY, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            try {
                UUID player = UUID.fromString(entry.getString("player"));
                double x = entry.getDouble("x");
                double y = entry.getDouble("y");
                double z = entry.getDouble("z");
                if (!isFiniteWorldPosition(x, y, z)) {
                    continue;
                }
                states.put(player, new State(
                    entry.getBoolean("extended"),
                    x, y, z,
                    entry.getInteger("dimension")
                ));
            } catch (IllegalArgumentException ignored) {
                // Ignore corrupt individual entries while preserving the rest of the save.
            }
        }
    }

    @Override
    public synchronized NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<UUID, State> entry : states.entrySet()) {
            State state = entry.getValue();
            NBTTagCompound serialized = new NBTTagCompound();
            serialized.setString("player", entry.getKey().toString());
            serialized.setBoolean("extended", state.extendedRange);
            serialized.setDouble("x", state.x);
            serialized.setDouble("y", state.y);
            serialized.setDouble("z", state.z);
            serialized.setInteger("dimension", state.dimension);
            list.appendTag(serialized);
        }
        nbt.setTag(LIST_KEY, list);
        return nbt;
    }

    public static final class State {
        public final boolean extendedRange;
        public final double x;
        public final double y;
        public final double z;
        public final int dimension;

        private State(boolean extendedRange, double x, double y, double z, int dimension) {
            this.extendedRange = extendedRange;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
        }
    }

    private static boolean isFiniteWorldPosition(double x, double y, double z) {
        return !Double.isNaN(x) && !Double.isInfinite(x)
            && !Double.isNaN(y) && !Double.isInfinite(y)
            && !Double.isNaN(z) && !Double.isInfinite(z)
            && y >= 0.0D && y < 256.0D
            && Math.abs(x) <= 30000000.0D
            && Math.abs(z) <= 30000000.0D;
    }
}
