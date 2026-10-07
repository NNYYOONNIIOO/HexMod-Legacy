package at.petra_k.hexcasting.interop.crafttweaker;

import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.Mishap;
import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;
import at.petra_k.hexcasting.api.casting.math.HexDir;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.lib.hex.BrainsweepRecipes;
import at.petra_k.hexcasting.common.lib.hex.CraftPhialRecipes;
import at.petra_k.hexcasting.common.lib.hex.CustomMediaValues;
import at.petra_k.hexcasting.common.lib.hex.EdifyRecipes;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import at.petra_k.hexcasting.common.world.PerWorldPatternData;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.IAction;
import crafttweaker.annotations.ModOnly;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.data.IData;
import crafttweaker.api.entity.IEntityDefinition;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/** Optional ZenScript API. CraftTweaker is deliberately not a hard dependency. */
@ModOnly("crafttweaker")
@ZenClass("mods.hexcasting.CraftPhial")
@ZenRegister
public final class HexCastingTweaker {
    private static final Logger LOGGER = LogManager.getLogger("HexCastingCraftTweaker");

    private HexCastingTweaker() {
    }

    @ZenMethod
    public static void add(IItemStack droppedInput, IItemStack heldInput,
                           long mediaCost, IItemStack output) {
        final ItemStack dropped = stack(droppedInput);
        final ItemStack held = stack(heldInput);
        final ItemStack result = stack(output);
        submit("Adding Hex Casting craft phial recipe", () -> {
            if (!CraftPhialRecipes.add(dropped, held, mediaCost, result)) {
                warn("Skipped craft phial recipe: inputs, output and non-negative media are required.");
            }
        });
    }

    @ZenMethod
    public static void remove(IItemStack droppedInput, IItemStack heldInput,
                              IItemStack output) {
        final ItemStack dropped = stack(droppedInput);
        final ItemStack held = stack(heldInput);
        final ItemStack result = stack(output);
        submit("Removing Hex Casting craft phial recipe", () ->
            CraftPhialRecipes.remove(dropped, held, result));
    }

    @ZenMethod
    public static void removeInput(IItemStack droppedInput) {
        final ItemStack input = stack(droppedInput);
        submit("Removing Hex Casting craft phial recipes by dropped input", () ->
            CraftPhialRecipes.removeInput(input));
    }

    @ZenMethod
    public static void removeOutput(IItemStack output) {
        final ItemStack result = stack(output);
        submit("Removing Hex Casting craft phial recipes by output", () ->
            CraftPhialRecipes.removeOutput(result));
    }

    @ZenClass("mods.hexcasting.Edify")
    @ZenRegister
    @ModOnly("crafttweaker")
    public static final class Edify {
        private Edify() {
        }

        @ZenMethod
        public static void add(IItemStack input, long mediaCost) {
            final ItemStack stack = stack(input);
            submit("Adding Hex Casting edify input " + stack, () -> {
                if (!EdifyRecipes.add(stack, mediaCost)) {
                    warn("Skipped edify input " + stack
                        + ": it must be an item with a concrete block form and non-negative media cost.");
                }
            });
        }

        @ZenMethod
        public static void remove(IItemStack input) {
            final ItemStack stack = stack(input);
            submit("Removing Hex Casting edify input " + stack, () ->
                EdifyRecipes.remove(stack));
        }

        @ZenMethod
        public static void removeAll() {
            submit("Removing all Hex Casting edify inputs", EdifyRecipes::removeAll);
        }
    }

    @ZenClass("mods.hexcasting.Brainsweep")
    @ZenRegister
    @ModOnly("crafttweaker")
    public static final class Brainsweep {
        private Brainsweep() {
        }

        @ZenMethod
        public static void add(IItemStack input, IEntityDefinition entity,
                               long mediaCost, IItemStack output) {
            addWithNbt(input, entity, null, mediaCost, output);
        }

        @ZenMethod
        public static void addWithNbt(IItemStack input, IEntityDefinition entity,
                                      IData nbt, long mediaCost,
                                      IItemStack output) {
            addWithNbt(input, entity, nbt, mediaCost, output, null);
        }

        @ZenMethod
        public static void addWithNbt(IItemStack input, IEntityDefinition entity,
                                      IData nbt, long mediaCost,
                                      IItemStack output, String entityNameKey) {
            final ItemStack in = stack(input);
            final ItemStack out = stack(output);
            final String entityId = entity == null ? null : entity.getId();
            final NBTTagCompound entityNbt = nbt(nbt);
            final String displayKey = entityNameKey;
            addCustom(in, entityId, entityNbt, mediaCost, out, displayKey);
        }

