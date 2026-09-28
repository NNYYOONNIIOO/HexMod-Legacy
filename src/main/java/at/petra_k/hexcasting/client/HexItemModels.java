package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.common.lib.HexItems;
import at.petra_k.hexcasting.common.misc.AmethystCompat;
import at.petra_k.hexcasting.common.config.HexConfig;
import at.petra_k.hexcasting.common.item.ItemColorizer;
import at.petra_k.hexcasting.common.item.ItemHexFocus;
import at.petra_k.hexcasting.common.item.ItemPackagedSpell;
import at.petra_k.hexcasting.common.item.ItemSpellbook;
import at.petra_k.hexcasting.common.lib.HexBlocks;
import net.minecraft.item.Item;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.util.ResourceLocation;
import vazkii.patchouli.common.item.PatchouliItems;
import com.google.common.collect.ImmutableMap;

/** Client-only 1.12.2 item model registration. */
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID, value = Side.CLIENT)
public final class HexItemModels {
    private HexItemModels() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        HexConfig.applyClientLanguageDefault(HexClientProxy.currentLanguage());
        registerPackagedSpellProperties();
        registerFocusProperties();
        registerSpellbookProperties();
        registerGaslightingProperties();
        registerGaslightingModels();
        registerLegacyResourceProperties();
        if (PatchouliItems.book != null) {
            ModelLoader.setCustomModelResourceLocation(
                PatchouliItems.book,
                0,
                new ModelResourceLocation(new ResourceLocation(HexAPI.MOD_ID, "patchouli_book"), "inventory")
            );
        }

        for (Item item : HexItems.allItems()) {
            if (item.getRegistryName() != null) {
                ResourceLocation modelId = item.getRegistryName();
                if (HexConfig.hidePrideColors()
                    && item instanceof ItemColorizer
                    && HexClientPigment.isPrideVariant(
                        ((ItemColorizer) item).getVariant())) {
                    Item defaultColorizer = HexItems.EXTRA_ITEMS.get("default_colorizer");
                    if (defaultColorizer != null
                        && defaultColorizer.getRegistryName() != null) {
                        modelId = defaultColorizer.getRegistryName();
                    }
                }
                ModelLoader.setCustomModelResourceLocation(
                    item, 0, new ModelResourceLocation(modelId, "inventory"));
            }
        }
        net.minecraft.client.renderer.color.ItemColors itemColors =
            Minecraft.getMinecraft().getItemColors();
        if (itemColors != null) {
            itemColors.registerItemColorHandler(
                (stack, tintIndex) -> tintIndex == 0 ? colorFor(stack) : -1,
                HexItems.FOCUS, HexItems.STAFF);
        }
        for (Item item : HexBlocks.blockItems()) {
            if (item != null && item.getRegistryName() != null) {
                ModelLoader.setCustomModelResourceLocation(
                    item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
            }
        }
    }

    /** Register Caves Not Cliffs' legacy 1.12 texture paths in the atlas. */
    @SubscribeEvent
    public static void registerAmethystTextures(TextureStitchEvent.Pre event) {
        if (event == null || event.getMap() == null
            || !AmethystCompat.CAVES_NOT_CLIFFS.equals(AmethystCompat.provider())) {
            return;
        }
        event.getMap().registerSprite(AmethystCompat.sconceCopperTexture());
        event.getMap().registerSprite(AmethystCompat.sconceAmethystTexture());
        event.getMap().registerSprite(AmethystCompat.shardTexture());
    }

    /**
     * Keep models that were authored against Farmer's Future Delight usable
     * when Caves Not Cliffs is the selected provider.  The blockstate event
     * has already expanded all sconce facing variants at this point, so each
     * variant is re-baked with its original transform intact.
     */
    @SubscribeEvent
    public static void retextureAmethystModels(ModelBakeEvent event) {
        if (event == null
            || !AmethystCompat.CAVES_NOT_CLIFFS.equals(AmethystCompat.provider())) {
            return;
        }

        for (ModelResourceLocation location : event.getModelRegistry().getKeys()) {
            if (!HexAPI.MOD_ID.equals(location.getResourceDomain())) {
                continue;
            }

            ImmutableMap<String, String> textures = texturesFor(location);
            if (textures.isEmpty()) {
                continue;
            }

            try {
                IModel model = ModelLoaderRegistry.getModel(location);
                IModel retextured = model.retexture(textures);
                boolean inventory = "inventory".equals(location.getVariant());
                IBakedModel baked = retextured.bake(
                    retextured.getDefaultState(),
                    inventory ? DefaultVertexFormats.ITEM : DefaultVertexFormats.BLOCK,
                    ModelLoader.defaultTextureGetter());
                event.getModelRegistry().putObject(location, baked);
            } catch (Exception ignored) {
                // The original model remains in the registry if a third-party
                // loader rejects retexturing; this must not prevent startup.
            }
        }
    }

