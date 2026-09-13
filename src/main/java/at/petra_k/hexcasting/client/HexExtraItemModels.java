package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.common.lib.HexItems;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Registers inventory models for the data-driven item registrations. */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID, value = Side.CLIENT)
public final class HexExtraItemModels {
    private HexExtraItemModels() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        for (Item item : HexItems.EXTRA_ITEMS.values()) {
            if (item.getRegistryName() == null) {
                continue;
            }
            ModelLoader.setCustomModelResourceLocation(
                item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
    }
}