        /**
         * Register a recipe by resource-location string.
         *
         * <p>CraftTweaker 1.12 rebuilds its entity bracket cache during a
         * recipe registry event. Mods which call EntityRegistry.registerModEntity
         * during init can therefore make a valid {@code <entity:...>} bracket
         * evaluate to null. Keeping this string form lets scripts refer to the
         * runtime registry ID without depending on that cache.</p>
         */
        @ZenMethod
        public static void addById(IItemStack input, String entityId,
                                   long mediaCost, IItemStack output) {
            addWithNbtById(input, entityId, null, mediaCost, output, null);
        }

        @ZenMethod
        public static void addWithNbtById(IItemStack input, String entityId,
                                          IData nbt, long mediaCost,
                                          IItemStack output) {
            addWithNbtById(input, entityId, nbt, mediaCost, output, null);
        }

        @ZenMethod
        public static void addWithNbtById(IItemStack input, String entityId,
                                          IData nbt, long mediaCost,
                                          IItemStack output,
                                          String entityNameKey) {
            final ItemStack in = stack(input);
            final ItemStack out = stack(output);
            final NBTTagCompound entityNbt = nbt(nbt);
            addCustom(in, entityId, entityNbt, mediaCost, out, entityNameKey);
        }

        @ZenMethod
        public static void removeInput(IItemStack input) {
            final ItemStack stack = stack(input);
            submit("Removing Hex Casting brainsweep recipes by input", () ->
                BrainsweepRecipes.removeCustomInput(stack));
        }

        @ZenMethod
        public static void removeOutput(IItemStack output) {
            final ItemStack stack = stack(output);
            submit("Removing Hex Casting brainsweep recipes by output", () ->
                BrainsweepRecipes.removeCustomOutput(stack));
        }

        @ZenMethod
        public static void remove(IItemStack input, IEntityDefinition entity,
                                  long mediaCost, IItemStack output) {
            removeWithNbt(input, entity, null, mediaCost, output);
        }

        @ZenMethod
        public static void removeWithNbt(IItemStack input, IEntityDefinition entity,
                                         IData nbt, long mediaCost,
                                         IItemStack output) {
            final ItemStack in = stack(input);
            final ItemStack out = stack(output);
            final String entityId = entity == null ? null : entity.getId();
            final NBTTagCompound entityNbt = nbt(nbt);
            submit("Removing exact Hex Casting brainsweep recipe", () ->
                BrainsweepRecipes.removeCustom(in, entityId, entityNbt,
                    mediaCost, out));
        }

        @ZenMethod
        public static void removeById(IItemStack input, String entityId,
                                      long mediaCost, IItemStack output) {
            removeWithNbtById(input, entityId, null, mediaCost, output);
        }

        @ZenMethod
        public static void removeWithNbtById(IItemStack input, String entityId,
                                             IData nbt, long mediaCost,
                                             IItemStack output) {
            final ItemStack in = stack(input);
            final ItemStack out = stack(output);
            final NBTTagCompound entityNbt = nbt(nbt);
            submit("Removing exact Hex Casting brainsweep recipe", () ->
                BrainsweepRecipes.removeCustom(in, entityId, entityNbt,
                    mediaCost, out));
        }

        private static void addCustom(ItemStack input, String entityId,
                                      NBTTagCompound entityNbt, long mediaCost,
                                      ItemStack output, String entityNameKey) {
            submit("Adding Hex Casting brainsweep recipe " + input + " -> " + output,
                () -> {
                    if (!BrainsweepRecipes.addCustom(input, entityId, entityNbt,
                        mediaCost, output, entityNameKey)) {
                        warn("Skipped brainsweep recipe " + input + " -> " + output
                            + ": input/output and entity id are required, and media must be non-negative."
                            + " Use addById for entities registered during mod initialization.");
                    }
                });
        }
    }

    @ZenClass("mods.hexcasting.CustomSpells")
    @ZenRegister
    @ModOnly("crafttweaker")
    public static final class CustomSpells {
        private CustomSpells() {
        }

        @ZenMethod
        public static void register(String id, String startDirection,
                                    String angleSequence,
                                    ICustomSpellAction action) {
            registerWithOptions(id, startDirection, angleSequence,
                false, false, action);
        }

        @ZenMethod
        public static void registerGreat(String id, String startDirection,
                                         String angleSequence,
                                         ICustomSpellAction action) {
            registerWithOptions(id, startDirection, angleSequence,
                true, false, action);
        }

        @ZenMethod
        public static void registerWithOptions(String id, String startDirection,
                                               String angleSequence,
                                               boolean great,
                                               boolean executesInParentheses,
                                               ICustomSpellAction action) {
            final String spellId = id;
            final String direction = startDirection;
            final String angles = angleSequence;
            final ICustomSpellAction callback = action;
            submit("Registering custom Hex action " + spellId, () ->
                registerNow(spellId, direction, angles, great,
                    executesInParentheses, callback));
        }

