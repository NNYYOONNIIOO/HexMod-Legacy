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
import net.minecraft.item.ItemStack;

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
        // Craft Phial is a per-world great spell, so its pattern is
        // intentionally hidden in JEI.  Use the produced item as the
        // category icon instead of leaving the tab blank.
        icon = guiHelper.createDrawableIngredient(new ItemStack(HexItems.BATTERY));
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
        itemStacks.init(0, true, 11, 11);
        itemStacks.init(1, true, 46, 11);
        itemStacks.init(2, false, 84, 11);
        itemStacks.set(ingredients);
    }
}
