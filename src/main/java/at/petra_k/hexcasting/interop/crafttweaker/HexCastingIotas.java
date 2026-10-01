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
import at.petra_k.hexcasting.api.casting.math.HexDir;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.lib.hex.HexIotaTypes;
import crafttweaker.annotations.ModOnly;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.block.IBlockState;
import crafttweaker.api.data.IData;
import crafttweaker.api.entity.IEntity;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Constructors for the Iota values used by custom action callbacks. */
@ModOnly("crafttweaker")
@ZenClass("mods.hexcasting.Iotas")
@ZenRegister
public final class HexCastingIotas {
    private HexCastingIotas() {
    }

    @ZenMethod
    public static HexCastingIota number(double value) {
        requireFinite(value, "number");
        return HexCastingIota.wrap(new DoubleIota(value));
    }

    @ZenMethod
    public static HexCastingIota booleanValue(boolean value) {
        return HexCastingIota.wrap(new BooleanIota(value));
    }

    @ZenMethod
    public static HexCastingIota vector(double x, double y, double z) {
        requireFinite(x, "vector x");
        requireFinite(y, "vector y");
        requireFinite(z, "vector z");
        return HexCastingIota.wrap(new Vec3Iota(new Vec3d(x, y, z)));
    }

    @ZenMethod
    public static HexCastingIota item(IItemStack value) {
        try {
            ItemStack converted = value == null ? ItemStack.EMPTY
                : CraftTweakerMC.getItemStack(value);
            ItemStack stack = converted == null ? ItemStack.EMPTY : converted.copy();
            return HexCastingIota.wrap(new ItemIota(stack));
        } catch (RuntimeException exception) {
            throw invalid("Cannot convert item to an item Iota: "
                + exception.getMessage());
        }
    }

    @ZenMethod
    public static HexCastingIota block(IBlockState value) {
        if (value == null) {
            throw invalid("Cannot create a block Iota from null");
        }
        try {
            net.minecraft.block.state.IBlockState state = CraftTweakerMC.getBlockState(value);
            if (state == null) {
                throw invalid("Cannot create a block Iota from an empty block state");
            }
            return HexCastingIota.wrap(new BlockIota(state));
        } catch (CustomSpellFailure failure) {
            throw failure;
        } catch (RuntimeException exception) {
            throw invalid("Cannot convert block state to a block Iota: "
                + exception.getMessage());
        }
    }

    @ZenMethod
    public static HexCastingIota entity(IEntity value) {
        if (value == null || value.getInternal() == null) {
            throw invalid("Cannot create an entity Iota from null");
        }
        try {
            Object internal = value.getInternal();
            if (!(internal instanceof Entity)) {
                throw invalid("The supplied value is not a Minecraft entity");
            }
            return HexCastingIota.wrap(new EntityIota((Entity) internal));
        } catch (CustomSpellFailure failure) {
            throw failure;
        } catch (RuntimeException exception) {
            throw invalid("Cannot convert entity to an entity Iota: "
                + exception.getMessage());
        }
    }

    @ZenMethod
    public static HexCastingIota pattern(String startDirection,
                                         String angleSequence) {
        try {
            HexDir direction = parseDirection(startDirection);
            return HexCastingIota.wrap(new PatternIota(
                HexPattern.fromAngles(angleSequence, direction)));
        } catch (RuntimeException exception) {
            throw invalid("Cannot create pattern Iota: " + exception.getMessage());
        }
    }

    @ZenMethod
    public static HexCastingIota list(HexCastingIota[] values) {
        try {
            return HexCastingIota.wrap(new ListIota(unwrap(values)));
        } catch (CustomSpellFailure failure) {
            throw failure;
        } catch (RuntimeException exception) {
            throw invalid("Cannot create list Iota: " + exception.getMessage());
        }
    }

    @ZenMethod
    public static HexCastingIota continuation(HexCastingIota[] values) {
        try {
            return HexCastingIota.wrap(new ContinuationIota(unwrap(values)));
        } catch (CustomSpellFailure failure) {
            throw failure;
        } catch (RuntimeException exception) {
            throw invalid("Cannot create continuation Iota: "
                + exception.getMessage());
        }
    }

    @ZenMethod
    public static HexCastingIota nullValue() {
        return HexCastingIota.wrap(new NullIota());
    }

    @ZenMethod
    public static HexCastingIota garbage() {
        return HexCastingIota.wrap(new GarbageIota());
    }

    @ZenMethod
    public static HexCastingIota fromNBT(IData value) {
        if (value == null) {
            throw invalid("Iota NBT cannot be null");
        }
        try {
            NBTBase raw = CraftTweakerMC.getNBT(value);
            if (!(raw instanceof NBTTagCompound)) {
                throw invalid("Iota NBT must be a compound");
            }
            return HexCastingIota.wrap(HexIotaTypes.deserialize(
                (NBTTagCompound) raw));
        } catch (CustomSpellFailure failure) {
            throw failure;
        } catch (RuntimeException exception) {
            throw invalid("Cannot deserialize Iota NBT: "
                + exception.getMessage());
        }
    }

    @ZenMethod
    public static IData toNBT(HexCastingIota value) {
        if (value == null) {
            return null;
        }
        try {
            return CraftTweakerMC.getIData(value.unwrap().serialize());
        } catch (RuntimeException exception) {
            throw invalid("Cannot serialize Iota NBT: " + exception.getMessage());
        }
    }

    private static HexDir parseDirection(String direction) {
        if (direction == null) {
            throw new IllegalArgumentException("Pattern start direction is required");
        }
        try {
            return HexDir.valueOf(direction.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown Hex direction: " + direction,
                exception);
        }
    }

    private static List<Iota> unwrap(HexCastingIota[] values) {
        if (values == null) {
            return java.util.Collections.emptyList();
        }
        if (values.length > Iota.MAX_SERIALIZATION_TOTAL) {
            throw invalid("Iota list exceeds the maximum size of "
                + Iota.MAX_SERIALIZATION_TOTAL);
        }
        ArrayList<Iota> result = new ArrayList<>(values.length);
        for (HexCastingIota value : values) {
            if (value == null) {
                throw invalid("Iota arrays cannot contain null");
            }
            result.add(value.unwrap());
        }
        return result;
    }

    private static void requireFinite(double value, String name) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw invalid(name + " must be finite");
        }
    }

    private static CustomSpellFailure invalid(String detail) {
        return CustomSpellFailure.mishap("hexcasting.error.invalid_value", detail);
    }
}
