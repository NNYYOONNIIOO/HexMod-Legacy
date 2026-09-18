package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.common.lib.HexItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import net.minecraft.item.ItemStack;

/**
 * JEI 4.x integration for the 1.12.2 port.  The modern Hex categories are
 * represented with the legacy wrapper/category API used by JEI 4.16.
 */
@JEIPlugin
public final class HexJeiPlugin implements IModPlugin {
    private static final String CRAFTING_UID = "minecraft.crafting";

    @Override
    public void register(IModRegistry registry) {
        registry.addRecipeCategories(
            new PhialRecipeCategory(registry.getJeiHelpers().getGuiHelper()),
            new EdifyRecipeCategory(registry.getJeiHelpers().getGuiHelper()),
            new BrainsweepRecipeCategory(registry.getJeiHelpers().getGuiHelper()));

        registry.addRecipes(PhialRecipeWrapper.createRecipes(), PhialRecipeCategory.UID);
        registry.addRecipes(EdifyRecipeWrapper.createRecipes(), EdifyRecipeCategory.UID);
        registry.addRecipes(BrainsweepRecipeWrapper.createRecipes(), BrainsweepRecipeCategory.UID);

        registry.addRecipeCatalyst(new ItemStack(HexItems.FOCUS), CRAFTING_UID);
        registry.addRecipeCatalyst(new ItemStack(HexItems.STAFF), CRAFTING_UID);
        registry.addRecipeCatalyst(new ItemStack(HexItems.SCRYING_LENS), CRAFTING_UID);
        registry.addRecipeCatalyst(new ItemStack(HexItems.BATTERY), CRAFTING_UID);
        registry.addRecipeCatalyst(new ItemStack(HexItems.EXTRA_ITEMS.get("scroll")), CRAFTING_UID);

        addStaffCatalysts(registry);
    }

    private static void addStaffCatalysts(IModRegistry registry) {
        registry.addRecipeCatalyst(new ItemStack(HexItems.STAFF),
            PhialRecipeCategory.UID, EdifyRecipeCategory.UID, BrainsweepRecipeCategory.UID);

        for (String id : new String[] {
            "staff/oak", "staff/spruce", "staff/birch", "staff/jungle",
            "staff/acacia", "staff/dark_oak", "staff/crimson", "staff/warped",
            "staff/mangrove", "staff/cherry", "staff/bamboo", "staff/edified",
            "staff/quenched", "staff/mindsplice"
        }) {
            if (HexItems.EXTRA_ITEMS.get(id) != null) {
                registry.addRecipeCatalyst(new ItemStack(HexItems.EXTRA_ITEMS.get(id)),
                    PhialRecipeCategory.UID, EdifyRecipeCategory.UID,
                    BrainsweepRecipeCategory.UID);
            }
        }
    }
}
