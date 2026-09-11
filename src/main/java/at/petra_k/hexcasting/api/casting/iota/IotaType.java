package at.petra_k.hexcasting.api.casting.iota;

import net.minecraft.nbt.NBTTagCompound;

import java.util.Objects;
import java.util.function.Function;

/** Describes an Iota kind and knows how to decode its payload. */
public final class IotaType<T extends Iota> {
    private final String id;
    private final Function<NBTTagCompound, T> decoder;

    public IotaType(String id, Function<NBTTagCompound, T> decoder) {
        this.id = Objects.requireNonNull(id, "id");
        this.decoder = Objects.requireNonNull(decoder, "decoder");
    }

    public String getId() {
        return id;
    }

    public T deserialize(NBTTagCompound data) {
        return decoder.apply(data);
    }

    @Override
    public String toString() {
        return "IotaType(" + id + ")";
    }
}
