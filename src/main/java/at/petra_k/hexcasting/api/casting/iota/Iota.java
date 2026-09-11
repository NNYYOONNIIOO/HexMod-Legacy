package at.petra_k.hexcasting.api.casting.iota;

import net.minecraft.nbt.NBTTagCompound;

import java.util.Objects;

/** Base value carried by the Hex Casting evaluation stack. */
public abstract class Iota {
    public static final int MAX_SERIALIZATION_DEPTH = 256;
    public static final int MAX_SERIALIZATION_TOTAL = 1024;

    private final IotaType<?> type;

    protected Iota(IotaType<?> type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    public final IotaType<?> getType() {
        return type;
    }

    public abstract Object getPayload();

    public abstract boolean isTruthy();

    /** A compact, server-safe representation used by tooltips and diagnostics. */
    public abstract String display();

    protected abstract void serializePayload(NBTTagCompound data);

    public final NBTTagCompound serialize() {
        NBTTagCompound out = new NBTTagCompound();
        out.setString("type", type.getId());
        NBTTagCompound data = new NBTTagCompound();
        serializePayload(data);
        out.setTag("data", data);
        return out;
    }

    @Override
    public String toString() {
        return type.getId() + "(" + display() + ")";
    }
}
