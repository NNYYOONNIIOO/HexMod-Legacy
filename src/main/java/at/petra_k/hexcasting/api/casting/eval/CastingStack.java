package at.petra_k.hexcasting.api.casting.eval;

import at.petra_k.hexcasting.common.lib.hex.HexIotaTypes;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.GarbageIota;
import at.petra_k.hexcasting.api.casting.iota.Vec3Iota;
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
            throw Mishap.invalidValue("hexcasting.error.invalid_value",
                "Cannot push a null Iota");
        }
        if (values.size() >= Iota.MAX_SERIALIZATION_TOTAL) {
            throw Mishap.stackSize();
        }
        values.add(value);
    }

    public Iota pop() throws CastingException {
        if (values.isEmpty()) {
            throw Mishap.notEnoughArguments(1, values.size());
        }
        return values.remove(values.size() - 1);
    }

    /** Fail before mutating the stack when an action needs several arguments. */
    public void requireSize(int expected) throws CastingException {
        if (expected < 0) {
            throw Mishap.invalidValue("hexcasting.error.invalid_value",
                "Expected stack size cannot be negative");
        }
        if (values.size() < expected) {
            throw Mishap.notEnoughArguments(expected, values.size());
        }
    }

    public <T extends Iota> T pop(Class<T> expected) throws CastingException {
        return pop(expected, 0);
    }

    /**
     * Pop a typed argument and retain the argument's position for a Mishap.
     *
     * <p>The reverse index is counted from the top of the complete action
     * argument slice after the VM restores the failed image. This matches the
     * modern action helpers: the top argument is index {@code 0}, the next is
     * {@code 1}, and so on.</p>
     */
    public <T extends Iota> T pop(Class<T> expected, int reverseIndex)
        throws CastingException {
        Iota value = pop();
        if (!expected.isInstance(value)) {
            values.add(value);
            throw Mishap.invalidIota(value, Math.max(0, reverseIndex),
                expectedName(expected));
        }
        return expected.cast(value);
    }

    /** Add the placeholder values required by a not-enough-arguments Mishap. */
    public void pushGarbage(int count) throws CastingException {
        if (count < 0) {
            throw Mishap.invalidValue("hexcasting.error.invalid_value",
                "Garbage count cannot be negative");
        }
        for (int i = 0; i < count; i++) {
            push(new GarbageIota());
        }
    }

    /** Replace one value counted from the top of the stack. */
    public void replaceFromTop(int reverseIndex, Iota replacement)
        throws CastingException {
        if (replacement == null) {
            throw Mishap.invalidValue("hexcasting.error.invalid_value",
                "Stack replacement cannot be null");
        }
        if (reverseIndex < 0 || reverseIndex >= values.size()) {
            throw Mishap.invalidValue("hexcasting.error.list_index_out_of_bounds",
                "Stack replacement index " + reverseIndex
                    + " is outside 0.." + (values.size() - 1));
        }
        int index = values.size() - 1 - reverseIndex;
        values.set(index, replacement);
    }

    /** Resolve a stack-size Mishap to the single black-spark placeholder. */
    public void clearAndPushGarbage() throws CastingException {
        values.clear();
        push(new GarbageIota());
    }

    public Iota peek() throws CastingException {
        if (values.isEmpty()) {
            throw Mishap.notEnoughArguments(1, values.size());
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
            throw Mishap.invalidValue("hexcasting.error.stack_state_limit",
                "Casting stack snapshot exceeded its size limit");
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
            throw Mishap.invalidValue("hexcasting.error.stack_state_limit",
                "Serialized casting stack exceeded its size limit");
        }
        CastingStack out = new CastingStack();
        for (int i = 0; i < serialized.tagCount(); i++) {
            out.push(HexIotaTypes.deserialize(serialized.getCompoundTagAt(i)));
        }
        return out;
    }

    private static String expectedName(Class<? extends Iota> expected) {
        if (expected == null) {
            return "unknown";
        }
        if (expected == Vec3Iota.class) {
            return "vector";
        }
        String name = expected.getSimpleName();
        if (name.endsWith("Iota")) {
            name = name.substring(0, name.length() - 4);
        }
        return name.toLowerCase(java.util.Locale.ROOT);
    }
}
