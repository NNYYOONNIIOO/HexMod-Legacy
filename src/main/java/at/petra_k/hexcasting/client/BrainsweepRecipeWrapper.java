package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.common.lib.HexBlocks;
import at.petra_k.hexcasting.common.lib.hex.BrainsweepRecipes;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** JEI 4.x display data for the recipe-driven brainsweep action. */
public final class BrainsweepRecipeWrapper implements IRecipeWrapper {
    private final BrainsweepRecipes.DisplayRecipe recipe;
    private final ItemStack blockInput;
    private final ItemStack result;
    private final List<List<ItemStack>> inputs;
    private final List<List<ItemStack>> outputs;

    private BrainsweepRecipeWrapper(BrainsweepRecipes.DisplayRecipe recipe,
                                    ItemStack blockInput, ItemStack result) {
        this.recipe = recipe;
        this.blockInput = blockInput;
        this.result = result;
        this.inputs = Collections.singletonList(Collections.singletonList(blockInput));
        this.outputs = Collections.singletonList(Collections.singletonList(result));
    }

    /** Only expose recipes whose input and result have a registered item form. */
    public static List<BrainsweepRecipeWrapper> createRecipes() {
        List<BrainsweepRecipeWrapper> wrappers = new ArrayList<>();
        for (BrainsweepRecipes.DisplayRecipe recipe : BrainsweepRecipes.displayRecipes()) {
            ItemStack input = blockStack(recipe.getBlockInputId());
            ItemStack output = blockStack(recipe.getResultId());
            if (!input.isEmpty() && !output.isEmpty()) {
                wrappers.add(new BrainsweepRecipeWrapper(recipe, input, output));
            }
        }
        return wrappers;
    }

    private static ItemStack blockStack(String id) {
        if (id == null || id.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Block block = Block.REGISTRY.getObject(new ResourceLocation(id));
        if ((block == null || block == Blocks.AIR)
            && "minecraft:amethyst_block".equals(id)) {
            // The port's dust block is the 1.12.2 stand-in for amethyst_block.
            block = Block.REGISTRY.getObject(new ResourceLocation(
                "hexcasting:amethyst_dust_block"));
        }
        if (block == null || block == Blocks.AIR) {
            return ItemStack.EMPTY;
        }
        Item item = Item.getItemFromBlock(block);
        return item == null || item == Items.AIR
            ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, inputs);
        ingredients.setOutputLists(VanillaTypes.ITEM, outputs);
    }

    @Override
    public List<String> getTooltipStrings(int mouseX, int mouseY) {
        if (mouseX < 37 || mouseX > 63 || mouseY < 19 || mouseY > 67) {
            return Collections.emptyList();
        }

        List<String> tooltip = new ArrayList<>();
        String entityType = recipe.getEntityTypeId();
        if ("minecraft:villager".equals(entityType)) {
            if (recipe.getMinLevel() >= 5) {
                tooltip.add(net.minecraft.util.text.translation.I18n.translateToLocalFormatted(
                    "hexcasting.tooltip.brainsweep.level", 5));
            } else if (recipe.getMinLevel() > 1) {
                tooltip.add(net.minecraft.util.text.translation.I18n.translateToLocalFormatted(
                    "hexcasting.tooltip.brainsweep.min_level", recipe.getMinLevel()));
            }
            if (recipe.getProfession() != null) {
                tooltip.add(localizeProfession(recipe.getProfession()));
            } else {
                tooltip.add(net.minecraft.util.text.translation.I18n.translateToLocal(
                    "entity.minecraft.villager"));
            }
        } else {
            tooltip.add(entityType == null ? "" : entityType);
        }
        return tooltip;
    }

    /**
     * Use a profession key supplied by another 1.12.2 mod when available,
     * then fall back to this mod's translations.  Modern Hex's
     * entity.minecraft.villager.<profession> keys do not exist in vanilla
     * 1.12.2, which previously left the raw key visible in JEI.
     */
    private static String localizeProfession(String profession) {
        String path = profession == null ? "" : profession.toLowerCase(java.util.Locale.ROOT);
        for (String key : new String[] {
            "entity.minecraft.villager." + path,
            "entity.villager." + path,
            "entity.Villager." + path,
            "hexcasting.jei.profession." + path
        }) {
            String translated = net.minecraft.util.text.translation.I18n.translateToLocal(key);
            if (!key.equals(translated)) {
                return translated;
            }
        }
        return path;
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight,
                         int mouseX, int mouseY) {
        if (minecraft == null || minecraft.world == null
            || !"minecraft:villager".equals(recipe.getEntityTypeId())) {
            return;
        }

        Entity entity = new EntityVillager(minecraft.world);
        RenderManager renderManager = minecraft.getRenderManager();
        Render renderer = renderManager.getEntityRenderObject(entity);
        if (renderer == null) {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(50.0F, 67.0F, 50.0F);
        GlStateManager.scale(-20.0F, 20.0F, 20.0F);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        RenderHelper.enableStandardItemLighting();
        renderer.doRender(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }
}
