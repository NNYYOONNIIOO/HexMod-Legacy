package at.petra_k.hexcasting.interop.crafttweaker;

import at.petra_k.hexcasting.api.casting.iota.BlockIota;
import at.petra_k.hexcasting.api.casting.iota.BooleanIota;
import at.petra_k.hexcasting.api.casting.iota.ContinuationIota;
import at.petra_k.hexcasting.api.casting.iota.DoubleIota;
import at.petra_k.hexcasting.api.casting.iota.EntityIota;
import at.petra_k.hexcasting.api.casting.iota.GarbageIota;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.ItemIota;
import at.petra_k.hexcasting.api.casting.iota.ListIota;
import at.petra_k.hexcasting.api.casting.iota.NullIota;
import at.petra_k.hexcasting.api.casting.iota.PatternIota;
import at.petra_k.hexcasting.api.casting.iota.Vec3Iota;
import at.petra_k.hexcasting.api.casting.eval.Mishap;
import crafttweaker.annotations.ModOnly;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.block.IBlockState;
import crafttweaker.api.entity.IEntity;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.util.math.Vec3d;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.List;

/** Script-visible view of an Iota on a custom action's stack. */
@ModOnly("crafttweaker")
@ZenClass("mods.hexcasting.api.HexIota")
@ZenRegister
public final class HexCastingIota {
    private final Iota value;

    HexCastingIota(Iota value) {
        if (value == null) {
            throw new IllegalArgumentException("Iota cannot be null");
        }
        this.value = value;
    }

    Iota unwrap() {
        return value;
    }

    @ZenGetter("type")
    @ZenMethod
    public String getType() {
        return value.getType().getId();
    }

    @ZenGetter("display")
    @ZenMethod
    public String getDisplay() {
        return value.display();
    }

    @ZenGetter("truthy")
    @ZenMethod
    public boolean isTruthy() {
        return value.isTruthy();
    }

    @ZenMethod
    public boolean isNumber() {
        return value instanceof DoubleIota;
    }

    @ZenMethod
    public boolean isBoolean() {
        return value instanceof BooleanIota;
    }

    @ZenMethod
    public boolean isVector() {
        return value instanceof Vec3Iota;
    }

    @ZenMethod
    public boolean isItem() {
        return value instanceof ItemIota;
    }

    @ZenMethod
    public boolean isBlock() {
        return value instanceof BlockIota;
    }

    @ZenMethod
    public boolean isEntity() {
        return value instanceof EntityIota;
    }

    @ZenMethod
    public boolean isList() {
        return value instanceof ListIota;
    }

    @ZenMethod
    public boolean isPattern() {
        return value instanceof PatternIota;
    }

    @ZenMethod
    public boolean isNull() {
        return value instanceof NullIota;
    }

    @ZenMethod
    public boolean isGarbage() {
        return value instanceof GarbageIota;
    }

    @ZenMethod
    public boolean isContinuation() {
        return value instanceof ContinuationIota;
    }

    @ZenGetter("number")
    @ZenMethod
    public double getNumber() {
        return require(DoubleIota.class, "number").getValue();
    }

    @ZenGetter("booleanValue")
    @ZenMethod
    public boolean getBooleanValue() {
        return require(BooleanIota.class, "boolean").getValue();
    }

    @ZenGetter("x")
    @ZenMethod
    public double getX() {
        return require(Vec3Iota.class, "vector").getValue().x;
    }

    @ZenGetter("y")
    @ZenMethod
    public double getY() {
        return require(Vec3Iota.class, "vector").getValue().y;
    }

    @ZenGetter("z")
    @ZenMethod
    public double getZ() {
        return require(Vec3Iota.class, "vector").getValue().z;
    }

    @ZenGetter("item")
    @ZenMethod
    public IItemStack getItem() {
        return CraftTweakerMC.getIItemStack(
            require(ItemIota.class, "item").getStack());
    }

    @ZenGetter("blockState")
    @ZenMethod
    public IBlockState getBlockState() {
        return CraftTweakerMC.getBlockState(
            require(BlockIota.class, "block").getState());
    }

    @ZenGetter("entity")
    @ZenMethod
    public IEntity getEntity() {
        EntityIota iota = require(EntityIota.class, "entity");
        if (iota.getEntity() == null) {
            throw CustomSpellFailure.from(
                Mishap.badEntity("hexcasting.error.entity_unavailable"));
        }
        return CraftTweakerMC.getIEntity(iota.getEntity());
    }

    @ZenGetter("patternSignature")
    @ZenMethod
    public String getPatternSignature() {
        return require(PatternIota.class, "pattern").getPattern().signature();
    }

    @ZenMethod
    public String getPatternAngles() {
        return require(PatternIota.class, "pattern").getPattern().anglesSignature();
    }

    @ZenMethod
    public HexCastingIota[] getList() {
        return wrapAll(require(ListIota.class, "list").getItems());
    }

    @ZenMethod
    public HexCastingIota[] getContinuation() {
        return wrapAll(require(ContinuationIota.class, "continuation")
            .getContinuation());
    }

    @ZenMethod
    public boolean tolerates(HexCastingIota other) {
        return other != null && Iota.tolerates(value, other.value);
    }

    static HexCastingIota wrap(Iota value) {
        return value == null ? null : new HexCastingIota(value);
    }

    static HexCastingIota[] wrapAll(List<? extends Iota> values) {
        HexCastingIota[] result = new HexCastingIota[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = wrap(values.get(i));
        }
        return result;
    }

    private <T extends Iota> T require(Class<T> type, String expected) {
        if (!type.isInstance(value)) {
            throw CustomSpellFailure.mishap("hexcasting.error.invalid_iota",
                "Expected " + expected + " Iota, got "
                    + value.getType().getId());
        }
        return type.cast(value);
    }
}