        private static void registerNow(String id, String startDirection,
                                        String angleSequence, boolean great,
                                        boolean executesInParentheses,
                                        ICustomSpellAction callback) {
            if (id == null || id.trim().isEmpty() || !id.contains(":")) {
                warn("Skipped custom Hex action: use a namespaced id such as my_pack:my_action.");
                return;
            }
            if (callback == null) {
                warn("Skipped custom Hex action " + id + ": callback is missing.");
                return;
            }

            final ResourceLocation actionId;
            final HexPattern pattern;
            try {
                actionId = new ResourceLocation(id.trim());
                HexDir direction = parseDirection(startDirection);
                pattern = HexPattern.fromAngles(angleSequence, direction);
            } catch (RuntimeException exception) {
                warn("Skipped custom Hex action " + id + ": "
                    + exception.getMessage());
                return;
            }

            HexActionRegistry.bootstrap();
            if (HexActionRegistry.get(actionId) != null) {
                warn("Skipped custom Hex action " + actionId
                    + ": that action id is already registered.");
                return;
            }
            if (HexActionRegistry.get(pattern) != null) {
                warn("Skipped custom Hex action " + actionId
                    + ": that pattern is already registered.");
                return;
            }

            HexAction custom = new HexAction() {
                @Override
                public void execute(
                    at.petra_k.hexcasting.api.casting.eval.CastingStack stack)
                    throws CastingException {
                    execute(stack, new CastingVM(stack));
                }

                @Override
                public void execute(at.petra_k.hexcasting.api.casting.eval.CastingStack stack,
                                    CastingVM vm) throws CastingException {
                    try {
                        callback.cast(new HexCastingStack(stack),
                            new HexCastingEnvironment(vm, actionId),
                            new HexCastingImage(vm));
                    } catch (CustomSpellFailure failure) {
                        if (failure.getCastingException() != null) {
                            throw failure.getCastingException();
                        }
                        throw Mishap.invalidValue("hexcasting.error.invalid_value",
                            failure.getMessage());
                    } catch (RuntimeException exception) {
                        LOGGER.error("Custom Hex action {} threw an unexpected exception",
                            actionId, exception);
                        throw exception;
                    }
                }

                @Override
                public boolean executesInParentheses() {
                    return executesInParentheses;
                }
            };

            try {
                HexActionRegistry.register(actionId, pattern, custom);
                if (great) {
                    PerWorldPatternData.registerCustomPerWorldAction(actionId);
                }
            } catch (RuntimeException exception) {
                warn("Could not register custom Hex action " + actionId + ": "
                    + exception.getMessage());
            }
        }

        private static HexDir parseDirection(String value) {
            if (value == null) {
                throw new IllegalArgumentException("start direction is required");
            }
            String normalized = value.trim().toUpperCase(java.util.Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
            try {
                return HexDir.valueOf(normalized);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("unknown start direction: " + value,
                    exception);
            }
        }
    }

    @ZenClass("mods.hexcasting.Media")
    @ZenRegister
    @ModOnly("crafttweaker")
    public static final class Media {
        private Media() {
        }

        @ZenMethod
        public static void add(IItemStack input, long mediaPerItem) {
            final ItemStack stack = stack(input);
            submit("Setting Hex Casting media for " + stack, () -> {
                if (!CustomMediaValues.add(stack, mediaPerItem)) {
                    warn("Skipped media value for " + stack
                        + ": provide an item and a non-negative media amount.");
                }
            });
        }

        @ZenMethod
        public static void remove(IItemStack input, long mediaPerItem) {
            final ItemStack stack = stack(input);
            submit("Removing Hex Casting media value for " + stack, () ->
                CustomMediaValues.remove(stack, mediaPerItem));
        }
    }

    private static ItemStack stack(IItemStack stack) {
        return stack == null ? ItemStack.EMPTY
            : CraftTweakerMC.getItemStack(stack).copy();
    }

    private static NBTTagCompound nbt(IData data) {
        if (data == null) {
            return new NBTTagCompound();
        }
        NBTTagCompound compound = CraftTweakerMC.getNBTCompound(data);
        return compound == null ? new NBTTagCompound() : compound.copy();
    }

    private static void submit(String description, Runnable action) {
        CraftTweakerAPI.apply(new IAction() {
            @Override
            public void apply() {
                action.run();
            }

            @Override
            public String describe() {
                return description;
            }
        });
    }

    private static void warn(String message) {
        CraftTweakerAPI.logWarning("[Hex Casting] " + message);
    }
}
