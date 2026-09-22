package at.petra_k.hexcasting.common.misc;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.common.lib.HexBlocks;
import at.petra_k.hexcasting.common.lib.HexItems;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The 1.12.2 counterpart of Hex's generated crafting recipe data.
 *
 * <p>Modern Hex recipes use data-pack tags and item names which do not exist
 * in 1.12.2 (for example {@code minecraft:amethyst_shard} and the separate
 * dye items).  Registering the recipes here lets the legacy game use Forge's
 * ore dictionary while keeping the original recipe ids, so Patchouli pages
 * and JEI can still look them up by the modern ids.</p>
 */
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID)
public final class HexRecipeRegistry {
    private HexRecipeRegistry() {
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        ItemStack dust = hexItem("amethyst_dust");
        ItemStack charged = hexItem("charged_amethyst");
        ItemStack quenchedShard = hexItem("quenched_allay_shard");
        ItemStack amethystShard = selectedAmethystShard();
        Object shard = oreOr("gemAmethyst", amethystShard);
        Object amethystBlock = oreOr("blockAmethyst", selectedAmethystBlock());
        Object deepslate = oreOr("blockDeepslate", new ItemStack(Blocks.STONE));
        Object copper = oreOr("ingotCopper", ItemStack.EMPTY);
        Object rawIron = oreOr("rawIron", ItemStack.EMPTY);
        Object rawCopper = oreOr("rawCopper", ItemStack.EMPTY);
        Object glowstoneDust = oreOr("dustGlowstone", new ItemStack(Items.GLOWSTONE_DUST));
        Object leather = oreOr("leather", new ItemStack(Items.LEATHER));
        Object ironIngot = oreOr("ingotIron", new ItemStack(Items.IRON_INGOT));
        Object goldIngot = oreOr("ingotGold", new ItemStack(Items.GOLD_INGOT));
        Object ironNugget = oreOr("nuggetIron", new ItemStack(Items.IRON_NUGGET));
        Object goldNugget = oreOr("nuggetGold", new ItemStack(Items.GOLD_NUGGET));

        // Focuses, spell containers, tools, and basic Hex items.
        shaped(event, "focus", hexItem("focus"),
            new String[] {"GLG", "PAP", "GLG"},
            'A', charged, 'G', glowstoneDust, 'L', leather, 'P', Items.PAPER);
        shaped(event, "focus_rotated", hexItem("focus"),
            new String[] {"GPG", "LAL", "GPG"},
            'A', charged, 'G', glowstoneDust, 'L', leather, 'P', Items.PAPER);
        shaped(event, "lens", hexItem("lens"),
            new String[] {" C ", "CIC", " C "},
            'C', Blocks.GLASS, 'I', dust);
        shaped(event, "abacus", hexItem("abacus"),
            new String[] {"WAW", "SAS", "WAW"},
            'A', shard, 'S', Items.STICK, 'W', ore("plankWood"));
        shaped(event, "cypher", hexItem("cypher"),
            new String[] {" C ", "CIC", " C "},
            'C', ore("ingotCopper"), 'I', dust);
        shaped(event, "trinket", hexItem("trinket"),
            new String[] {" C ", "CIC", " C "},
            'C', ironIngot, 'I', shard);
        shaped(event, "artifact", hexItem("artifact"),
            new String[] {" F ", "FAF", " D "},
            'A', charged, 'D', musicDiscs(), 'F', goldIngot);
        shaped(event, "spellbook", hexItem("spellbook"),
            new String[] {"NBA", "NFA", "NBA"},
            'A', charged, 'B', Items.WRITABLE_BOOK, 'F', Items.CHORUS_FRUIT,
            'N', goldNugget);
        shaped(event, "jeweler_hammer", hexItem("jeweler_hammer"),
            new String[] {"IAN", " S ", " S "},
            'A', shard, 'I', ironIngot, 'N', ironNugget,
            'S', Arrays.asList(new ItemStack(Items.STICK), ore("rodWooden")));
        shapeless(event, "thought_knot", hexItem("thought_knot"), dust, Items.STRING);
        shaped(event, "sub_sandwich", hexItem("sub_sandwich"),
            new String[] {" SA", " C ", " B "},
            'A', shard, 'B', Items.BREAD, 'C', Items.COOKED_BEEF, 'S', Items.STICK);

        // Scrolls and their paper blocks.
        shaped(event, "scroll_small", hexItem("scroll_small"),
            new String[] {" A", "P "}, 'A', dust, 'P', Items.PAPER);
        shaped(event, "scroll_medium", hexItem("scroll_medium"),
            new String[] {"  A", "PP ", "PP "}, 'A', dust, 'P', Items.PAPER);
        shaped(event, "scroll", hexItem("scroll"),
            new String[] {"PPA", "PPP", "PPP"}, 'A', dust, 'P', Items.PAPER);
        shaped(event, "scroll_paper", blockItem("scroll_paper"), 8,
            new String[] {"DCD", "CIC", "DCD"},
            'C', Items.PAPER, 'D', Items.PAPER, 'I', shard);
        shaped(event, "scroll_paper_lantern", blockItem("scroll_paper_lantern"),
            new String[] {"T", "B"}, 'B', Blocks.TORCH, 'T', blockItem("scroll_paper"));
        shapelessCount(event, "ancient_scroll_paper", blockItem("ancient_scroll_paper"), 8,
            dye(EnumDyeColor.BROWN), blockItem("scroll_paper"), blockItem("scroll_paper"),
            blockItem("scroll_paper"), blockItem("scroll_paper"), blockItem("scroll_paper"),
            blockItem("scroll_paper"), blockItem("scroll_paper"), blockItem("scroll_paper"));
        shaped(event, "ancient_scroll_paper_lantern", blockItem("ancient_scroll_paper_lantern"),
            new String[] {"T", "B"}, 'B', Blocks.TORCH, 'T', blockItem("ancient_scroll_paper"));
        shapelessCount(event, "ageing_scroll_paper_lantern", blockItem("ancient_scroll_paper_lantern"), 8,
            dye(EnumDyeColor.BROWN), blockItem("scroll_paper_lantern"),
            blockItem("scroll_paper_lantern"), blockItem("scroll_paper_lantern"),
            blockItem("scroll_paper_lantern"), blockItem("scroll_paper_lantern"),
            blockItem("scroll_paper_lantern"), blockItem("scroll_paper_lantern"),
            blockItem("scroll_paper_lantern"));

        // Pigments.  1.12.2 stores all vanilla dyes in minecraft:dye with a
        // metadata value, so each modern dye item is represented by a stack.
        String[] dyes = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
            "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
        };
        EnumDyeColor[] dyeValues = {
            EnumDyeColor.WHITE, EnumDyeColor.ORANGE, EnumDyeColor.MAGENTA,
            EnumDyeColor.LIGHT_BLUE, EnumDyeColor.YELLOW, EnumDyeColor.LIME,
            EnumDyeColor.PINK, EnumDyeColor.GRAY, EnumDyeColor.SILVER,
            EnumDyeColor.CYAN, EnumDyeColor.PURPLE, EnumDyeColor.BLUE,
            EnumDyeColor.BROWN, EnumDyeColor.GREEN, EnumDyeColor.RED, EnumDyeColor.BLACK
        };
        for (int i = 0; i < dyes.length; i++) {
            shaped(event, "dye_colorizer_" + dyes[i],
                hexItem("dye_colorizer_" + dyes[i]),
                new String[] {" D ", "DCD", " D "},
                'C', dye(dyeValues[i]), 'D', dust);
        }
        shaped(event, "default_colorizer", hexItem("default_colorizer"),
            new String[] {" C ", "CIC", " C "}, 'C', dust, 'I', shard);
        shaped(event, "ancient_colorizer", hexItem("ancient_colorizer"),
            new String[] {" C ", "CIC", " C "}, 'C', dust, 'I', copper);
        shaped(event, "uuid_colorizer", hexItem("uuid_colorizer"),
            new String[] {"DCD", "CIC", "DCD"}, 'C', dust, 'D', dust, 'I', shard);

