package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.misc.MediaConstants;
import at.petra_k.hexcasting.common.item.ItemMediaBattery;
import at.petra_k.hexcasting.common.lib.HexBlocks;
import at.petra_k.hexcasting.common.lib.HexItems;
import at.petra_k.hexcasting.common.lib.hex.CraftPhialRecipes;
import at.petra_k.hexcasting.common.lib.hex.CustomMediaValues;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** JEI 4.x display data for the craft/battery action. */
public final class PhialRecipeWrapper implements IRecipeWrapper {
    private final List<List<ItemStack>> inputs;
    private final List<List<ItemStack>> outputs;

    private PhialRecipeWrapper(List<ItemStack> mediaInputs,
                               List<ItemStack> batteries) {
        this.inputs = new ArrayList<>();
        this.inputs.add(mediaInputs);
        this.inputs.add(Collections.singletonList(new ItemStack(Items.GLASS_BOTTLE)));
        this.outputs = Collections.singletonList(batteries);
    }

    private PhialRecipeWrapper(ItemStack droppedInput, ItemStack heldInput,
                               ItemStack output) {
        this.inputs = new ArrayList<>();
        this.inputs.add(Collections.singletonList(droppedInput));
        this.inputs.add(Collections.singletonList(heldInput));
        this.outputs = Collections.singletonList(
            Collections.singletonList(output));
    }

    /**
     * Build one rotating JEI recipe.  Each displayed media material mirrors
     * the amount that the action can drain into a newly made battery.
     */
    public static List<PhialRecipeWrapper> createRecipes() {
        List<ItemStack> mediaInputs = new ArrayList<>();
        List<ItemStack> batteries = new ArrayList<>();
        Set<String> knownMediaItems = new HashSet<>();

        addMaterial(mediaInputs, batteries,
            itemStack(HexItems.EXTRA_ITEMS.get("amethyst_dust")),
            MediaConstants.DUST_UNIT,
            knownMediaItems);
        addMaterial(mediaInputs, batteries,
            itemStack(HexItems.EXTRA_ITEMS.get("charged_amethyst")),
            MediaConstants.CRYSTAL_UNIT,
            knownMediaItems);
        addMaterial(mediaInputs, batteries,
            itemStack(HexItems.EXTRA_ITEMS.get("quenched_allay_shard")),
            MediaConstants.QUENCHED_SHARD_UNIT, knownMediaItems);
        addMaterial(mediaInputs, batteries,
            itemStack(HexBlocks.getBlockItem("quenched_allay")),
            MediaConstants.QUENCHED_BLOCK_UNIT, knownMediaItems);

        for (CustomMediaValues.Entry entry : CustomMediaValues.entries()) {
            ItemStack custom = entry.getStack();
            if (knownMediaItems.add(itemKey(custom))) {
                addMaterial(mediaInputs, batteries, custom,
                    entry.getMediaPerItem(), knownMediaItems);
            }
        }

        List<PhialRecipeWrapper> recipes = new ArrayList<>();
        if (!mediaInputs.isEmpty()) {
            recipes.add(new PhialRecipeWrapper(mediaInputs, batteries));
        }
        for (CraftPhialRecipes.Recipe recipe : CraftPhialRecipes.recipes()) {
            recipes.add(new PhialRecipeWrapper(recipe.getDroppedInput(),
                recipe.getHeldInput(), recipe.getOutput()));
        }
        return recipes;
    }

    private static void addMaterial(List<ItemStack> inputs,
                                    List<ItemStack> outputs,
                                    ItemStack prototype, long defaultMediaPerItem,
                                    Set<String> knownMediaItems) {
        if (prototype == null || prototype.isEmpty()) {
            return;
        }
        Item item = prototype.getItem();
        String key = itemKey(prototype);
        long mediaPerItem = CustomMediaValues.has(prototype)
            ? CustomMediaValues.get(prototype) : defaultMediaPerItem;
        if (mediaPerItem <= 0L) {
            return;
        }
        knownMediaItems.add(key);
        for (int count : new int[] {1, 64}) {
            ItemStack input = new ItemStack(item, count, prototype.getMetadata());
            ItemStack battery = new ItemStack(HexItems.BATTERY);
            long media = mediaPerItem > Long.MAX_VALUE / count
                ? Long.MAX_VALUE : mediaPerItem * (long) count;
            ItemMediaBattery batteryItem = HexItems.BATTERY;
            batteryItem.setMaxMedia(battery, media);
            batteryItem.setMedia(battery, media);
            inputs.add(input);
            outputs.add(battery);
        }
    }

    private static String itemKey(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null) {
            return "";
        }
        return stack.getItem().getRegistryName().toString() + "#" + stack.getMetadata();
    }

    private static ItemStack itemStack(Item item) {
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, inputs);
        ingredients.setOutputLists(VanillaTypes.ITEM, outputs);
    }
}
