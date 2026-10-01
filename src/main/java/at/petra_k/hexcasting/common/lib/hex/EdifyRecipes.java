package at.petra_k.hexcasting.common.lib.hex;

import net.minecraft.block.BlockSapling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** Input and media-cost configuration for the edify action. */
public final class EdifyRecipes {
    private static final List<Recipe> RECIPES = new ArrayList<>();
    private static final List<ItemStack> SUPPRESSED_DEFAULTS = new ArrayList<>();
    private static boolean defaultsEnabled = true;

    private EdifyRecipes() {
    }

    public static synchronized boolean add(ItemStack input, long mediaCost) {
        IBlockState state = RecipeStackMatcher.blockState(input);
        if (state == null || input.getMetadata() == 32767 || mediaCost < 0L) {
            return false;
        }
        ItemStack normalized = input.copy();
        normalized.setCount(1);
        remove(normalized);
        for (Iterator<ItemStack> iterator = SUPPRESSED_DEFAULTS.iterator(); iterator.hasNext();) {
            if (sameIngredient(iterator.next(), normalized)) {
                iterator.remove();
            }
        }
        RECIPES.add(new Recipe(normalized, mediaCost));
        return true;
    }

    public static synchronized boolean remove(ItemStack input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        boolean removed = false;
        for (Iterator<Recipe> iterator = RECIPES.iterator(); iterator.hasNext();) {
            if (sameIngredient(iterator.next().input, input)) {
                iterator.remove();
                removed = true;
            }
        }
        IBlockState state = RecipeStackMatcher.blockState(input);
        if (state != null && state.getBlock() instanceof BlockSapling) {
            suppressDefault(input);
            removed = true;
        }
        return removed;
    }

    public static synchronized void removeAll() {
        RECIPES.clear();
        SUPPRESSED_DEFAULTS.clear();
        defaultsEnabled = false;
    }

    public static synchronized Match find(IBlockState state) {
        if (state == null) {
            return null;
        }
        Item item = Item.getItemFromBlock(state.getBlock());
        if (item != null && item != net.minecraft.init.Items.AIR) {
            ItemStack actual = new ItemStack(item, 1,
                state.getBlock().getMetaFromState(state));
            for (Recipe recipe : RECIPES) {
                if (RecipeStackMatcher.matches(recipe.input, actual)) {
                    return new Match(recipe.input, recipe.mediaCost);
                }
            }
            if (defaultsEnabled && state.getBlock() instanceof BlockSapling
                && !isDefaultSuppressed(actual)) {
                return new Match(new ItemStack(Item.getItemFromBlock(Blocks.SAPLING),
                    1, 32767), at.petra_k.hexcasting.api.misc.MediaConstants.CRYSTAL_UNIT);
            }
        }
        return null;
    }

    public static synchronized List<Recipe> recipes() {
        return Collections.unmodifiableList(new ArrayList<>(RECIPES));
    }

    public static synchronized boolean defaultsEnabled() {
        return defaultsEnabled;
    }

    private static boolean isDefaultSuppressed(ItemStack input) {
        for (ItemStack suppressed : SUPPRESSED_DEFAULTS) {
            if (sameIngredient(suppressed, input)) {
                return true;
            }
        }
        return false;
    }

    private static void suppressDefault(ItemStack input) {
        for (ItemStack existing : SUPPRESSED_DEFAULTS) {
            if (sameIngredient(existing, input)) {
                return;
            }
        }
        ItemStack normalized = input.copy();
        normalized.setCount(1);
        SUPPRESSED_DEFAULTS.add(normalized);
    }

    private static boolean sameIngredient(ItemStack left, ItemStack right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()
            || left.getItem() != right.getItem()) {
            return false;
        }
        return left.getMetadata() == 32767 || right.getMetadata() == 32767
            || left.getMetadata() == right.getMetadata();
    }

    public static final class Recipe {
        private final ItemStack input;
        private final long mediaCost;

        private Recipe(ItemStack input, long mediaCost) {
            this.input = input;
            this.mediaCost = mediaCost;
        }

        public ItemStack getInput() {
            return input.copy();
        }

        public long getMediaCost() {
            return mediaCost;
        }
    }

    public static final class Match {
        private final ItemStack input;
        private final long mediaCost;

        private Match(ItemStack input, long mediaCost) {
            this.input = input;
            this.mediaCost = mediaCost;
        }

        public ItemStack getInput() {
            return input.copy();
        }

        public long getMediaCost() {
            return mediaCost;
        }
    }
}
