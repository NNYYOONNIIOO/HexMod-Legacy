package at.petra_k.hexcasting.common.misc;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.common.item.ItemHexFocus;
import at.petra_k.hexcasting.common.item.ItemSpellbook;
import at.petra_k.hexcasting.common.lib.HexItems;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

/**
 * Dynamic equivalent of Hex's seal_focus and seal_spellbook recipes.
 *
 * <p>The result must retain the complete input NBT, so a normal shaped or
 * shapeless recipe cannot represent it.  The honeycomb is optional in a
 * plain 1.12.2 installation, but becomes usable as soon as a backport
 * registers either the item or the {@code honeycomb} ore entry.</p>
 */
public final class SealRecipe implements IRecipe {
    private final ResourceLocation registryName;
    private final boolean focus;

    public SealRecipe(ResourceLocation registryName, boolean focus) {
        this.registryName = registryName;
        this.focus = focus;
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        boolean foundSealee = false;
        boolean foundHoneycomb = false;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (isSealee(stack)) {
                if (foundSealee) {
                    return false;
                }
                foundSealee = true;
            } else if (isHoneycomb(stack)) {
                if (foundHoneycomb) {
                    return false;
                }
                foundHoneycomb = true;
            } else {
                return false;
            }
        }
        return foundSealee && foundHoneycomb;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (isSealee(stack)) {
                ItemStack result = stack.copy();
                result.setCount(1);
                if (focus) {
                    ItemHexFocus.seal(result);
                } else {
                    ItemSpellbook.setSealed(result, true);
                }
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        Item item = focus ? HexItems.FOCUS : spellbookItem();
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(item);
        if (focus) {
            ItemHexFocus.seal(result);
        } else {
            ItemSpellbook.setSealed(result, true);
        }
        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inventory) {
        return NonNullList.withSize(inventory.getSizeInventory(), ItemStack.EMPTY);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        Item item = focus ? HexItems.FOCUS : spellbookItem();
        if (item != null) {
            ingredients.add(Ingredient.fromStacks(new ItemStack(item)));
        }
        Item honeycomb = ForgeRegistries.ITEMS.getValue(
            new ResourceLocation("cavesnotcliffs", "honeycomb"));
        if (honeycomb != null) {
            ingredients.add(Ingredient.fromStacks(new ItemStack(honeycomb)));
        } else {
            ingredients.add(Ingredient.EMPTY);
        }
        return ingredients;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public String getGroup() {
        return "hexcasting";
    }

    @Override
    public Class<IRecipe> getRegistryType() {
        return IRecipe.class;
    }

    @Override
    public ResourceLocation getRegistryName() {
        return registryName;
    }

    @Override
    public SealRecipe setRegistryName(ResourceLocation name) {
        return this;
    }

    private boolean isSealee(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getCount() != 1) {
            return false;
        }
        if (focus) {
            return stack.getItem() == HexItems.FOCUS
                && !ItemHexFocus.isSealed(stack)
                && HexItems.FOCUS.readIotaTag(stack) != null;
        }
        return stack.getItem() == spellbookItem()
            && !ItemSpellbook.isSealed(stack)
            && ItemSpellbook.hasIota(stack);
    }

    private static Item spellbookItem() {
        return HexItems.EXTRA_ITEMS.get("spellbook");
    }

    private static boolean isHoneycomb(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item registered = ForgeRegistries.ITEMS.getValue(
            new ResourceLocation("cavesnotcliffs", "honeycomb"));
        if (registered != null && stack.getItem() == registered) {
            return true;
        }
        for (int oreId : OreDictionary.getOreIDs(stack)) {
            if ("honeycomb".equals(OreDictionary.getOreName(oreId))) {
                return true;
            }
        }
        return false;
    }
}
