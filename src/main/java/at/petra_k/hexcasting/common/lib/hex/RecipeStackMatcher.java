package at.petra_k.hexcasting.common.lib.hex;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
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

    /**
     * Return the block represented by an item stack.
     *
     * <p>Most 1.12.2 block items are direct {@link ItemBlock}s.  A few legacy
     * mods create subtype ItemBlocks dynamically, however, and their item
     * registry entry is the only reliable lookup point while registries are
     * still settling.  Keep both paths so script recipes do not depend on the
     * concrete ItemBlock implementation.</p>
     */
    public static Block blockForItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        Item item = stack.getItem();
        if (item == null) {
            return null;
        }

        if (item instanceof ItemBlock) {
            Block block = ((ItemBlock) item).getBlock();
            if (block != null) {
                return block;
            }
        }

        ResourceLocation itemId = item.getRegistryName();
        if (itemId != null) {
            Block block = Block.REGISTRY.getObject(itemId);
            if (block != null && block != net.minecraft.init.Blocks.AIR) {
                return block;
            }
        }

        // Some legacy dynamic registries expose the ItemBlock before the
        // corresponding registry-name lookup is visible.  Scan the block
        // registry as a final compatibility path and compare both the item
        // identity and the unlocalized names used by 1.12.2 ItemBlocks.
        String itemName = item.getUnlocalizedName(stack);
        for (Block block : Block.REGISTRY) {
            if (block == null || block == net.minecraft.init.Blocks.AIR) {
                continue;
            }
            if (Item.getItemFromBlock(block) == item) {
                return block;
            }
            if (itemName != null && itemName.equals(block.getUnlocalizedName())) {
                return block;
            }
        }
        return null;
    }

    /** Whether the stack is backed by a registered block item. */
    public static boolean isBlockItem(ItemStack stack) {
        return blockForItem(stack) != null;
    }

    public static IBlockState blockState(ItemStack stack) {
        Block block = blockForItem(stack);
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