    private static ImmutableMap<String, String> texturesFor(ModelResourceLocation location) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        String path = location.getResourcePath();
        if ("amethyst_sconce".equals(path)) {
            builder.put("0", AmethystCompat.sconceCopperTexture().toString());
            builder.put("3", AmethystCompat.sconceAmethystTexture().toString());
            builder.put("particle", AmethystCompat.sconceCopperTexture().toString());
        } else if ("conjured_block".equals(path)
            || "conjured_light".equals(path)) {
            builder.put("layer0", AmethystCompat.shardTexture().toString());
        }
        return builder.build();
    }

    private static void registerPackagedSpellProperties() {
        IItemPropertyGetter filled =
            (stack, world, entity) -> ItemPackagedSpell.getPackagedAction(stack) == null ? 0.0F : 1.0F;
        IItemPropertyGetter variant =
            (stack, world, entity) -> ItemPackagedSpell.getVariant(stack)
                / (ItemPackagedSpell.VARIANT_COUNT - 1.0F);
        HexItems.CYPHER.addPropertyOverride(new ResourceLocation(HexAPI.MOD_ID, "filled"), filled);
        HexItems.TRINKET.addPropertyOverride(new ResourceLocation(HexAPI.MOD_ID, "filled"), filled);
        HexItems.ARTIFACT.addPropertyOverride(new ResourceLocation(HexAPI.MOD_ID, "filled"), filled);
        HexItems.CYPHER.addPropertyOverride(new ResourceLocation(HexAPI.MOD_ID, "variant"), variant);
        HexItems.TRINKET.addPropertyOverride(new ResourceLocation(HexAPI.MOD_ID, "variant"), variant);
        HexItems.ARTIFACT.addPropertyOverride(new ResourceLocation(HexAPI.MOD_ID, "variant"), variant);
        Item ancient = HexItems.EXTRA_ITEMS.get("ancient_cypher");
        if (ancient != null) {
            ancient.addPropertyOverride(new ResourceLocation(HexAPI.MOD_ID, "filled"), filled);
            ancient.addPropertyOverride(new ResourceLocation(HexAPI.MOD_ID, "variant"), variant);
        }
    }

    private static void registerFocusProperties() {
        ResourceLocation filledId = new ResourceLocation(HexAPI.MOD_ID, "filled");
        ResourceLocation sealedId = new ResourceLocation(HexAPI.MOD_ID, "sealed");
        ResourceLocation variantId = new ResourceLocation(HexAPI.MOD_ID, "variant");
        HexItems.FOCUS.addPropertyOverride(filledId, (stack, world, entity) ->
            !ItemHexFocus.isSealed(stack) && HexItems.FOCUS.readIotaTag(stack) != null ? 1.0F : 0.0F);
        HexItems.FOCUS.addPropertyOverride(sealedId, (stack, world, entity) ->
            ItemHexFocus.isSealed(stack) ? 1.0F : 0.0F);
        HexItems.FOCUS.addPropertyOverride(variantId, (stack, world, entity) ->
            ItemHexFocus.getVariant(stack) / (ItemHexFocus.VARIANT_COUNT - 1.0F));
    }


    private static void registerSpellbookProperties() {
        Item spellbook = HexItems.EXTRA_ITEMS.get("spellbook");
        if (spellbook == null) {
            return;
        }
        spellbook.addPropertyOverride(new ResourceLocation(HexAPI.MOD_ID, "filled"),
            (stack, world, entity) -> ItemSpellbook.hasIota(stack) ? 1.0F : 0.0F);
        spellbook.addPropertyOverride(new ResourceLocation(HexAPI.MOD_ID, "sealed"),
            (stack, world, entity) -> ItemSpellbook.isSealed(stack) ? 1.0F : 0.0F);
        spellbook.addPropertyOverride(new ResourceLocation(HexAPI.MOD_ID, "variant"),
            (stack, world, entity) -> spellbookVariant(stack));
    }

    private static void registerGaslightingProperties() {
        IItemPropertyGetter variant = (stack, world, entity) ->
            HexGaslightingTracker.getVariant();
        registerProperty(HexBlocks.getBlockItem("quenched_allay"), "variant", variant);
        registerProperty(HexBlocks.getBlockItem("quenched_allay_bricks"), "variant", variant);
        registerProperty(HexBlocks.getBlockItem("quenched_allay_bricks_small"), "variant", variant);
        registerProperty(HexBlocks.getBlockItem("quenched_allay_tiles"), "variant", variant);
        registerProperty(HexItems.EXTRA_ITEMS.get("quenched_allay_shard"), "variant", variant);
        registerProperty(HexItems.EXTRA_ITEMS.get("staff/quenched"), "variant", variant);
    }

    /**
     * The four gaslighting models are referenced by item overrides, but a
     * 1.12.2 model bake only loads models reachable from registered variants.
     * Register the block-model locations explicitly, matching the modern
     * port's extra-model hook, so the block entity renderer can use them too.
     */
    private static void registerGaslightingModels() {
        registerGaslightingModels("quenched_allay", false);
        registerGaslightingModels("quenched_allay_bricks", true);
        registerGaslightingModels("quenched_allay_bricks_small", true);
        registerGaslightingModels("quenched_allay_tiles", true);
    }

    private static void registerGaslightingModels(String blockId, boolean decorative) {
        Item item = HexBlocks.getBlockItem(blockId);
        if (item == null) {
            return;
        }
        String prefix = decorative ? "block/deco/" : "block/";
        ResourceLocation[] models = new ResourceLocation[4];
        for (int i = 0; i < models.length; i++) {
            models[i] = HexAPI.modLoc(prefix + blockId + "_" + i);
        }
        ModelBakery.registerItemVariants(item, models);
    }

    private static boolean spellbookHasPayload(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag == null) {
            return false;
        }
        return tag.hasKey("pattern", 10) || tag.hasKey("patterns", 9)
            || tag.hasKey("iota", 10) || tag.hasKey("selected_action", 8)
            || tag.hasKey("action", 8) || tag.hasKey(ItemSpellbook.TAG_PAGES, 10)
            || tag.hasKey(ItemSpellbook.TAG_SELECTED_PAGE, 3);
    }

    private static boolean spellbookIsSealed(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        return tag != null && tag.getBoolean("sealed");
    }

    private static float spellbookVariant(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag == null) {
            return 0.0F;
        }
        return ItemSpellbook.getVariant(stack) / (ItemSpellbook.VARIANT_COUNT - 1.0F);
    }
    private static int colorFor(ItemStack stack) {
        if (HexClientPigment.hides(ItemColorizer.getVariant(stack))) {
            return HexClientPigment.defaultColor(
                HexClientTickCounter.getTotal(), 0.0D, 0.0D, 0.0D);
        }
        int color = ItemColorizer.getColor(stack);
        return color < 0 ? 0xFFFFFF : color;
    }

    private static void registerLegacyResourceProperties() {
        registerNbtProperty(HexItems.FOCUS, "overlay_layer",
            (stack, world, entity) -> focusOverlayLayer(stack));
        registerNbtProperty(HexItems.EXTRA_ITEMS.get("spellbook"), "overlay_layer",
            (stack, world, entity) -> spellbookOverlayLayer(stack));
        registerNbtProperty(HexItems.EXTRA_ITEMS.get("ancient_cypher"), "has_patterns",
            (stack, world, entity) -> nbtNumberProperty(stack, "has_patterns"));
        registerNbtProperty(HexItems.EXTRA_ITEMS.get("ancient_cypher"), "variant",
            (stack, world, entity) -> nbtNumberProperty(stack, "variant"));
        registerNbtProperty(HexItems.ARTIFACT, "has_patterns",
            (stack, world, entity) -> nbtNumberProperty(stack, "has_patterns"));
        registerNbtProperty(HexItems.ARTIFACT, "variant",
            (stack, world, entity) -> nbtNumberProperty(stack, "variant"));
        registerNbtProperty(HexItems.BATTERY, "max_media",
            (stack, world, entity) -> batteryMaxMediaProperty(stack));
        registerNbtProperty(HexItems.BATTERY, "media",
            (stack, world, entity) -> batteryMediaProperty(stack));
        registerNbtProperty(HexItems.CYPHER, "has_patterns",
            (stack, world, entity) -> nbtNumberProperty(stack, "has_patterns"));
        registerNbtProperty(HexItems.CYPHER, "variant",
            (stack, world, entity) -> nbtNumberProperty(stack, "variant"));
        registerNbtProperty(HexItems.EXTRA_ITEMS.get("scroll"), "ancient",
            (stack, world, entity) -> nbtBooleanProperty(stack, "ancient"));
        registerNbtProperty(HexItems.EXTRA_ITEMS.get("scroll_medium"), "ancient",
            (stack, world, entity) -> nbtBooleanProperty(stack, "ancient"));
        registerNbtProperty(HexItems.EXTRA_ITEMS.get("scroll_small"), "ancient",
            (stack, world, entity) -> nbtBooleanProperty(stack, "ancient"));
        registerNbtProperty(HexBlocks.getBlockItem("slate"), "written",
            (stack, world, entity) -> nbtBooleanProperty(stack, "written"));
        registerNbtProperty(HexItems.EXTRA_ITEMS.get("thought_knot"), "written",
            (stack, world, entity) -> nbtBooleanProperty(stack, "written"));
        registerNbtProperty(HexItems.TRINKET, "has_patterns",
            (stack, world, entity) -> nbtNumberProperty(stack, "has_patterns"));
        registerNbtProperty(HexItems.TRINKET, "variant",
            (stack, world, entity) -> nbtNumberProperty(stack, "variant"));
    }

    private static void registerProperty(Item item, String key,
                                          IItemPropertyGetter getter) {
        if (item != null) {
            item.addPropertyOverride(new ResourceLocation(HexAPI.MOD_ID, key),
                getter);
        }
    }

    private static void registerNbtProperty(Item item, String key, IItemPropertyGetter getter) {
        if (item != null) {
            item.addPropertyOverride(new net.minecraft.util.ResourceLocation(HexAPI.MOD_ID, key), getter);
        }
    }

    private static String resolveNbtKey(ItemStack stack, String key) {
        if (stack == null || stack.getTagCompound() == null) return key;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag.hasKey(key, 99)) return key;
        String namespaced = HexAPI.MOD_ID + ":" + key;
        return tag.hasKey(namespaced, 99) ? namespaced : key;
    }

    private static float nbtNumberProperty(ItemStack stack, String key) {
        if (stack == null || stack.getTagCompound() == null) return 0.0F;
        NBTTagCompound tag = stack.getTagCompound();
        String actual = resolveNbtKey(stack, key);
        if (!tag.hasKey(actual, 99)) return 0.0F;
        switch (tag.getTagId(actual)) {
            case 1: return tag.getByte(actual);
            case 2: return tag.getShort(actual);
            case 3: return tag.getInteger(actual);
            case 4: return (float) tag.getLong(actual);
            case 5: return tag.getFloat(actual);
            case 6: return (float) tag.getDouble(actual);
            default: return 0.0F;
        }
    }

    private static float nbtBooleanProperty(ItemStack stack, String key) {
        if (stack == null || stack.getTagCompound() == null) return 0.0F;
        NBTTagCompound tag = stack.getTagCompound();
        String actual = resolveNbtKey(stack, key);
        if (tag.hasKey(actual, 1)) return tag.getBoolean(actual) ? 1.0F : 0.0F;
        return nbtNumberProperty(stack, key) == 0.0F ? 0.0F : 1.0F;
    }

    private static float focusOverlayLayer(ItemStack stack) {
        if (stack == null) return 0.0F;
        if (ItemHexFocus.isSealed(stack)) return 2.0F;
        return HexItems.FOCUS.readIotaTag(stack) != null ? 1.0F : 0.0F;
    }

    private static float spellbookOverlayLayer(ItemStack stack) {
        if (stack == null) return 0.0F;
        Item spellbook = HexItems.EXTRA_ITEMS.get("spellbook");
        if (spellbook == null || stack.getItem() != spellbook) return 0.0F;
        NBTTagCompound tag = stack.getTagCompound();
        if (ItemSpellbook.isSealed(stack)) return 2.0F;
        return ItemSpellbook.hasIota(stack) ? 1.0F : 0.0F;
    }
    private static float batteryMaxMediaProperty(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof at.petra_k.hexcasting.common.item.ItemMediaBattery)) return 0.0F;
        at.petra_k.hexcasting.common.item.ItemMediaBattery battery = (at.petra_k.hexcasting.common.item.ItemMediaBattery) stack.getItem();
        long max = battery.getMaxMedia(stack);
        long dust = at.petra_k.hexcasting.api.misc.MediaConstants.DUST_UNIT * 64L;
        long shard = at.petra_k.hexcasting.api.misc.MediaConstants.SHARD_UNIT * 64L;
        long crystal = at.petra_k.hexcasting.api.misc.MediaConstants.CRYSTAL_UNIT * 64L;
        long quenchedShard = at.petra_k.hexcasting.api.misc.MediaConstants.QUENCHED_SHARD_UNIT * 64L;
        if (max <= dust) return 0.0F;
        if (max <= shard) return 1.0F;
        if (max <= crystal) return 2.0F;
        if (max <= quenchedShard) return 3.0F;
        return 4.0F;
    }

    private static float batteryMediaProperty(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof at.petra_k.hexcasting.common.item.ItemMediaBattery)) return 0.0F;
        at.petra_k.hexcasting.common.item.ItemMediaBattery battery = (at.petra_k.hexcasting.common.item.ItemMediaBattery) stack.getItem();
        long max = battery.getMaxMedia(stack);
        if (max <= 0L) return 0.0F;
        return (float) Math.max(0.0D, Math.min(1.0D, (double) battery.getMedia(stack) / (double) max));
    }
}
