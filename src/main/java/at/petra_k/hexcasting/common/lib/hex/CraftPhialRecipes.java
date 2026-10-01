package at.petra_k.hexcasting.common.lib.hex;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** Custom item-combination recipes for the craft/battery action. */
public final class CraftPhialRecipes {
    private static final List<Recipe> RECIPES = new ArrayList<>();

    private CraftPhialRecipes() {
    }

    public static synchronized boolean add(ItemStack droppedInput,
                                           ItemStack heldInput,
                                           long mediaCost,
                                           ItemStack output) {
        if (invalid(droppedInput) || invalid(heldInput) || invalid(output)
            || mediaCost < 0L) {
            return false;
        }
        Recipe recipe = new Recipe(droppedInput.copy(), heldInput.copy(),
            mediaCost, output.copy());
        removeMatchingInputs(recipe.getDroppedInput(), recipe.getHeldInput());
        RECIPES.add(0, recipe);
        return true;
    }

    public static synchronized boolean remove(ItemStack droppedInput,
                                              ItemStack heldInput,
                                              ItemStack output) {
        boolean removed = false;
        for (Iterator<Recipe> iterator = RECIPES.iterator(); iterator.hasNext();) {
            Recipe recipe = iterator.next();
            if (RecipeStackMatcher.same(recipe.droppedInput, droppedInput)
                && RecipeStackMatcher.same(recipe.heldInput, heldInput)
                && RecipeStackMatcher.same(recipe.output, output)) {
                iterator.remove();
                removed = true;
            }
        }
        return removed;
    }

    /** Remove every recipe whose dropped-item ingredient matches this item. */
    public static synchronized boolean removeInput(ItemStack input) {
        boolean removed = false;
        for (Iterator<Recipe> iterator = RECIPES.iterator(); iterator.hasNext();) {
            if (RecipeStackMatcher.matchesRemoval(
                iterator.next().droppedInput, input)) {
                iterator.remove();
                removed = true;
            }
        }
        return removed;
    }

    public static synchronized boolean removeOutput(ItemStack output) {
        boolean removed = false;
        for (Iterator<Recipe> iterator = RECIPES.iterator(); iterator.hasNext();) {
            if (RecipeStackMatcher.matchesRemoval(iterator.next().output, output)) {
                iterator.remove();
                removed = true;
            }
        }
        return removed;
    }

    public static synchronized Match find(ItemStack droppedInput,
                                          ItemStack heldInput) {
        for (Recipe recipe : RECIPES) {
            if (RecipeStackMatcher.matches(recipe.droppedInput, droppedInput)
                && RecipeStackMatcher.matches(recipe.heldInput, heldInput)) {
                return new Match(recipe);
            }
        }
        return null;
    }

    /** Find a recipe by its dropped ingredient, regardless of the held item. */
    public static synchronized Match findByDroppedInput(ItemStack droppedInput) {
        for (Recipe recipe : RECIPES) {
            if (RecipeStackMatcher.matches(recipe.droppedInput, droppedInput)) {
                return new Match(recipe);
            }
        }
        return null;
    }

    public static synchronized List<Recipe> recipes() {
        return Collections.unmodifiableList(new ArrayList<>(RECIPES));
    }

    private static void removeMatchingInputs(ItemStack droppedInput,
                                            ItemStack heldInput) {
        for (Iterator<Recipe> iterator = RECIPES.iterator(); iterator.hasNext();) {
            Recipe recipe = iterator.next();
            if (RecipeStackMatcher.same(recipe.droppedInput, droppedInput)
                && RecipeStackMatcher.same(recipe.heldInput, heldInput)) {
                iterator.remove();
            }
        }
    }

    private static boolean invalid(ItemStack stack) {
        return stack == null || stack.isEmpty();
    }

    public static final class Recipe {
        private final ItemStack droppedInput;
        private final ItemStack heldInput;
        private final long mediaCost;
        private final ItemStack output;

        private Recipe(ItemStack droppedInput, ItemStack heldInput,
                       long mediaCost, ItemStack output) {
            this.droppedInput = droppedInput;
            this.heldInput = heldInput;
            this.mediaCost = mediaCost;
            this.output = output;
        }

        public ItemStack getDroppedInput() {
            return droppedInput.copy();
        }

        public ItemStack getHeldInput() {
            return heldInput.copy();
        }

        public long getMediaCost() {
            return mediaCost;
        }

        public ItemStack getOutput() {
            return output.copy();
        }
    }

    public static final class Match {
        private final Recipe recipe;

        private Match(Recipe recipe) {
            this.recipe = recipe;
        }

        public ItemStack getDroppedInput() {
            return recipe.getDroppedInput();
        }

        public ItemStack getHeldInput() {
            return recipe.getHeldInput();
        }

        public long getMediaCost() {
            return recipe.getMediaCost();
        }

        public ItemStack getOutput() {
            return recipe.getOutput();
        }
    }
}
