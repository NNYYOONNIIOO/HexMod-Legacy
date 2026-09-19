package at.petra_k.hexcasting.common.misc;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

/**
 * Selects the amethyst backport used by Hex Casting.
 *
 * <p>Farmer's Future Delight and Caves Not Cliffs expose the same content
 * under the same paths.  Caves Not Cliffs has priority when both are loaded,
 * so every integration point uses this class instead of checking the two
 * mods independently.</p>
 */
public final class AmethystCompat {
    public static final String CAVES_NOT_CLIFFS = "cavesnotcliffs";
    public static final String FARMERS_FUTURE_DELIGHT = "farmers_future_delight";
    private static final String DEPTHS_UPDATE = "depthsupdate";

    private AmethystCompat() {
    }

    /** Return the selected provider, with Caves Not Cliffs taking priority. */
    public static String provider() {
        if (Loader.isModLoaded(CAVES_NOT_CLIFFS)) {
            return CAVES_NOT_CLIFFS;
        }
        if (Loader.isModLoaded(FARMERS_FUTURE_DELIGHT)) {
            return FARMERS_FUTURE_DELIGHT;
        }
        return null;
    }

    public static boolean hasProvider() {
        return provider() != null;
    }

    public static ResourceLocation clusterId() {
        return contentId("amethyst_cluster");
    }

    public static ResourceLocation shardId() {
        return contentId("amethyst_shard");
    }

    public static ResourceLocation blockId() {
        return contentId("amethyst_block");
    }

    public static ResourceLocation buddingBlockId() {
        return contentId("budding_amethyst");
    }

    /**
     * Return the legacy fallback cluster when neither preferred provider is
     * installed.  This keeps the older Depths Update compatibility intact.
     */
    public static ResourceLocation legacyClusterId() {
        return new ResourceLocation(DEPTHS_UPDATE, "amethyst_cluster");
    }

    public static ResourceLocation legacyShardId() {
        return new ResourceLocation(DEPTHS_UPDATE, "amethyst_shard");
    }

    /** Texture path used by the Hex amethyst sconce model. */
    public static ResourceLocation sconceCopperTexture() {
        return providerTexture("copper_block", "block", "blocks");
    }

    public static ResourceLocation sconceAmethystTexture() {
        return providerTexture("amethyst_block", "block", "blocks");
    }

    /** Texture path used by the conjured-block and conjured-light item models. */
    public static ResourceLocation shardTexture() {
        return providerTexture("amethyst_shard", "item", "items");
    }

    /** Whether an ID belongs to the selected provider's named content. */
    public static boolean isSelected(ResourceLocation id, String path) {
        String selected = provider();
        return selected != null && id != null
            && selected.equals(id.getResourceDomain())
            && path.equals(id.getResourcePath());
    }

    public static boolean isSelectedCluster(ResourceLocation id) {
        return isSelected(id, "amethyst_cluster");
    }

    public static boolean isSelectedShard(ResourceLocation id) {
        return isSelected(id, "amethyst_shard");
    }

    public static boolean isSelectedAmethystBlock(String id) {
        ResourceLocation block = blockId();
        return block != null && block.toString().equals(id);
    }

    private static ResourceLocation contentId(String path) {
        String selected = provider();
        return selected == null ? null : new ResourceLocation(selected, path);
    }

    private static ResourceLocation providerTexture(String path,
                                                    String farmersFolder,
                                                    String cavesFolder) {
        String selected = provider();
        if (CAVES_NOT_CLIFFS.equals(selected)) {
            return new ResourceLocation(selected, cavesFolder + "/" + path);
        }
        return new ResourceLocation(
            selected == null ? FARMERS_FUTURE_DELIGHT : selected,
            farmersFolder + "/" + path);
    }
}
