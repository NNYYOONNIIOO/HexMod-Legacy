package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.common.lib.HexBlocks;
import at.petra_k.hexcasting.common.lib.hex.BrainsweepRecipes;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

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
            ItemStack input = blockStack(recipe.getBlockInputId(),
                recipe.getBlockInputMeta());
            ItemStack output = blockStack(recipe.getResultId(),
                recipe.getResultMeta());
            if (!input.isEmpty() && !output.isEmpty()) {
                wrappers.add(new BrainsweepRecipeWrapper(recipe, input, output));
            }
        }
        return wrappers;
    }

    private static ItemStack blockStack(String id) {
        return blockStack(id, 0);
    }

    private static ItemStack blockStack(String id, int metadata) {
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
            ? ItemStack.EMPTY : new ItemStack(item, 1,
                metadata == 32767 ? 0 : metadata);
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
            String level = null;
            boolean hasNbtMinimum = villagerInteger(recipe.getEntityNbt(),
                "CareerLevel") != null;
            if (hasNbtMinimum) {
                level = net.minecraft.util.text.translation.I18n.translateToLocalFormatted(
                    "hexcasting.tooltip.brainsweep.min_level", recipe.getMinLevel());
            } else if (recipe.getMinLevel() >= 5) {
                level = net.minecraft.util.text.translation.I18n.translateToLocalFormatted(
                    "hexcasting.tooltip.brainsweep.level", 5);
            } else if (recipe.getMinLevel() > 1) {
                level = net.minecraft.util.text.translation.I18n.translateToLocalFormatted(
                    "hexcasting.tooltip.brainsweep.min_level", recipe.getMinLevel());
            }
            String career = vanillaVillagerCareer(recipe.getEntityNbt());
            String target = career == null ? localizeEntity(entityType)
                : localizeProfession(career);
            if (recipe.getProfession() != null) {
                target = localizeProfession(recipe.getProfession());
            }
            if (recipe.getEntityNameKey() != null) {
                String localized = net.minecraft.util.text.translation.I18n
                    .translateToLocal(recipe.getEntityNameKey());
                if (!recipe.getEntityNameKey().equals(localized)) {
                    target = localized;
                }
            }
            tooltip.add(level == null ? target : level + " " + target);
        } else {
            tooltip.add(localizeEntity(entityType));
        }
        if (!"minecraft:villager".equals(entityType)
            && !recipe.getEntityNbt().getKeySet().isEmpty()) {
            tooltip.add("NBT: " + recipe.getEntityNbt());
        }
        return tooltip;
    }

    private static String vanillaVillagerCareer(
        net.minecraft.nbt.NBTTagCompound nbt) {
        if (nbt == null) {
            return null;
        }
        Integer careerValue = villagerInteger(nbt, "Career");
        if (careerValue == null || careerValue <= 0) {
            return null;
        }
        int career = careerValue;

        int profession = -1;
        net.minecraft.nbt.NBTBase professionNameTag =
            villagerTag(nbt, "ProfessionName");
        if (professionNameTag instanceof net.minecraft.nbt.NBTTagString) {
            String name = ((net.minecraft.nbt.NBTTagString) professionNameTag).getString();
            try {
                profession = Integer.parseInt(name);
            } catch (NumberFormatException ignored) {
                try {
                    ResourceLocation professionId = new ResourceLocation(name);
                    if ("minecraft".equals(professionId.getResourceDomain())) {
                        profession = vanillaProfessionId(
                            professionId.getResourcePath());
                    }
                } catch (RuntimeException ignoredId) {
                    // Fall through to the numeric Profession field.
                }
            }
        }
        if (profession < 0) {
            Integer professionValue = villagerInteger(nbt, "Profession");
            if (professionValue != null) {
                profession = professionValue;
            }
        }
        if (profession < 0) {
            // Career 4 is the vanilla farmer-profession fletcher career. Keep
            // useful names for scripts which specify just this career field.
            return career == 4 ? "fletcher" : null;
        }

        switch (profession) {
            case 0:
                switch (career) {
                    case 1: return "farmer";
                    case 2: return "fisherman";
                    case 3: return "shepherd";
                    case 4: return "fletcher";
                    default: return null;
                }
            case 1:
                switch (career) {
                    case 1: return "librarian";
                    case 2: return "cartographer";
                    default: return null;
                }
            case 2:
                return career == 1 ? "cleric" : null;
            case 3:
                switch (career) {
                    case 1: return "armor";
                    case 2: return "weapon";
                    case 3: return "tool";
                    default: return null;
                }
            case 4:
                switch (career) {
                    case 1: return "butcher";
                    case 2: return "leather";
                    default: return null;
                }
            case 5:
                return career == 1 ? "nitwit" : null;
            default:
                return null;
        }
    }

    private static Integer villagerInteger(net.minecraft.nbt.NBTTagCompound nbt,
                                           String name) {
        net.minecraft.nbt.NBTBase tag = villagerTag(nbt, name);
        if (tag instanceof net.minecraft.nbt.NBTPrimitive) {
            return ((net.minecraft.nbt.NBTPrimitive) tag).getInt();
        }
        if (tag instanceof net.minecraft.nbt.NBTTagString) {
            try {
                return Integer.parseInt(((net.minecraft.nbt.NBTTagString) tag).getString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static int vanillaProfessionId(String profession) {
        switch (profession) {
            case "farmer": return 0;
            case "librarian": return 1;
            case "priest": return 2;
            case "smith": return 3;
            case "butcher": return 4;
            case "nitwit": return 5;
            default: return -1;
        }
    }

    private static net.minecraft.nbt.NBTBase villagerTag(
        net.minecraft.nbt.NBTTagCompound nbt, String expectedName) {
        for (String key : nbt.getKeySet()) {
            if (expectedName.equalsIgnoreCase(key)) {
                return nbt.getTag(key);
            }
        }
        return null;
    }

    private static String localizeEntity(String entityType) {
        if (entityType == null || entityType.isEmpty()) {
            return "";
        }
        ResourceLocation id = new ResourceLocation(entityType);
        String path = id.getResourcePath();
        String modernKey = "entity." + id.getResourceDomain() + "." + path;
        List<String> keys = new ArrayList<>();
        // Forge 1.12 entity translations use EntityEntry's registered name,
        // which may be CamelCase and therefore cannot be reconstructed from
        // the resource-location path (e.g. StrayedMirror vs strayed_mirror).
        EntityEntry entry = ForgeRegistries.ENTITIES.getValue(id);
        if (entry != null && entry.getName() != null
            && !entry.getName().isEmpty()) {
            keys.add("entity." + entry.getName() + ".name");
            keys.add("entity." + entry.getName());
            keys.add("entity." + id.getResourceDomain() + "."
                + entry.getName() + ".name");
        }
        Collections.addAll(keys,
            modernKey,
            "entity." + path,
            "entity." + capitalize(path) + ".name",
            "entity." + capitalize(path),
            "hexcasting.entity." + id.getResourceDomain() + "." + path,
            "hexcasting.entity." + path);
        for (String key : keys) {
            String translated = net.minecraft.util.text.translation.I18n.translateToLocal(key);
            if (!key.equals(translated)) {
                return translated;
            }
        }
        return entityType;
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
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
            "entity.Villager." + path + ".name",
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
        if (minecraft == null || minecraft.world == null) {
            return;
        }

        Entity entity = createDisplayEntity(minecraft.world,
            recipe.getEntityTypeId(), recipe.getEntityNbt());
        if (entity == null) {
            return;
        }
        RenderManager renderManager = minecraft.getRenderManager();

        // Keep the same angle source and transform as Hex's modern
        // RenderLib.renderEntity helper.  The client tick counter includes
        // render partial ticks, so this is smooth and has the same constant
        // one-degree-per-tick speed as the 1.20.1 preview.
        float entityRotation = HexClientTickCounter.getTotal();

        float previousViewYaw = renderManager.playerViewY;
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        try {
            // The item renderer used by the left ingredient list can leave a
            // multiplied vertex colour or the lightmap texture unit active.
            // Follow Patchouli's 1.12.2 entity-GUI path so the preview is
            // rendered with a clean white colour and a known render-manager
            // camera orientation.
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.translate(50.0F, 67.0F, 50.0F);
            GlStateManager.scale(-20.0F, 20.0F, 20.0F);
            GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(entityRotation, 0.0F, 1.0F, 0.0F);
            RenderHelper.enableStandardItemLighting();
            renderManager.playerViewY = 180.0F;
            renderManager.renderEntity(entity, 0.0D, 0.0D, 0.0D,
                0.0F, 1.0F, false);
        } finally {
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
            renderManager.playerViewY = previousViewYaw;
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableColorMaterial();
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.enableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.enableDepth();
            GlStateManager.resetColor();
        }
    }

    /**
     * JEI used to render only the vanilla villager. The Allay recipe is
     * supplied by Raids Backport, so resolve its registered entity class at
     * runtime instead of inventing a second model in Hex Casting.
     */
    private static Entity createDisplayEntity(World world, String entityType,
                                              net.minecraft.nbt.NBTTagCompound entityNbt) {
        if (world == null || entityType == null || entityType.isEmpty()) {
            return null;
        }
        if ("minecraft:villager".equals(entityType)) {
            // Keep the model preview neutral; the tooltip maps NBT careers.
            return new EntityVillager(world);
        }

        EntityEntry entry = ForgeRegistries.ENTITIES.getValue(
            new ResourceLocation(entityType));
        if (entry == null || entry.getEntityClass() == null) {
            return null;
        }
        try {
            java.lang.reflect.Constructor<? extends Entity> constructor =
                entry.getEntityClass().getDeclaredConstructor(World.class);
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            return applyDisplayNbt(constructor.newInstance(world), entityNbt);
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return null;
        }
    }

    private static Entity applyDisplayNbt(Entity entity,
                                          net.minecraft.nbt.NBTTagCompound partialNbt) {
        if (entity == null || partialNbt == null
            || partialNbt.getKeySet().isEmpty()) {
            return entity;
        }
        net.minecraft.nbt.NBTTagCompound completeNbt =
            entity.writeToNBT(new net.minecraft.nbt.NBTTagCompound());
        completeNbt.merge(partialNbt);
        entity.readFromNBT(completeNbt);
        return entity;
    }
}
