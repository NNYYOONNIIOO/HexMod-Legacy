package at.petra_k.hexcasting.interop.crafttweaker;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.iota.BooleanIota;
import at.petra_k.hexcasting.api.casting.iota.DoubleIota;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import crafttweaker.annotations.ModOnly;
import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;
import stanhebben.zenscript.annotations.ZenSetter;

import java.util.ArrayList;
import java.util.List;

/** Mutable ZenScript facade over the active casting stack and Ravenmind. */
@ModOnly("crafttweaker")
@ZenClass("mods.hexcasting.api.HexCastingStack")
@ZenRegister
public final class HexCastingStack {
    private final CastingStack stack;

    HexCastingStack(CastingStack stack) {
        this.stack = stack;
    }

    @ZenGetter("size")
    @ZenMethod
    public int size() {
        return stack.size();
    }

    @ZenGetter("empty")
    @ZenMethod
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    @ZenMethod
    public HexCastingIota pop() {
        try {
            return HexCastingIota.wrap(stack.pop());
        } catch (CastingException exception) {
            throw CustomSpellFailure.from(exception);
        }
    }

    @ZenMethod
    public HexCastingIota peek() {
        try {
            return HexCastingIota.wrap(stack.peek());
        } catch (CastingException exception) {
            throw CustomSpellFailure.from(exception);
        }
    }

    @ZenMethod
    public HexCastingIota get(int index) {
        List<Iota> values = stack.snapshot();
        if (index < 0 || index >= values.size()) {
            throw CustomSpellFailure.mishap("hexcasting.error.list_index_out_of_bounds",
                "Stack index " + index + " outside 0.."
                    + (values.size() - 1));
        }
        return HexCastingIota.wrap(values.get(index));
    }

    @ZenMethod
    public void push(HexCastingIota value) {
        try {
            stack.push(unwrap(value));
        } catch (CastingException exception) {
            throw CustomSpellFailure.from(exception);
        }
    }

    @ZenMethod
    public double popNumber() {
        try {
            return stack.pop(DoubleIota.class).getValue();
        } catch (CastingException exception) {
            throw CustomSpellFailure.from(exception);
        }
    }

    @ZenMethod
    public boolean popBoolean() {
        try {
            return stack.pop(BooleanIota.class).getValue();
        } catch (CastingException exception) {
            throw CustomSpellFailure.from(exception);
        }
    }

    @ZenMethod
    public void requireSize(int count) {
        try {
            stack.requireSize(count);
        } catch (CastingException exception) {
            throw CustomSpellFailure.from(exception);
        }
    }

    @ZenMethod
    public void pushGarbage(int count) {
        try {
            stack.pushGarbage(count);
        } catch (CastingException exception) {
            throw CustomSpellFailure.from(exception);
        }
    }

    @ZenMethod
    public void clearAndPushGarbage() {
        try {
            stack.clearAndPushGarbage();
        } catch (CastingException exception) {
            throw CustomSpellFailure.from(exception);
        }
    }

    @ZenMethod
    public void replaceFromTop(int reverseIndex, HexCastingIota value) {
        try {
            stack.replaceFromTop(reverseIndex, unwrap(value));
        } catch (CastingException exception) {
            throw CustomSpellFailure.from(exception);
        }
    }

    @ZenMethod
    public void set(int index, HexCastingIota value) {
        if (index < 0 || index >= stack.size()) {
            throw CustomSpellFailure.mishap("hexcasting.error.list_index_out_of_bounds",
                "Stack index " + index + " outside 0.."
                    + (stack.size() - 1));
        }
        int reverseIndex = stack.size() - index - 1;
        replaceFromTop(reverseIndex, value);
    }

    @ZenMethod
    public void clear() {
        stack.clear();
    }

    @ZenGetter("local")
    @ZenMethod
    public HexCastingIota getLocal() {
        return HexCastingIota.wrap(stack.readLocal());
    }

    @ZenSetter("local")
    @ZenMethod
    public void setLocal(HexCastingIota value) {
        stack.writeLocal(unwrap(value));
    }

    @ZenGetter("ravenmind")
    @ZenMethod
    public HexCastingIota getRavenmind() {
        return getLocal();
    }

    @ZenSetter("ravenmind")
    @ZenMethod
    public void setRavenmind(HexCastingIota value) {
        setLocal(value);
    }

    @ZenMethod
    public HexCastingIota[] snapshot() {
        return HexCastingIota.wrapAll(stack.snapshot());
    }

    @ZenMethod
    public void restore(HexCastingIota[] values) {
        ArrayList<Iota> raw = new ArrayList<>();
        if (values != null) {
            for (HexCastingIota value : values) {
                raw.add(unwrap(value));
            }
        }
        try {
            stack.restore(raw);
        } catch (CastingException exception) {
            throw CustomSpellFailure.from(exception);
        }
    }

    private static Iota unwrap(HexCastingIota value) {
        if (value == null) {
            throw CustomSpellFailure.from(new CastingException(
                "A custom action cannot push a null Iota"));
        }
        return value.unwrap();
    }
}
