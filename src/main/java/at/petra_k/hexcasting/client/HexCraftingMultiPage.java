package at.petra_k.hexcasting.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.BookPage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 1.12.2 replacement for Hex's multi-recipe Patchouli page.
 *
 * <p>The modern page cycles through a list of recipes in one book page.  The
 * legacy Patchouli API has no equivalent page type, so this page keeps the
 * same data shape and draws one recipe at a time.  Cycling is deliberately
 * clock based, which also keeps the page useful when the book is opened from
 * a screen without a loaded world.</p>
 */
@SideOnly(Side.CLIENT)
public final class HexCraftingMultiPage extends BookPage {
    private String heading;
    private String text;
    private List<String> recipes = Collections.emptyList();

    private transient List<IRecipe> loadedRecipes = Collections.emptyList();

    @Override
    public void build(BookEntry entry, int pageNum) {
        super.build(entry, pageNum);
        List<IRecipe> loaded = new ArrayList<>();
        for (String recipeId : recipes) {
            if (recipeId == null || recipeId.isEmpty()) {
                continue;
            }
            IRecipe recipe = CraftingManager.getRecipe(
                new ResourceLocation(recipeId));
            if (recipe != null) {
                loaded.add(recipe);
                entry.addRelevantStack(recipe.getRecipeOutput(), pageNum);
            }
        }
        loadedRecipes = loaded;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (fontRenderer == null || parent == null) {
            return;
        }

        String title = translate(heading);
        if (!title.isEmpty()) {
            parent.drawCenteredStringNoShadow(title, left + 58, top + 4,
                book == null ? 0x202020 : book.headerColor);
        }

        if (!loadedRecipes.isEmpty()) {
            long cycle = Minecraft.getSystemTime() / 1800L;
            IRecipe recipe = loadedRecipes.get((int) (cycle
                % loadedRecipes.size()));
            drawRecipe(recipe, mouseX, mouseY);
        } else {
            fontRenderer.drawString("Recipe unavailable", left + 8, top + 30,
                0xAA3333);
        }

        String body = translate(text);
        if (!body.isEmpty()) {
            fontRenderer.drawSplitString(body, left + 5, top + 78, 118,
                book == null ? 0x404040 : book.textColor);
        }
    }

    private void drawRecipe(IRecipe recipe, int mouseX, int mouseY) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        int width = 3;
        if (recipe instanceof IShapedRecipe) {
            width = Math.max(1, ((IShapedRecipe) recipe).getRecipeWidth());
        }

        for (int i = 0; i < ingredients.size(); i++) {
            int x = left + 12 + (i % width) * 19;
            int y = top + 12 + (i / width) * 19;
            parent.renderIngredient(x, y, mouseX, mouseY,
                ingredients.get(i));
        }

        ItemStack output = recipe.getRecipeOutput();
        if (output != null && !output.isEmpty()) {
            parent.renderItemStack(left + 88, top + 31, mouseX, mouseY,
                output);
        }

        FontRenderer font = fontRenderer;
        font.drawString("->", left + 66, top + 32,
            book == null ? 0x404040 : book.textColor);
    }

    private static String translate(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        String translated = I18n.format(key);
        return key.equals(translated) ? key : translated;
    }
}