        Object[] prideMaterials = {
            Blocks.GLASS, Items.WHEAT_SEEDS, Items.ARROW, Items.BREAD,
            Items.WHEAT, rawIron, rawCopper, new ItemStack(Blocks.STONEBRICK),
            Items.WATER_BUCKET, Items.GLASS_BOTTLE,
            externalItem("cavesnotcliffs", "azalea"),
            externalItem("cavesnotcliffs", "honeycomb"),
            externalItem("cavesnotcliffs", "moss_block"),
            new ItemStack(Blocks.UNPOWERED_REPEATER), Items.EGG
        };
        String[] prideNames = {
            "agender", "aroace", "aromantic", "asexual", "bisexual", "demiboy",
            "demigirl", "gay", "genderfluid", "genderqueer", "intersex", "lesbian",
            "nonbinary", "pansexual", "plural", "transgender"
        };
        for (int i = 0; i < prideNames.length; i++) {
            Object material = prideMaterials[i];
            // Pansexual pigment's modern conditional ingredient defaults to a
            // carrot when Farmers' Delight is absent.  That is the only part
            // retained here; the optional Farmers' Delight cutting integration
            // is intentionally not part of this porting pass.
            if ("pansexual".equals(prideNames[i])) {
                material = Items.CARROT;
            }
            shaped(event, "pride_colorizer_" + prideNames[i],
                hexItem("pride_colorizer_" + prideNames[i]),
                new String[] {" D ", "DCD", " D "}, 'C', material, 'D', dust);
        }

