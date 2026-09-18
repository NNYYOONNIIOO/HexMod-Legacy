package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.common.lib.HexBlocks;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** JEI 4.x display data for the edify action. */
public final class EdifyRecipeWrapper implements IRecipeWrapper {
    private final List<List<ItemStack>> inputs;
    private final List<List<ItemStack>> outputs;

    private EdifyRecipeWrapper(List<ItemStack> leaves, List<ItemStack> logs) {
        this.inputs = Collections.singletonList(Collections.singletonList(
            new ItemStack(Items.SAPLING, 1, OreDictionary.WILDCARD_VALUE)));
        this.outputs = Arrays.asList(leaves, logs);
    }

    public static List<EdifyRecipeWrapper> createRecipes() {
        List<ItemStack> leaves = new ArrayList<>();
        addBlock(leaves, "amethyst_edified_leaves");
        addBlock(leaves, "aventurine_edified_leaves");
        addBlock(leaves, "citrine_edified_leaves");

        List<ItemStack> logs = new ArrayList<>();
        addBlock(logs, "edified_log");
        addBlock(logs, "edified_log_amethyst");
        addBlock(logs, "edified_log_aventurine");
        addBlock(logs, "edified_log_citrine");

        if (leaves.isEmpty() || logs.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new EdifyRecipeWrapper(leaves, logs));
    }

    private static void addBlock(List<ItemStack> destination, String id) {
        Item item = HexBlocks.getBlockItem(id);
        if (item != null) {
            destination.add(new ItemStack(item));
        }
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, inputs);
        ingredients.setOutputLists(VanillaTypes.ITEM, outputs);
    }
}
