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
            for (int i = 0; i < variants.length; i++) {
                // Blockstate model ids are rooted at assets/<mod>/models,
                // whereas item models may refer to them with a block/
                // prefix.  The old lookup used the item-model form here,
                // so the block entity renderer received null models for all
                // four decorative variants.
                String prefix = "quenched_allay".equals(blockId)
                    ? "" : "deco/";
                ResourceLocation modelLocation = HexAPI.modLoc(
                    prefix + blockId + "_" + i);
                variants[i] = event.getModelRegistry().getObject(
                    new net.minecraft.client.renderer.block.model.ModelResourceLocation(
                        modelLocation, "normal"));
            }
            MODELS.put(blockId, variants);
        }
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
