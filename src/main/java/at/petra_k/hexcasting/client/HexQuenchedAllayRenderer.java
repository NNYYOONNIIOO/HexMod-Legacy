package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.common.block.BlockQuenchedAllay;
import at.petra_k.hexcasting.common.block.TileEntityQuenchedAllay;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

/** Renders the invisible Quenched Allay block using its fixed model. */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID, value = Side.CLIENT)
public final class HexQuenchedAllayRenderer
    extends TileEntitySpecialRenderer<TileEntityQuenchedAllay> {
    private static final String[] BLOCK_IDS = {
        "quenched_allay",
        "quenched_allay_bricks",
        "quenched_allay_bricks_small",
        "quenched_allay_tiles"
    };
    private static final Map<String, IBakedModel[]> MODELS = new HashMap<>();

    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        for (String blockId : BLOCK_IDS) {
            IBakedModel[] variants = new IBakedModel[BlockQuenchedAllay.VARIANTS];
            IBakedModel fallback = findBakedModel(event, blockId, "normal");
            for (int i = 0; i < variants.length; i++) {
                String modelPath = ("quenched_allay".equals(blockId)
                    ? "" : "deco/") + blockId + "_" + i;
                variants[i] = findBakedModel(event, modelPath, "normal");
                if (variants[i] == null) {
                    // Models referenced by an item override are registered
                    // with the item-model path and inventory variant.  The
                    // same JSON is still a valid block model for the TESR.
                    variants[i] = findBakedModel(event, "block/" + modelPath,
                        "inventory");
                }
                // Keep the block visible even if a third-party model loader
                // declines an extra variant.  Variant zero is the same
                // fallback used by the blockstate and is still a valid
                // server/client-safe model.
                if (variants[i] == null) {
                    variants[i] = fallback;
                }
            }
            MODELS.put(blockId, variants);
        }
    }

    private static IBakedModel findBakedModel(ModelBakeEvent event,
                                               String path, String variant) {
        return event.getModelRegistry().getObject(
            new net.minecraft.client.renderer.block.model.ModelResourceLocation(
                HexAPI.modLoc(path), variant));
    }

    @Override
    public void render(TileEntityQuenchedAllay tile, double x, double y,
                       double z, float partialTicks, int destroyStage,
                       float alpha) {
        if (tile == null || tile.getWorld() == null
            || tile.getBlockType() == null
            || tile.getBlockType().getRegistryName() == null) {
            return;
        }

        String blockId = tile.getBlockType().getRegistryName().getResourcePath();
        IBakedModel[] variants = MODELS.get(blockId);
        if (variants == null || variants.length == 0) {
            return;
        }
        IBakedModel model = variants[HexGaslightingTracker.getVariant()
            % variants.length];
        if (model == null) {
            return;
        }

        IBlockState state = tile.getWorld().getBlockState(tile.getPos());
        BlockRendererDispatcher dispatcher =
            Minecraft.getMinecraft().getBlockRendererDispatcher();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);
        GlStateManager.enableAlpha();
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha <= 0.0F ? 1.0F : alpha);
        Minecraft.getMinecraft().getTextureManager().bindTexture(
            TextureMap.LOCATION_BLOCKS_TEXTURE);

        buffer.begin(GL11.GL_QUADS,
            net.minecraft.client.renderer.vertex.DefaultVertexFormats.BLOCK);
        dispatcher.getBlockModelRenderer().renderModel(
            tile.getWorld(), model, state, tile.getPos(), buffer, false);
        Tessellator.getInstance().draw();

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}
