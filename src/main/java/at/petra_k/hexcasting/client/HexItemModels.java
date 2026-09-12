package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.common.lib.HexItems;
import at.petra_k.hexcasting.common.item.ItemColorizer;
import at.petra_k.hexcasting.common.item.ItemPackagedSpell;
import at.petra_k.hexcasting.common.lib.HexBlocks;
import net.minecraft.item.Item;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemStack;
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
        ModelLoader.setCustomModelResourceLocation(
            HexItems.PATTERN_SCROLL,
            0,
            new ModelResourceLocation(HexItems.PATTERN_SCROLL.getRegistryName(), "inventory")
        );
        ModelLoader.setCustomModelResourceLocation(
            PatchouliItems.book,
            0,
            new ModelResourceLocation(new ResourceLocation(HexAPI.MOD_ID, "patchouli_book"), "inventory")
        );

        for (Item item : HexItems.allItems()) {
            if (item.getRegistryName() != null) {
                ModelLoader.setCustomModelResourceLocation(
                    item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
            }
        }
        Minecraft.getMinecraft().getItemColors().registerItemColorHandler(
            (stack, tintIndex) -> tintIndex == 0 ? colorFor(stack) : -1,
            HexItems.FOCUS, HexItems.STAFF);
        for (Item item : HexBlocks.blockItems()) {
            if (item.getRegistryName() != null) {
                ModelLoader.setCustomModelResourceLocation(
                    item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
            }
        }
    }

    private static void registerPackagedSpellProperties() {
        IItemPropertyGetter filled =
            (stack, world, entity) -> ItemPackagedSpell.getPackagedAction(stack) == null ? 0.0F : 1.0F;
        HexItems.CYPHER.addPropertyOverride(new ResourceLocation(HexAPI.MOD_ID, "filled"), filled);
        HexItems.TRINKET.addPropertyOverride(new ResourceLocation(HexAPI.MOD_ID, "filled"), filled);
        HexItems.ARTIFACT.addPropertyOverride(new ResourceLocation(HexAPI.MOD_ID, "filled"), filled);
        Item ancient = HexItems.EXTRA_ITEMS.get("ancient_cypher");
        if (ancient != null) {
            ancient.addPropertyOverride(new ResourceLocation(HexAPI.MOD_ID, "filled"), filled);
        }
    }

    private static int colorFor(ItemStack stack) {
        int color = ItemColorizer.getColor(stack);
        return color < 0 ? 0xFFFFFF : color;
    }
}
