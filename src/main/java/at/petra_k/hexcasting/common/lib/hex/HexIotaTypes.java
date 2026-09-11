package at.petra_k.hexcasting.common.lib.hex;

import at.petra_k.hexcasting.api.casting.iota.BooleanIota;
import at.petra_k.hexcasting.api.casting.iota.DoubleIota;
import at.petra_k.hexcasting.api.casting.iota.GarbageIota;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.IotaType;
import at.petra_k.hexcasting.api.casting.iota.ListIota;
import at.petra_k.hexcasting.api.casting.iota.NullIota;
import at.petra_k.hexcasting.api.casting.iota.PatternIota;
import at.petra_k.hexcasting.api.casting.iota.Vec3Iota;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Registry and bounded deserializer for the first 1.12.2 Iota set. */
public final class HexIotaTypes {
    public static final String KEY_TYPE = "type";
    public static final String KEY_DATA = "data";

    private static final Map<String, IotaType<? extends Iota>> TYPES = new LinkedHashMap<>();

    static {
        register(NullIota.TYPE);
        register(DoubleIota.TYPE);
        register(BooleanIota.TYPE);
        register(GarbageIota.TYPE);
        register(PatternIota.TYPE);
        register(ListIota.TYPE);
        register(Vec3Iota.TYPE);
    }

    private HexIotaTypes() {
    }

    private static void register(IotaType<? extends Iota> type) {
        IotaType<? extends Iota> old = TYPES.put(type.getId(), type);
        if (old != null) {
            throw new IllegalStateException("Duplicate Iota type: " + type.getId());
        }
    }

    public static Map<String, IotaType<? extends Iota>> all() {
        return Collections.unmodifiableMap(TYPES);
    }

    public static IotaType<? extends Iota> get(String id) {
        return TYPES.get(id);
    }

    public static Iota deserialize(NBTTagCompound serialized) {
        if (serialized == null || !serialized.hasKey(KEY_TYPE, 8)) {
            throw new IllegalArgumentException("Iota is missing its type");
        }
        IotaType<? extends Iota> type = TYPES.get(serialized.getString(KEY_TYPE));
        if (type == null) {
            throw new IllegalArgumentException("Unknown Iota type: " + serialized.getString(KEY_TYPE));
        }
        NBTTagCompound data = serialized.hasKey(KEY_DATA, 10)
            ? serialized.getCompoundTag(KEY_DATA)
            : serialized;
        return type.deserialize(data);
    }
}
