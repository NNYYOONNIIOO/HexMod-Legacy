package at.petra_k.hexcasting.interop.crafttweaker;

import crafttweaker.annotations.ModOnly;
import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/** ZenScript callback invoked for one registered custom Hex action. */
@FunctionalInterface
@ModOnly("crafttweaker")
@ZenClass("mods.hexcasting.api.ICustomSpellAction")
@ZenRegister
public interface ICustomSpellAction {
    @ZenMethod
    void cast(HexCastingStack stack, HexCastingEnvironment environment,
              HexCastingImage image);
}
