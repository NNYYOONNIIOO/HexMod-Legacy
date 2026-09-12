package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.common.lib.HexItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import net.minecraft.item.ItemStack;

/**
 * JEI 4.x integration for the 1.12.2 port. Hex items act as recipe
 * catalysts, matching the role of their modern counterparts without
 * introducing a custom recipe category for vanilla crafting recipes.
 */
@JEIPlugin
public final class HexJeiPlugin implements IModPlugin {
    private static final String CRAFTING_UID = "minecraft.crafting";

    @Override
    public void register(IModRegistry registry) {
        registry.addRecipeCatalyst(new ItemStack(HexItems.FOCUS), CRAFTING_UID);
        registry.addRecipeCatalyst(new ItemStack(HexItems.STAFF), CRAFTING_UID);
        registry.addRecipeCatalyst(new ItemStack(HexItems.SCRYING_LENS), CRAFTING_UID);
        registry.addRecipeCatalyst(new ItemStack(HexItems.BATTERY), CRAFTING_UID);
        registry.addRecipeCatalyst(new ItemStack(HexItems.PATTERN_SCROLL), CRAFTING_UID);
    }
}

