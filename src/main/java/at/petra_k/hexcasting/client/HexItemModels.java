package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.common.lib.HexItems;
import at.petra_k.hexcasting.common.item.ItemColorizer;
import at.petra_k.hexcasting.common.item.ItemHexFocus;
import at.petra_k.hexcasting.common.item.ItemPackagedSpell;
import at.petra_k.hexcasting.common.item.ItemPatternScroll;
import at.petra_k.hexcasting.common.item.ItemSpellbook;
import at.petra_k.hexcasting.common.lib.HexBlocks;
import net.minecraft.item.Item;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.util.ResourceLocation;
import vazkii.patchouli.common.item.PatchouliItems;

/** Client-only 1.12.2 item model registration. */
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID, value = Side.CLIENT)
public final class HexItemModels {
    private HexItemModels() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerPackagedSpellProperties();
        registerFocusProperties();
        registerSpellbookProperties();
        registerLegacyResourceProperties();
        ModelLoader.setCustomModelResourceLocation(
            HexItems.FOCUS,
            0,
            new ModelResourceLocation(HexItems.FOCUS.getRegistryName(), "inventory")
        );
        ModelLoader.setCustomModelResourceLocation(
            HexItems.STAFF,
            0,
            new ModelResourceLocation(HexItems.STAFF.getRegistryName(), "inventory")
        );
        ModelLoader.setCustomModelResourceLocation(
            HexItems.SCRYING_LENS,
            0,
            new ModelResourceLocation(HexItems.SCRYING_LENS.getRegistryName(), "inventory")
        );
        ModelLoader.setCustomModelResourceLocation(
            HexItems.BATTERY,
            0,
            new ModelResourceLocation(HexItems.BATTERY.getRegistryName(), "inventory")
        );
        if (PatchouliItems.book != null) {
            ModelLoader.setCustomModelResourceLocation(
                PatchouliItems.book,
                0,
                new ModelResourceLocation(new ResourceLocation(HexAPI.MOD_ID, "patchouli_book"), "inventory")
            );
        }

        for (Item item : HexItems.allItems()) {
            if (item.getRegistryName() != null) {
                ModelLoader.setCustomModelResourceLocation(
                    item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
            }
        }
        for (Item item : HexItems.EXTRA_ITEMS.values()) {
            if (item instanceof ItemPatternScroll && item.getRegistryName() != null) {
                ModelLoader.setCustomModelResourceLocation(
                    item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
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

    private static void registerPackagedSpellProperties() {
        IItemPropertyGetter filled =
            (stack, world, entity) -> ItemPackagedSpell.getPackagedAction(stack) == null ? 0.0F : 1.0F;
        IItemPropertyGetter variant =
            (stack, world, entity) -> ItemPackagedSpell.getVariant(stack) / 7.0F;
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
            ItemHexFocus.getVariant(stack) / 7.0F);
    }


    private static void registerSpellbookProperties() {
        Item spellbook = HexItems.EXTRA_ITEMS.get("spellbook");
        if (spellbook == null) {
            return;
        }
        spellbook.addPropertyOverride(new ResourceLocation(HexAPI.MOD_ID, "filled"),
            (stack, world, entity) -> ItemSpellbook.getPattern(stack) != null ? 1.0F : 0.0F);
        spellbook.addPropertyOverride(new ResourceLocation(HexAPI.MOD_ID, "sealed"),
            (stack, world, entity) -> ItemSpellbook.isSealed(stack) ? 1.0F : 0.0F);
        spellbook.addPropertyOverride(new ResourceLocation(HexAPI.MOD_ID, "variant"),
            (stack, world, entity) -> spellbookVariant(stack));
    }

    private static boolean spellbookHasPayload(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag == null) {
            return false;
        }
        return tag.hasKey("pattern", 10) || tag.hasKey("patterns", 9)
            || tag.hasKey("iota", 10) || tag.hasKey("selected_action", 8)
            || tag.hasKey("action", 8);
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
        return Math.max(0, Math.min(7, tag.getInteger("variant"))) / 7.0F;
    }
    private static int colorFor(ItemStack stack) {
        int color = ItemColorizer.getColor(stack);
        return color < 0 ? 0xFFFFFF : color;
    }

    private static void registerLegacyResourceProperties() {
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
        registerNbtProperty(HexItems.EXTRA_ITEMS.get("quenched_allay"), "variant",
            (stack, world, entity) -> nbtNumberProperty(stack, "variant"));
        registerNbtProperty(HexItems.EXTRA_ITEMS.get("quenched_allay_bricks"), "variant",
            (stack, world, entity) -> nbtNumberProperty(stack, "variant"));
        registerNbtProperty(HexItems.EXTRA_ITEMS.get("quenched_allay_bricks_small"), "variant",
            (stack, world, entity) -> nbtNumberProperty(stack, "variant"));
        registerNbtProperty(HexItems.EXTRA_ITEMS.get("quenched_allay_shard"), "variant",
            (stack, world, entity) -> nbtNumberProperty(stack, "variant"));
        registerNbtProperty(HexItems.EXTRA_ITEMS.get("quenched_allay_tiles"), "variant",
            (stack, world, entity) -> nbtNumberProperty(stack, "variant"));
        registerNbtProperty(HexItems.EXTRA_ITEMS.get("scroll"), "ancient",
            (stack, world, entity) -> nbtBooleanProperty(stack, "ancient"));
        registerNbtProperty(HexItems.EXTRA_ITEMS.get("scroll_medium"), "ancient",
            (stack, world, entity) -> nbtBooleanProperty(stack, "ancient"));
        registerNbtProperty(HexItems.EXTRA_ITEMS.get("scroll_small"), "ancient",
            (stack, world, entity) -> nbtBooleanProperty(stack, "ancient"));
        registerNbtProperty(HexItems.EXTRA_ITEMS.get("slate"), "written",
            (stack, world, entity) -> nbtBooleanProperty(stack, "written"));
        registerNbtProperty(HexItems.EXTRA_ITEMS.get("staff/quenched"), "variant",
            (stack, world, entity) -> nbtNumberProperty(stack, "variant"));
        registerNbtProperty(HexItems.EXTRA_ITEMS.get("thought_knot"), "written",
            (stack, world, entity) -> nbtBooleanProperty(stack, "written"));
        registerNbtProperty(HexItems.TRINKET, "has_patterns",
            (stack, world, entity) -> nbtNumberProperty(stack, "has_patterns"));
        registerNbtProperty(HexItems.TRINKET, "variant",
            (stack, world, entity) -> nbtNumberProperty(stack, "variant"));
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