        // Slate, amethyst, and quenched-allay decorative sets.
        shaped(event, "slate", blockItem("slate"), 6,
            new String[] {" A ", "SSS"}, 'A', dust, 'S', deepslate);
        shaped(event, "slate_block", blockItem("slate_block"), 8,
            new String[] {"DCD", "CIC", "DCD"},
            'C', deepslate, 'D', deepslate, 'I', dust);
        shaped(event, "slate_block_from_slates", blockItem("slate_block"),
            new String[] {"S", "S"}, 'S', blockItem("slate"));
        stoneSet(event, "slate", "slate_block", "slate_bricks", "slate_bricks_small",
            "slate_tiles", "slate_pillar");
        stoneSet(event, "amethyst_base", amethystBlock, "amethyst_bricks",
            "amethyst_bricks_small", "amethyst_tiles", "amethyst_pillar");
        stoneSet(event, "quenched_base", blockItem("quenched_allay"),
            "quenched_allay_bricks", "quenched_allay_bricks_small",
            "quenched_allay_tiles", null);

        shapelessCount(event, "slate_amethyst_bricks", blockItem("slate_amethyst_bricks"), 2,
            blockItem("slate_bricks"), blockItem("amethyst_bricks"));
        shapelessCount(event, "slate_amethyst_bricks_small", blockItem("slate_amethyst_bricks_small"), 2,
            blockItem("slate_bricks_small"), blockItem("amethyst_bricks_small"));
        shapelessCount(event, "slate_amethyst_tiles", blockItem("slate_amethyst_tiles"), 2,
            blockItem("slate_tiles"), blockItem("amethyst_tiles"));
        shapelessCount(event, "slate_amethyst_pillar", blockItem("slate_amethyst_pillar"), 2,
            blockItem("slate_pillar"), blockItem("amethyst_pillar"));
        shaped(event, "amethyst_dust_packing", blockItem("amethyst_dust_block"),
            new String[] {"XX", "XX"}, 'X', dust);
        shapelessCount(event, "amethyst_dust_unpacking", dust, 4,
            blockItem("amethyst_dust_block"));
        shaped(event, "amethyst_sconce", blockItem("amethyst_sconce"), 4,
            new String[] {"T", "B"}, 'B', copper, 'T', charged);

