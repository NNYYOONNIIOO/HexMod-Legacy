package at.petra_k.hexcasting.api.casting.eval;

import at.petra_k.hexcasting.common.lib.hex.HexIotaTypes;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.NullIota;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mutable evaluation stack for a spell.
 *
 * <p>The stack is deliberately independent of Minecraft entities and Forge
 * events. Actions can therefore be ported and tested before they are wired to
 * items, packets, or a casting environment.</p>
 */
public final class CastingStack {
    private final ArrayList<Iota> values = new ArrayList<>();
    private Iota local = new NullIota();

    public void push(Iota value) throws CastingException {
        if (value == null) {
            throw new CastingException("Cannot push a null Iota");
        }
        if (values.size() >= Iota.MAX_SERIALIZATION_TOTAL) {
            throw new CastingException("Casting stack exceeded its size limit");
        }
        values.add(value);
    }

    public Iota pop() throws CastingException {
        if (values.isEmpty()) {
            throw new CastingException("Not enough Iotas on the casting stack");
        }
        return values.remove(values.size() - 1);
    }

    public <T extends Iota> T pop(Class<T> expected) throws CastingException {
        Iota value = pop();
        if (!expected.isInstance(value)) {
            values.add(value);
            throw new CastingException("Expected " + expected.getSimpleName()
                + " but found " + value.getType().getId());
        }
        return expected.cast(value);
    }

    public Iota peek() throws CastingException {
        if (values.isEmpty()) {
            throw new CastingException("Not enough Iotas on the casting stack");
        }
        return values.get(values.size() - 1);
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public void clear() {
        values.clear();
    }

    /** Return the spell-local value (the modern Ravenmind/local slot). */
    public Iota readLocal() {
        return local;
    }

    /** Replace the spell-local value without mutating the evaluation stack. */
    public void writeLocal(Iota value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot write a null local Iota");
        }
        local = value;
    }

    public List<Iota> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    public void restore(List<? extends Iota> snapshot) throws CastingException {
        if (snapshot.size() > Iota.MAX_SERIALIZATION_TOTAL) {
            throw new CastingException("Casting stack snapshot exceeded its size limit");
        }
        values.clear();
        for (Iota value : snapshot) {
            push(value);
        }
    }

    public NBTTagList serialize() {
        NBTTagList out = new NBTTagList();
        for (Iota value : values) {
            out.appendTag(value.serialize());
        }
        return out;
    }


    /** Serialize both the evaluation stack and the local slot. */
    public NBTTagCompound serializeState() {
        NBTTagCompound out = new NBTTagCompound();
        out.setTag("stack", serialize());
        out.setTag("local", local.serialize());
        return out;
    }

    /** Decode the state-level representation used by the player capability. */
    public static CastingStack deserializeState(NBTTagCompound serialized) throws CastingException {
        CastingStack out = deserialize(serialized.getTagList("stack", 10));
        if (serialized.hasKey("local", 10)) {
            out.writeLocal(HexIotaTypes.deserialize(serialized.getCompoundTag("local")));
        }
        return out;
    }
    public static CastingStack deserialize(NBTTagList serialized) throws CastingException {
        if (serialized.tagCount() > Iota.MAX_SERIALIZATION_TOTAL) {
            throw new CastingException("Serialized casting stack exceeded its size limit");
        }
        CastingStack out = new CastingStack();
        for (int i = 0; i < serialized.tagCount(); i++) {
            out.push(HexIotaTypes.deserialize(serialized.getCompoundTagAt(i)));
        }
        return out;
    }
}
