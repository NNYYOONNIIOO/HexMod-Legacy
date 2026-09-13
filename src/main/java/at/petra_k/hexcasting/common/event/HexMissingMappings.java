package at.petra_k.hexcasting.common.event;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import at.petra_k.hexcasting.common.lib.HexItems;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Keeps worlds created before the duplicate Hex Casting guide item was removed
 * loadable. Forge reports the old registry id as a missing mapping; remapping
 * it to Patchouli's shared guide item also preserves the book NBT.
 */
@Mod.EventBusSubscriber(modid = "hexcasting")
public final class HexMissingMappings {
    private static final ResourceLocation OLD_GUIDE_BOOK =
        new ResourceLocation("hexcasting", "guide_book");
    private static final ResourceLocation PATCHOULI_GUIDE_BOOK =
        new ResourceLocation("patchouli", "guide_book");

    private HexMissingMappings() {
    }

    @SubscribeEvent
    public static void onMissingItemMappings(RegistryEvent.MissingMappings<Item> event) {
        for (RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getAllMappings()) {
            if (new ResourceLocation("hexcasting", "scrying_lens").equals(mapping.key)) {
                mapping.remap(HexItems.SCRYING_LENS);
                continue;
            }
            if (!OLD_GUIDE_BOOK.equals(mapping.key)) {
                continue;
            }

            Item replacement = Item.REGISTRY.getObject(PATCHOULI_GUIDE_BOOK);
            if (replacement != null) {
                mapping.remap(replacement);
            } else {
                // Patchouli is a required runtime dependency for the guide.
                mapping.ignore();
            }
        }
    }
}
