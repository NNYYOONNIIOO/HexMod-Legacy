package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.common.lib.hex.BrainsweepRecipes;
import at.petra_k.hexcasting.common.lib.HexItems;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.BookPage;

import java.util.List;

/**
 * Compact 1.12.2 rendering of Hex's recipe-driven brainsweep page.
 *
 * <p>Patchouli 1.12 has no page component capable of rendering an entity
 * together with a recipe-defined result.  The recipe data is already shared
 * with JEI, so this page reuses that authoritative table and shows the input,
 * media cost, target label, and result in the same positions as the modern
 * page.  The prose remains a normal Patchouli-localized text block.</p>
 */
@SideOnly(Side.CLIENT)
public final class HexBrainsweepPage extends BookPage {
    private String recipe;
    private String text;

    private transient BrainsweepRecipes.DisplayRecipe displayRecipe;
    private transient ItemStack input = ItemStack.EMPTY;
    private transient ItemStack result = ItemStack.EMPTY;

    @Override
    public void build(BookEntry entry, int pageNum) {
        super.build(entry, pageNum);
        displayRecipe = findRecipe(recipe);
        if (displayRecipe == null) {
            return;
        }

        input = stackForId(displayRecipe.getBlockInputId());
        result = stackForId(displayRecipe.getResultId());
        if (!input.isEmpty()) {
            entry.addRelevantStack(input, pageNum);
        }
        if (!result.isEmpty()) {
            entry.addRelevantStack(result, pageNum);
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (fontRenderer == null || parent == null) {
            return;
        }

        int headerColor = book == null ? 0x202020 : book.headerColor;
        int textColor = book == null ? 0x404040 : book.textColor;
        String header = translate("hexcasting.action.brainsweep");
        parent.drawCenteredStringNoShadow(header, left + 58, top + 3,
            headerColor);

        if (displayRecipe == null) {
            fontRenderer.drawString(translate(
                "hexcasting.patchouli.recipe_unavailable"), left + 8, top + 30,
                0xAA3333);
        } else {
            if (!input.isEmpty()) {
                parent.renderItemStack(left + 12, top + 35, mouseX, mouseY,
                    input);
            }
            Item mediaItem = HexItems.EXTRA_ITEMS.get("charged_amethyst");
            if (mediaItem != null) {
                ItemStack media = new ItemStack(mediaItem,
                    mediaCount(displayRecipe.getMediaCost()), 0);
                parent.renderItemStack(left + 12, top + 55, mouseX, mouseY,
                    media);
            }
            if (!result.isEmpty()) {
                parent.renderItemStack(left + 87, top + 35, mouseX, mouseY,
                    result);
            }

            String target = targetName(displayRecipe);
            fontRenderer.drawSplitString(target, left + 34, top + 19, 57,
                textColor);
        }

        String body = translate(text);
        if (!body.isEmpty()) {
            fontRenderer.drawSplitString(body, left + 5, top + 77, 118,
                textColor);
        }
    }

    private static BrainsweepRecipes.DisplayRecipe findRecipe(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        List<BrainsweepRecipes.DisplayRecipe> recipes =
            BrainsweepRecipes.displayRecipes();
        for (BrainsweepRecipes.DisplayRecipe candidate : recipes) {
            String resultId = candidate.getResultId();
            if (id.endsWith(resultId.substring(resultId.indexOf(':') + 1))) {
                return candidate;
            }
        }
        return null;
    }

    private static ItemStack stackForId(String id) {
        if (id == null || id.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ResourceLocation location = new ResourceLocation(id);
        Item item = Item.REGISTRY.getObject(location);
        if (item == null || item == Items.AIR) {
            Block block = Block.REGISTRY.getObject(location);
            if (block != null) {
                item = Item.getItemFromBlock(block);
            }
        }
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    private static int mediaCount(long cost) {
        return Math.max(1, (int) Math.min(64L,
            Math.round(cost / 100_000.0D)));
    }

    private static String targetName(BrainsweepRecipes.DisplayRecipe recipe) {
        if (recipe.getProfession() != null) {
            String path = recipe.getProfession().toLowerCase(java.util.Locale.ROOT);
            for (String key : new String[] {
                "entity.minecraft.villager." + path,
                "entity.villager." + path,
                "entity.Villager." + path,
                "entity.Villager." + path + ".name",
                "hexcasting.jei.profession." + path
            }) {
                String translated = I18n.format(key);
                if (!key.equals(translated)) {
                    return translated;
                }
            }
            return path;
        }
        String id = recipe.getEntityTypeId();
        ResourceLocation location = new ResourceLocation(id);
        String path = location.getResourcePath();
        for (String key : new String[] {
            "entity." + location.getResourceDomain() + "." + path,
            "entity." + path,
            "entity." + capitalize(path) + ".name",
            "entity." + capitalize(path),
            "hexcasting.entity." + location.getResourceDomain() + "." + path,
            "hexcasting.entity." + path
        }) {
            String translated = I18n.format(key);
            if (!key.equals(translated)) {
                return translated;
            }
        }
        return id;
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String translate(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        String translated = I18n.format(key);
        return key.equals(translated) ? key : translated;
    }
}