        // Edified wood and the Akashic/redstone greatworks.
        List<ItemStack> edifiedLogs = hexBlocks(
            "edified_log", "edified_log_amethyst", "edified_log_aventurine",
            "edified_log_citrine", "edified_log_purple", "stripped_edified_log",
            "edified_wood", "stripped_edified_wood");
        List<ItemStack> edifiedPlanks = Collections.singletonList(blockItem("edified_planks"));
        shapelessCount(event, "edified_planks", blockItem("edified_planks"), 4, edifiedLogs);
        shaped(event, "edified_wood", blockItem("edified_wood"), 3,
            new String[] {"WW", "WW"}, 'W', blockItem("edified_log"));
        shaped(event, "stripped_edified_wood", blockItem("stripped_edified_wood"), 3,
            new String[] {"WW", "WW"}, 'W', blockItem("stripped_edified_log"));
        shaped(event, "edified_panel", blockItem("edified_panel"), 9,
            new String[] {"WWW", "WWW", "WWW"}, 'W', edifiedPlanks);
        shaped(event, "edified_tile", blockItem("edified_tile"), 6,
            new String[] {"WW ", "W W", " WW"}, 'W', edifiedPlanks);
        shaped(event, "edified_door", blockItem("edified_door"), 3,
            new String[] {"WW", "WW", "WW"}, 'W', edifiedPlanks);
        shaped(event, "edified_trapdoor", blockItem("edified_trapdoor"), 2,
            new String[] {"WWW", "WWW"}, 'W', edifiedPlanks);
        shaped(event, "edified_stairs", blockItem("edified_stairs"), 4,
            new String[] {"W  ", "WW ", "WWW"}, 'W', edifiedPlanks);
        shaped(event, "edified_fence", blockItem("edified_fence"), 3,
            new String[] {"WSW", "WSW"}, 'S', Items.STICK, 'W', edifiedPlanks);
        shaped(event, "edified_fence_gate", blockItem("edified_fence_gate"),
            new String[] {"SWS", "SWS"}, 'S', Items.STICK, 'W', edifiedPlanks);
        shaped(event, "edified_slab", blockItem("edified_slab"), 6,
            new String[] {"WWW"}, 'W', edifiedPlanks);
        shaped(event, "edified_pressure_plate", blockItem("edified_pressure_plate"),
            new String[] {"WW"}, 'W', edifiedPlanks);
        shapeless(event, "edified_button", blockItem("edified_button"), edifiedPlanks);
        shaped(event, "akashic_bookshelf", blockItem("akashic_bookshelf"),
            new String[] {"LPL", "CCC", "LPL"},
            'C', Items.BOOK, 'L', edifiedLogs, 'P', edifiedPlanks);
        shaped(event, "akashic_connector", blockItem("akashic_connector"), 4,
            new String[] {"LPL", "123", "LPL"},
            '1', dust, '2', shard, '3', charged, 'L', edifiedLogs, 'P', edifiedPlanks);
        shaped(event, "impetus/empty", blockItem("impetus/empty"),
            new String[] {"PSS", "BAB", "SSP"},
            'A', charged, 'B', Blocks.IRON_BARS, 'P', Blocks.PURPUR_BLOCK,
            'S', blockItem("slate_block"));
        shaped(event, "directrix/empty", blockItem("directrix/empty"),
            new String[] {"CSS", "OAO", "SSC"},
            'A', charged, 'C', Blocks.UNPOWERED_COMPARATOR, 'O', Blocks.OBSERVER,
            'S', blockItem("slate_block"));

        // The special greatwork recipes are exposed by the brainsweep table;
        // these entries are the two craftable base components used by it.
        List<ItemStack> circleComponents = hexBlocks(
            "impetus/look", "impetus/rightclick", "impetus/redstone",
            "directrix/redstone", "directrix/boolean");
        registerStaffRecipes(event, charged, quenchedShard, circleComponents);

