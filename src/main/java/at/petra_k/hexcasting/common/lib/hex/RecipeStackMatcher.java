package at.petra_k.hexcasting.common.lib.hex;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

/** Stack identity helpers shared by the script-configurable recipe tables. */
public final class RecipeStackMatcher {
    private RecipeStackMatcher() {
    }

    /** Match the recipe item, metadata and optional NBT, with a count minimum. */
    public static boolean matches(ItemStack recipe, ItemStack actual) {
        if (recipe == null || recipe.isEmpty() || actual == null || actual.isEmpty()
            || recipe.getItem() != actual.getItem()
            || recipe.getMetadata() != 32767
                && recipe.getMetadata() != actual.getMetadata()
            || actual.getCount() < recipe.getCount()) {
            return false;
        }
        NBTTagCompound expectedTag = recipe.getTagCompound();
        return expectedTag == null || expectedTag.equals(actual.getTagCompound());
    }

    /** A remove-by-ingredient selector ignores stack count but honors NBT if supplied. */
    public static boolean matchesRemoval(ItemStack recipe, ItemStack selector) {
        if (recipe == null || recipe.isEmpty() || selector == null || selector.isEmpty()
            || recipe.getItem() != selector.getItem()
            || selector.getMetadata() != 32767
                && recipe.getMetadata() != selector.getMetadata()) {
            return false;
        }
        NBTTagCompound selectorTag = selector.getTagCompound();
        return selectorTag == null || selectorTag.equals(recipe.getTagCompound());
    }

    /** Exact recipe identity, including amount and any configured NBT. */
    public static boolean same(ItemStack left, ItemStack right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return left == null || left.isEmpty()
                ? right == null || right.isEmpty()
                : false;
        }
        return left.getItem() == right.getItem()
            && left.getMetadata() == right.getMetadata()
            && left.getCount() == right.getCount()
            && ItemStack.areItemStackTagsEqual(left, right);
    }

    public static ResourceLocation itemId(ItemStack stack) {
        return stack == null || stack.isEmpty()
            ? null : stack.getItem().getRegistryName();
    }

    public static IBlockState blockState(ItemStack stack) {
        if (stack == null || stack.isEmpty()
            || !(stack.getItem() instanceof ItemBlock)) {
            return null;
        }
        Block block = ((ItemBlock) stack.getItem()).getBlock();
        if (block == null) {
            return null;
        }
        try {
            return block.getStateFromMeta(stack.getMetadata());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static boolean matchesBlock(ItemStack recipe, IBlockState actual) {
        IBlockState expected = blockState(recipe);
        if (expected == null || actual == null
            || expected.getBlock() != actual.getBlock()) {
            return false;
        }
        return recipe.getMetadata() == OreDictionaryWildcard.VALUE
            || expected.getBlock().getMetaFromState(expected)
                == actual.getBlock().getMetaFromState(actual);
    }

    private static final class OreDictionaryWildcard {
        private static final int VALUE = 32767;
    }
}
