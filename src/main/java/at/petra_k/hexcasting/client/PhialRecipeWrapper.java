package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.misc.MediaConstants;
import at.petra_k.hexcasting.common.item.ItemMediaBattery;
import at.petra_k.hexcasting.common.lib.HexItems;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    /**
     * Build one rotating JEI recipe.  Each displayed media material mirrors
     * the amount that the action can drain into a newly made battery.
     */
    public static List<PhialRecipeWrapper> createRecipes() {
        List<ItemStack> mediaInputs = new ArrayList<>();
        List<ItemStack> batteries = new ArrayList<>();

        addMaterial(mediaInputs, batteries,
            HexItems.EXTRA_ITEMS.get("amethyst_dust"), MediaConstants.DUST_UNIT);
        addMaterial(mediaInputs, batteries,
            HexItems.EXTRA_ITEMS.get("charged_amethyst"), MediaConstants.CRYSTAL_UNIT);
        addMaterial(mediaInputs, batteries,
            HexItems.EXTRA_ITEMS.get("quenched_allay_shard"),
            MediaConstants.QUENCHED_SHARD_UNIT);

        if (mediaInputs.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new PhialRecipeWrapper(mediaInputs, batteries));
    }

    private static void addMaterial(List<ItemStack> inputs,
                                    List<ItemStack> outputs,
                                    Item item, long mediaPerItem) {
        if (item == null || mediaPerItem <= 0L) {
            return;
        }
        for (int count : new int[] {1, 64}) {
            ItemStack input = new ItemStack(item, count);
            ItemStack battery = new ItemStack(HexItems.BATTERY);
            long media = mediaPerItem * (long) count;
            ItemMediaBattery batteryItem = HexItems.BATTERY;
            batteryItem.setMaxMedia(battery, media);
            batteryItem.setMedia(battery, media);
            inputs.add(input);
            outputs.add(battery);
        }
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, inputs);
        ingredients.setOutputLists(VanillaTypes.ITEM, outputs);
    }
}
