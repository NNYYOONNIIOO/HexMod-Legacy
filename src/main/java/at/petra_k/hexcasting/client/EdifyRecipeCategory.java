package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.common.lib.hex.HexActions;
import at.petra_k.hexcasting.interop.inline.PatternDrawable;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

/** Legacy JEI category for growing an edified tree. */
public final class EdifyRecipeCategory implements IRecipeCategory<EdifyRecipeWrapper> {
    public static final String UID = HexAPI.modLoc("edify_tree").toString();

    private final IDrawable background;
    private final IDrawable icon;
    private final String title;

    public EdifyRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.drawableBuilder(
            HexAPI.modLoc("textures/gui/edify_recipe.png"), 0, 0, 79, 61)
            .setTextureSize(128, 128)
            .build();
        icon = new PatternDrawable(HexActions.EDIFY_ID, 16, 16)
            .strokeOrder(false);
        title = I18n.format("hexcasting.action.edify");
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
        // The supplied texture contains the tree transformation glyph.
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout,
                          EdifyRecipeWrapper recipeWrapper,
                          IIngredients ingredients) {
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        itemStacks.init(0, true, 12, 22);
        itemStacks.init(1, false, 51, 10);
        itemStacks.init(2, false, 51, 35);
        itemStacks.set(ingredients);
    }
}