        // Decomposing one quenched shard is deliberately kept as three
        // separate ids because Patchouli links to each result independently.
        shapelessCount(event, "decompose_quenched_shard/dust", dust, 31,
            quenchedShard, dust);
        shapelessCount(event, "decompose_quenched_shard/charged", charged, 4,
            quenchedShard, charged);
        shapelessCount(event, "decompose_quenched_shard/shard", amethystShard, 7,
            quenchedShard, shard);
    }

    private static void registerStaffRecipes(RegistryEvent.Register<IRecipe> event,
                                             ItemStack charged,
                                             ItemStack quenchedShard,
                                             List<ItemStack> circleComponents) {
        String[] variants = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};
        for (String variant : variants) {
            shaped(event, "staff/" + variant, hexItem("staff/" + variant),
                new String[] {" SA", " WS", "S  "},
                'A', charged, 'S', Items.STICK, 'W', vanillaPlanks(variant));
        }
        shaped(event, "staff/edified", hexItem("staff/edified"),
            new String[] {" SA", " WS", "S  "},
            'A', charged, 'S', Items.STICK, 'W', blockItem("edified_planks"));
        shaped(event, "staff/quenched", hexItem("staff/quenched"),
            new String[] {" SA", " WS", "S  "},
            'A', charged, 'S', Items.STICK, 'W', quenchedShard);
        shaped(event, "staff/mindsplice", hexItem("staff/mindsplice"),
            new String[] {" SA", " WS", "S  "},
            'A', charged, 'S', Items.STICK, 'W', circleComponents);
    }

    private static Object vanillaPlanks(String variant) {
        if ("oak".equals(variant)) {
            return new ItemStack(Blocks.PLANKS, 1, 0);
        }
        if ("spruce".equals(variant)) {
            return new ItemStack(Blocks.PLANKS, 1, 1);
        }
        if ("birch".equals(variant)) {
            return new ItemStack(Blocks.PLANKS, 1, 2);
        }
        if ("jungle".equals(variant)) {
            return new ItemStack(Blocks.PLANKS, 1, 3);
        }
        if ("acacia".equals(variant)) {
            return new ItemStack(Blocks.PLANKS, 1, 4);
        }
        return new ItemStack(Blocks.PLANKS, 1, 5);
    }

    private static void stoneSet(RegistryEvent.Register<IRecipe> event,
                                 String baseName,
                                 Object base,
                                 String bricksName,
                                 String smallBricksName,
                                 String tilesName,
                                 String pillarName) {
        shaped(event, bricksName, blockItem(bricksName), 4,
            new String[] {"##", "##"}, '#', base);
        shapeless(event, bricksName + "_from_" + smallBricksName,
            blockItem(bricksName), blockItem(smallBricksName));
        shapeless(event, smallBricksName + "_from_" + bricksName,
            blockItem(smallBricksName), blockItem(bricksName));
        shaped(event, tilesName, blockItem(tilesName), 4,
            new String[] {"##", "##"}, '#', blockItem(bricksName));
        if (pillarName != null) {
            shaped(event, pillarName, blockItem(pillarName), 2,
                new String[] {"#", "#"}, '#', base);
        }
    }

    private static ItemStack selectedAmethystShard() {
        ResourceLocation id = AmethystCompat.shardId();
        if (id == null) {
            id = AmethystCompat.legacyShardId();
        }
        return item(id);
    }

    private static ItemStack selectedAmethystBlock() {
        ResourceLocation id = AmethystCompat.blockId();
        return id == null ? blockItem("amethyst_dust_block") : item(id);
    }

    private static Object ore(String name) {
        return oreOr(name, ItemStack.EMPTY);
    }

    private static Object oreOr(String name, ItemStack fallback) {
        if (!OreDictionary.getOres(name).isEmpty()) {
            return name;
        }
        return fallback == null ? ItemStack.EMPTY : fallback;
    }

    private static ItemStack dye(EnumDyeColor color) {
        return new ItemStack(Items.DYE, 1, color.getDyeDamage());
    }

    private static List<ItemStack> musicDiscs() {
        return Arrays.asList(
            new ItemStack(Items.RECORD_13), new ItemStack(Items.RECORD_CAT),
            new ItemStack(Items.RECORD_BLOCKS), new ItemStack(Items.RECORD_CHIRP),
            new ItemStack(Items.RECORD_FAR), new ItemStack(Items.RECORD_MALL),
            new ItemStack(Items.RECORD_MELLOHI), new ItemStack(Items.RECORD_STAL),
            new ItemStack(Items.RECORD_STRAD), new ItemStack(Items.RECORD_WARD),
            new ItemStack(Items.RECORD_11), new ItemStack(Items.RECORD_WAIT));
    }

    private static Object externalItem(String modId, String path) {
        return item(new ResourceLocation(modId, path));
    }

    private static List<ItemStack> hexBlocks(String... ids) {
        List<ItemStack> stacks = new ArrayList<>();
        for (String id : ids) {
            ItemStack stack = blockItem(id);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    private static ItemStack hexItem(String path) {
        return item(new ResourceLocation(HexAPI.MOD_ID, path));
    }

    private static ItemStack blockItem(String path) {
        Item item = HexBlocks.getBlockItem(path);
        if (item != null) {
            return new ItemStack(item);
        }
        return hexItem(path);
    }

    private static ItemStack item(ResourceLocation id) {
        return item(id, 1);
    }

    private static ItemStack item(ResourceLocation id, int count) {
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static void shaped(RegistryEvent.Register<IRecipe> event,
                               String id,
                               ItemStack output,
                               String[] pattern,
                               Object... keyValues) {
        shaped(event, id, output, 1, pattern, keyValues);
    }

    private static void shaped(RegistryEvent.Register<IRecipe> event,
                               String id,
                               ItemStack output,
                               int count,
                               String[] pattern,
                               Object... keyValues) {
        if (output == null || output.isEmpty() || pattern == null
            || keyValues.length % 2 != 0) {
            return;
        }
        for (int i = 1; i < keyValues.length; i += 2) {
            if (!validIngredient(keyValues[i])) {
                return;
            }
        }
        ItemStack result = output.copy();
        result.setCount(count);
        Object[] recipe = new Object[pattern.length + keyValues.length];
        System.arraycopy(pattern, 0, recipe, 0, pattern.length);
        System.arraycopy(keyValues, 0, recipe, pattern.length, keyValues.length);
        ShapedOreRecipe shaped = new ShapedOreRecipe(
            new ResourceLocation(HexAPI.MOD_ID, id), result, recipe);
        register(event, shaped, id);
    }

    private static void shapeless(RegistryEvent.Register<IRecipe> event,
                                  String id,
                                  ItemStack output,
                                  Object... ingredients) {
        shapelessCount(event, id, output, 1, ingredients);
    }

    private static void shapelessCount(RegistryEvent.Register<IRecipe> event,
                                       String id,
                                       ItemStack output,
                                       int count,
                                       Object... ingredients) {
        if (output == null || output.isEmpty() || !validIngredients(ingredients)) {
            return;
        }
        ItemStack result = output.copy();
        result.setCount(count);
        ShapelessOreRecipe shapeless = new ShapelessOreRecipe(
            new ResourceLocation(HexAPI.MOD_ID, id), result, ingredients);
        register(event, shapeless, id);
    }

    private static void register(RegistryEvent.Register<IRecipe> event,
                                 IRecipe recipe,
                                 String id) {
        recipe.setRegistryName(new ResourceLocation(HexAPI.MOD_ID, id));
        event.getRegistry().register(recipe);
    }

    private static boolean validIngredients(Object[] ingredients) {
        for (Object ingredient : ingredients) {
            if (!validIngredient(ingredient)) {
                return false;
            }
        }
        return true;
    }

    private static boolean validIngredient(Object ingredient) {
        if (ingredient == null) {
            return false;
        }
        if (ingredient instanceof ItemStack) {
            return !((ItemStack) ingredient).isEmpty();
        }
        if (ingredient instanceof String) {
            return !OreDictionary.getOres((String) ingredient).isEmpty();
        }
        if (ingredient instanceof Collection) {
            for (Object value : (Collection<?>) ingredient) {
                if (validIngredient(value)) {
                    return true;
                }
            }
            return false;
        }
        return ingredient instanceof Item || ingredient instanceof net.minecraft.block.Block;
    }
}
