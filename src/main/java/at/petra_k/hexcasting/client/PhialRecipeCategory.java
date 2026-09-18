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

/** Legacy JEI category for crafting a media battery. */
public final class PhialRecipeCategory implements IRecipeCategory<PhialRecipeWrapper> {
    public static final String UID = HexAPI.modLoc("craft_phial").toString();

    private final IDrawable background;
    private final IDrawable icon;
    private final String title;

    public PhialRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.drawableBuilder(
            HexAPI.modLoc("textures/gui/phial_recipe.png"), 0, 0, 113, 40)
            .setTextureSize(128, 128)
            .build();
        icon = new PatternDrawable(HexActions.CRAFT_BATTERY_ID, 12, 12);
        title = I18n.format("hexcasting.action.craft/battery");
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
        // The supplied texture already contains the bottle-to-battery arrow.
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout,
                          PhialRecipeWrapper recipeWrapper,
                          IIngredients ingredients) {
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        itemStacks.init(0, true, 12, 12);
        itemStacks.init(1, true, 47, 12);
        itemStacks.init(2, false, 85, 12);
        itemStacks.set(ingredients);
    }
}
