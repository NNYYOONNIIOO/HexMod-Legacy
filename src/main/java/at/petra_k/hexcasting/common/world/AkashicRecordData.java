package at.petra_k.hexcasting.common.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import java.util.HashMap;
import java.util.Map;

/** World-persistent storage bridge for Akashic records in the 1.12.2 port. */
public final class AkashicRecordData extends WorldSavedData {
    private static final String DATA_NAME = "hexcasting_akashic_records";
    private static final String KEY_RECORDS = "records";
    private final Map<String, NBTTagCompound> records = new HashMap<>();

    public AkashicRecordData() {
        super(DATA_NAME);
    }

    public AkashicRecordData(String name) {
        super(name);
    }

    public static AkashicRecordData get(World world) {
        MapStorage storage = world.getPerWorldStorage();
        AkashicRecordData data = (AkashicRecordData) storage.getOrLoadData(
            AkashicRecordData.class, DATA_NAME);
        if (data == null) {
            data = new AkashicRecordData();
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public NBTTagCompound read(BlockPos position, String pattern) {
        NBTTagCompound value = records.get(key(position, pattern));
        return value == null ? null : value.copy();
    }

    public void write(BlockPos position, String pattern, NBTTagCompound value) {
        if (value == null) {
            records.remove(key(position, pattern));
        } else {
            records.put(key(position, pattern), value.copy());
        }
        markDirty();
    }

    /** Copy every legacy mapping stored at one record position. */
    public NBTTagCompound snapshotAt(BlockPos position) {
        NBTTagCompound snapshot = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        String prefix = position.getX() + ":" + position.getY() + ":"
            + position.getZ() + ":";
        for (Map.Entry<String, NBTTagCompound> record : records.entrySet()) {
            if (!record.getKey().startsWith(prefix)) {
                continue;
            }
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("pattern", record.getKey().substring(prefix.length()));
            entry.setTag("value", record.getValue().copy());
            list.appendTag(entry);
        }
        snapshot.setTag("entries", list);
        return snapshot;
    }

    /** Restore the legacy mappings at one record position from a snapshot. */
    public void restoreAt(BlockPos position, NBTTagCompound snapshot) {
        clearAt(position);
        if (snapshot == null || !snapshot.hasKey("entries", 9)) {
            return;
        }
        NBTTagList list = snapshot.getTagList("entries", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            if (entry.hasKey("pattern", 8) && entry.hasKey("value", 10)) {
                records.put(key(position, entry.getString("pattern")),
                    entry.getCompoundTag("value").copy());
            }
        }
        markDirty();
    }

    public int countAt(BlockPos position) {
        String prefix = position.getX() + ":" + position.getY() + ":"
            + position.getZ() + ":";
        int count = 0;
        for (String key : records.keySet()) {
            if (key.startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }

    public void clearAt(BlockPos position) {
        String prefix = position.getX() + ":" + position.getY() + ":"
            + position.getZ() + ":";
        records.keySet().removeIf(key -> key.startsWith(prefix));
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        records.clear();
        if (nbt == null || !nbt.hasKey(KEY_RECORDS, 9)) {
            return;
        }
        NBTTagList list = nbt.getTagList(KEY_RECORDS, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            if (entry.hasKey("key", 8) && entry.hasKey("value", 10)) {
                records.put(entry.getString("key"),
                    entry.getCompoundTag("value").copy());
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, NBTTagCompound> record : records.entrySet()) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("key", record.getKey());
            entry.setTag("value", record.getValue().copy());
            list.appendTag(entry);
        }
        nbt.setTag(KEY_RECORDS, list);
        return nbt;
    }

    private static String key(BlockPos position, String pattern) {
        return position.getX() + ":" + position.getY() + ":"
            + position.getZ() + ":" + pattern;
    }
}
