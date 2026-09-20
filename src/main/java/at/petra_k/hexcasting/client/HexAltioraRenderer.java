package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelElytra;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Renders the translucent Altiora wings behind players with active grace. */
@SideOnly(Side.CLIENT)
final class HexAltioraRenderer {
    private static final net.minecraft.util.ResourceLocation TEXTURE =
        HexAPI.modLoc("textures/misc/altiora.png");
    private static final ModelElytra MODEL = new ModelElytra();

    private HexAltioraRenderer() {
    }

    static void render(RenderPlayerEvent.Post event) {
        if (event == null || event.getEntityPlayer() == null) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        if (!hasAltiora(player) || isWearingElytra(player)) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        TextureManager textures = minecraft.getTextureManager();
        textures.bindTexture(TEXTURE);

        float partialTicks = event.getPartialRenderTick();
        float ageInTicks = player.ticksExisted + partialTicks;
        float limbSwing = player.limbSwing;
        float limbSwingAmount = player.limbSwingAmount;
        float yaw = player.rotationYawHead;
        float pitch = player.rotationPitch;
        float scale = 0.0625F;

        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        // RenderPlayerEvent.Post is fired after the living renderer has
        // restored its matrix, so replay the same world-relative origin that
        // vanilla's LayerElytra receives while it is inside that renderer.
        GlStateManager.translate(event.getX(), event.getY(), event.getZ());
        GlStateManager.translate(0.0F, 0.0F, 0.125F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);

        MODEL.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks,
            yaw, pitch, scale, player);
        MODEL.render(player, limbSwing, limbSwingAmount, ageInTicks,
            yaw, pitch, scale);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }

    private static boolean hasAltiora(EntityPlayer player) {
        if (HexCapabilities.CASTING_DATA == null) {
            return false;
        }
        IHexCastingData data = player.getCapability(
            HexCapabilities.CASTING_DATA, null);
        return data != null && data.isAltioraActive();
    }

    private static boolean isWearingElytra(EntityPlayer player) {
        if (player.inventory == null || player.inventory.armorInventory == null
            || player.inventory.armorInventory.size() <= 2) {
            return false;
        }
        ItemStack chest = player.inventory.armorInventory.get(2);
        return chest != null && !chest.isEmpty() && chest.getItem() == Items.ELYTRA;
    }
}
