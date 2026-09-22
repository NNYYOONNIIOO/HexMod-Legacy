package at.petra_k.hexcasting.common.misc;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.common.lib.HexItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Optional wood integrations for the variant staff recipes. */
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID)
public final class StaffCompatRecipes {
    private static final String CHERRY_MOD = "suikecherry";
    private static final String BAMBOO_MOD = "bamboodecor";
    private static final String NETHER_BACKPORT_MOD = "nb";

    private StaffCompatRecipes() {
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        registerIfLoaded(event, CHERRY_MOD, "cherry_planks", "cherry");
        registerIfLoaded(event, BAMBOO_MOD, "bamboo_planks", "bamboo");

        // Use Unseens Nether Backport when it is present.  Farmers' Future
        // Delight is the fallback provider, so either optional mod can supply
        // the recipe on its own, while nb wins when both are installed.
        String plankMod = Loader.isModLoaded(NETHER_BACKPORT_MOD)
            ? NETHER_BACKPORT_MOD
            : Loader.isModLoaded(AmethystCompat.FARMERS_FUTURE_DELIGHT)
                ? AmethystCompat.FARMERS_FUTURE_DELIGHT
                : null;
        if (plankMod != null) {
            register(event, plankMod, "crimson_planks", "crimson");
            register(event, plankMod, "warped_planks", "warped");
        }
    }

    private static void registerIfLoaded(RegistryEvent.Register<IRecipe> event,
                                          String modId,
                                          String plankId,
                                          String staffVariant) {
        if (Loader.isModLoaded(modId)) {
            register(event, modId, plankId, staffVariant);
        }
    }

    private static void register(RegistryEvent.Register<IRecipe> event,
                                 String plankMod,
                                 String plankId,
                                 String staffVariant) {
        Item planks = ForgeRegistries.ITEMS.getValue(new ResourceLocation(plankMod, plankId));
        Item staff = HexItems.EXTRA_ITEMS.get("staff/" + staffVariant);
        if (planks == null || staff == null) {
            return;
        }

        // Keep the same IDs used by the modern recipes and by the Patchouli
        // staff page.  Only one provider is selected for each variant, so the
        // ID remains unique even when multiple compatibility mods are loaded.
        ResourceLocation recipeId = new ResourceLocation(
            HexAPI.MOD_ID, "staff/" + staffVariant);
        // Modern Hex uses a diagonal staff shape.  The focus is not the
        // recipe's centre ingredient: a charged amethyst is mounted at the
        // upper end, while the selected wood makes up the handle.
        ShapedOreRecipe recipe = new ShapedOreRecipe(
            recipeId,
            new ItemStack(staff),
            " SA",
            " WS",
            "S  ",
            'S', new ItemStack(net.minecraft.init.Items.STICK),
            'W', new ItemStack(planks),
            'A', new ItemStack(HexItems.EXTRA_ITEMS.get("charged_amethyst")));
        recipe.setRegistryName(recipeId);
        event.getRegistry().register(recipe);
    }
}
