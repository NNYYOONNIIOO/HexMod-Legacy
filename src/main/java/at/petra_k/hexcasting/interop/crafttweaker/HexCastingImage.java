package at.petra_k.hexcasting.interop.crafttweaker;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.Mishap;
import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;
import at.petra_k.hexcasting.api.casting.iota.ContinuationIota;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.PatternIota;
import crafttweaker.annotations.ModOnly;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.data.IData;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.ArrayList;
/** Script-visible operations over the active VM image. */
@ModOnly("crafttweaker")
@ZenClass("mods.hexcasting.api.HexCastingImage")
@ZenRegister
public final class HexCastingImage {
    private final CastingVM vm;

    HexCastingImage(CastingVM vm) {
        this.vm = vm;
    }

    @ZenGetter("operationsConsumed")
    @ZenMethod
    public int getOperationsConsumed() {
        return vm.getOperationsConsumed();
    }

    @ZenGetter("operationsRemaining")
    @ZenMethod
    public int getOperationsRemaining() {
        return vm.getRemainingOperationsForCurrentAction();
    }

    @ZenGetter("parenthesisDepth")
    @ZenMethod
    public int getParenthesisDepth() {
        return vm.getParenDepth();
    }

    @ZenGetter("escapeNext")
    @ZenMethod
    public boolean isEscapeNext() {
        return vm.isEscapeNext();
    }

    @ZenGetter("pendingCount")
    @ZenMethod
    public int getPendingCount() {
        return vm.getPendingCount();
    }

    @ZenGetter("halted")
    @ZenMethod
    public boolean isHalted() {
        return vm.isHalted();
    }

    @ZenGetter("userData")
    @ZenMethod
    public IData getUserData() {
        try {
            return CraftTweakerMC.getIDataModifyable(vm.getUserData());
        } catch (RuntimeException exception) {
            throw CustomSpellFailure.mishap("hexcasting.error.invalid_value",
                "Could not expose custom spell userData: "
                    + exception.getClass().getSimpleName());
        }
    }

    @ZenMethod
    public void setUserData(IData data) {
        NBTTagCompound replacement = null;
        if (data != null) {
            try {
                NBTBase raw = CraftTweakerMC.getNBT(data);
                if (!(raw instanceof NBTTagCompound)) {
                    throw CustomSpellFailure.mishap("hexcasting.error.invalid_value",
                        "Custom spell userData must be an NBT compound");
                }
                replacement = (NBTTagCompound) raw;
            } catch (CustomSpellFailure failure) {
                throw failure;
            } catch (RuntimeException exception) {
                throw CustomSpellFailure.mishap("hexcasting.error.invalid_value",
                    "Could not read custom spell userData: "
                        + exception.getClass().getSimpleName());
            }
        }
        NBTTagCompound target = vm.getUserData();
        for (String key : new ArrayList<>(target.getKeySet())) {
            target.removeTag(key);
        }
        if (replacement != null) {
            target.merge(replacement.copy());
        }
    }

    @ZenMethod
    public void enqueue(HexCastingIota value) {
        vm.enqueueIota(unwrap(value));
    }

    @ZenMethod
    public void enqueueFirst(HexCastingIota value) {
        vm.enqueueFrontIotas(java.util.Collections.singletonList(unwrap(value)));
    }

    @ZenMethod
    public void enqueuePattern(HexCastingIota value) {
        Iota iota = unwrap(value);
        if (!(iota instanceof PatternIota)) {
            throw CustomSpellFailure.mishap("hexcasting.error.invalid_iota",
                "enqueuePattern expects a pattern Iota");
        }
        vm.enqueue(((PatternIota) iota).getPattern());
    }

    @ZenMethod
    public HexCastingIota captureContinuation() {
        return HexCastingIota.wrap(vm.captureContinuation());
    }

    @ZenMethod
    public void invokeContinuation(HexCastingIota value) {
        Iota iota = unwrap(value);
        if (!(iota instanceof ContinuationIota)) {
            throw CustomSpellFailure.mishap("hexcasting.error.invalid_iota",
                "invokeContinuation expects a continuation Iota");
        }
        try {
            vm.invokeContinuation((ContinuationIota) iota);
        } catch (CastingException exception) {
            throw CustomSpellFailure.from(exception);
        }
    }

    @ZenMethod
    public HexCastingStack runNested(HexCastingIota value) {
        try {
            return new HexCastingStack(vm.runNestedIota(unwrap(value)));
        } catch (CastingException exception) {
            throw CustomSpellFailure.from(exception);
        } catch (RuntimeException exception) {
            throw CustomSpellFailure.mishap("hexcasting.error.invalid_iota",
                "Nested custom spell evaluation failed: "
                    + exception.getClass().getSimpleName());
        }
    }

    @ZenMethod
    public void consumeMedia(long amount) {
        try {
            vm.consumeMedia(amount);
        } catch (CastingException exception) {
            throw CustomSpellFailure.from(exception);
        }
    }

    @ZenMethod
    public void halt() {
        vm.halt();
    }

    @ZenMethod
    public void setEscapeNext() {
        vm.setEscapeNext();
    }

    @ZenMethod
    public void resetEscape() {
        vm.resetEscape();
    }

    @ZenMethod
    public void resetMetaState() {
        vm.resetMetaState();
    }

    @ZenMethod
    public void openParen() {
        vm.openParen();
    }

    @ZenMethod
    public void openParens(int count) {
        try {
            vm.openParens(count);
        } catch (IllegalArgumentException exception) {
            throw CustomSpellFailure.mishap("hexcasting.error.invalid_value",
                exception.getMessage());
        }
    }

    @ZenMethod
    public void closeParen() {
        try {
            vm.closeParen();
        } catch (CastingException exception) {
            throw CustomSpellFailure.from(exception);
        }
    }

    @ZenMethod
    public void closeAllParens() {
        try {
            vm.closeAllParens();
        } catch (CastingException exception) {
            throw CustomSpellFailure.from(exception);
        }
    }

    @ZenMethod
    public void readIntoParen() {
        try {
            vm.readIntoParen();
        } catch (CastingException exception) {
            throw CustomSpellFailure.from(exception);
        }
    }

    @ZenMethod
    public void mishap(String translationKey) {
        throw CustomSpellFailure.mishap(translationKey, null);
    }

    @ZenMethod
    public void mishap(String translationKey, String detail) {
        throw CustomSpellFailure.mishap(translationKey, detail);
    }

    @ZenGetter("lastMishapKey")
    @ZenMethod
    public String getLastMishapKey() {
        Mishap mishap = vm.getLastMishap();
        return mishap == null ? "" : mishap.getErrorKey();
    }

    private static Iota unwrap(HexCastingIota value) {
        if (value == null) {
            throw CustomSpellFailure.mishap("hexcasting.error.invalid_iota",
                "Custom action received a null Iota");
        }
        return value.unwrap();
    }
}
