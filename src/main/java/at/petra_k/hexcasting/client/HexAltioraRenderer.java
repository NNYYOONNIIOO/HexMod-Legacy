package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelElytra;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

/** Renders Altiora through the same model transform as vanilla's Elytra layer. */
final class HexAltioraRenderer implements LayerRenderer<AbstractClientPlayer> {
    private static final net.minecraft.util.ResourceLocation TEXTURE =
        HexAPI.modLoc("textures/misc/altiora.png");
    private static final ModelElytra MODEL = new ModelElytra();

    @Override
    public void doRenderLayer(AbstractClientPlayer player, float limbSwing,
                              float limbSwingAmount, float partialTicks,
                              float ageInTicks, float netHeadYaw,
                              float headPitch, float scale) {
        if (!hasAltiora(player) || isWearingElytra(player)
            || player.onGround || player.collided
            || player.capabilities.isFlying) {
            return;
        }

        TextureManager textures = Minecraft.getMinecraft().getTextureManager();
        textures.bindTexture(TEXTURE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);

        // RenderLivingBase has already applied the player's translation,
        // rotation, model scale, and the exact body transform used by
        // LayerElytra. Keep this identical to vanilla instead of drawing
        // again from RenderPlayerEvent.Post with world coordinates.
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, 0.125F);
        MODEL.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks,
            netHeadYaw, headPitch, scale, player);
        MODEL.render(player, limbSwing, limbSwingAmount, ageInTicks,
            netHeadYaw, headPitch, scale);
        GlStateManager.popMatrix();

        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }

    private static boolean hasAltiora(AbstractClientPlayer player) {
        if (player == null || HexCapabilities.CASTING_DATA == null) {
            return false;
        }
        IHexCastingData data = player.getCapability(
            HexCapabilities.CASTING_DATA, null);
        return data != null && data.isAltioraActive();
    }

    private static boolean isWearingElytra(AbstractClientPlayer player) {
        ItemStack chest = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        return chest != null && !chest.isEmpty() && chest.getItem() == Items.ELYTRA;
    }
}
