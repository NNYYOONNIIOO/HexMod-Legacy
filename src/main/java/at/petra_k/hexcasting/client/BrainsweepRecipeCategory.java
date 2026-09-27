package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.common.lib.HexItems;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Legacy JEI category for the recipe-driven brainsweep action. */
public final class BrainsweepRecipeCategory
    implements IRecipeCategory<BrainsweepRecipeWrapper> {
    public static final String UID = HexAPI.modLoc("brainsweeping").toString();

    private final IDrawable background;
    private final IDrawable icon;
    private final String title;

    public BrainsweepRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.drawableBuilder(
            HexAPI.modLoc("textures/gui/brainsweep_recipe.png"), 0, 0, 118, 86)
            .setTextureSize(128, 128)
            .build();
        // Brainsweep is a per-world great spell, so its pattern is
        // intentionally hidden in JEI.  Chargeable amethyst is the spell's
        // defining ingredient and keeps the category tab visible.
        Item chargedAmethyst = HexItems.EXTRA_ITEMS.get("charged_amethyst");
        icon = chargedAmethyst == null
            ? guiHelper.createBlankDrawable(16, 16)
            : guiHelper.createDrawableIngredient(new ItemStack(chargedAmethyst));
        title = I18n.format("hexcasting.action.brainsweep");
    }

    @Override
    public String getUid() {
        return UID;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getModName() {
        return HexAPI.MOD_NAME;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void drawExtras(Minecraft minecraft) {
        // BrainsweepRecipeWrapper draws the registered target entity in the
        // center panel (including the Raids Backport Allay).
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout,
                          BrainsweepRecipeWrapper recipeWrapper,
                          IIngredients ingredients) {
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        itemStacks.init(0, true, 11, 34);
        itemStacks.init(1, false, 86, 34);
        itemStacks.set(ingredients);
    }
}
