package at.petra_k.hexcasting.api.casting.iota;

import at.petra_k.hexcasting.common.lib.hex.HexIotaTypes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A first-class snapshot of the remaining casting continuation.
 *
 * <p>This is the value produced by {@code eval/cc}. It is deliberately an
 * Iota rather than a Java callback, so it can travel through a list and be
 * invoked later by {@code eval}.</p>
 */
public final class ContinuationIota extends Iota {
    public static final String KEY_CONTINUATION = "continuation";
    public static final IotaType<ContinuationIota> TYPE =
        new IotaType<>("continuation", data -> {
            NBTTagList serialized = data.getTagList(KEY_CONTINUATION, 10);
            ArrayList<Iota> values = new ArrayList<>(serialized.tagCount());
            for (int i = 0; i < serialized.tagCount(); i++) {
                values.add(HexIotaTypes.deserialize(serialized.getCompoundTagAt(i)));
            }
            return new ContinuationIota(values);
        });

    private final List<Iota> continuation;

    public ContinuationIota(List<? extends Iota> continuation) {
        super(TYPE);
        if (continuation.size() > MAX_SERIALIZATION_TOTAL) {
            throw new IllegalArgumentException("Continuation exceeded its size limit");
        }
        ArrayList<Iota> copy = new ArrayList<>(continuation.size());
        for (Iota value : continuation) {
            copy.add(Objects.requireNonNull(value, "continuation cannot contain null"));
        }
        this.continuation = copy;
    }

    public List<Iota> getContinuation() {
        return Collections.unmodifiableList(continuation);
    }

    @Override
    public List<Iota> getPayload() {
        return getContinuation();
    }

    @Override
    public boolean isTruthy() {
        return true;
    }

    @Override
    public String display() {
        return "Continuation [" + continuation.size() + " pending]";
    }

    @Override
    protected void serializePayload(NBTTagCompound data) {
        NBTTagList serialized = new NBTTagList();
        for (Iota value : continuation) {
            serialized.appendTag(value.serialize());
        }
        data.setTag(KEY_CONTINUATION, serialized);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ContinuationIota
            && continuation.equals(((ContinuationIota) other).continuation);
    }

    @Override
    public int hashCode() {
        return continuation.hashCode();
    }
}
