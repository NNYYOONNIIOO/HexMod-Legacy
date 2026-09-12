package com.samsthenerd.inline.client;

import com.samsthenerd.inline.api.InlineAPI;
import com.samsthenerd.inline.api.InlineData;
import com.samsthenerd.inline.api.InlineRenderContext;
import com.samsthenerd.inline.api.MatchContext;
import com.samsthenerd.inline.api.data.EntityInlineData;
import com.samsthenerd.inline.api.data.ItemInlineData;
import com.samsthenerd.inline.api.data.ModIconData;
import com.samsthenerd.inline.api.data.PlayerHeadData;
import com.samsthenerd.inline.matching.RegexInlineMatcher;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.UUID;
import java.util.regex.Pattern;

/** Registers the player-facing matchers shipped by the Inline port. */
public final class InlineBuiltins {
    private static boolean registered;

    private InlineBuiltins() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        InlineAPI.addDataType(ItemInlineData.TYPE);
        InlineAPI.addDataType(EntityInlineData.TYPE);
        InlineAPI.addDataType(PlayerHeadData.TYPE);
        InlineAPI.addDataType(ModIconData.TYPE);
        InlineAPI.registerRenderer(ItemInlineData.class,
                (data, context) -> renderItem(data));
        InlineAPI.registerRenderer(EntityInlineData.class,
                (data, context) -> renderEntity(data));
        InlineAPI.registerRenderer(PlayerHeadData.class,
                (data, context) -> renderPlayer(data));
        InlineAPI.registerRenderer(ModIconData.class,
            (data, context) -> renderMod(data));

        InlineAPI.addChatMatcher(new RegexInlineMatcher(
            Pattern.compile("\\[item:([^\\]]+)\\]"),
            (matcher, context) -> item(matcher.group(1))));
        InlineAPI.addChatMatcher(new RegexInlineMatcher(
            Pattern.compile("\\[entity:([^\\]]+)\\]"),
            (matcher, context) -> entity(matcher.group(1))));
        InlineAPI.addChatMatcher(new RegexInlineMatcher(
            Pattern.compile("\\[face:([^\\]]+)\\]"),
            (matcher, context) -> new PlayerHeadData(
                new com.mojang.authlib.GameProfile(
                    UUID.nameUUIDFromBytes(matcher.group(1).getBytes()), matcher.group(1)))));
        InlineAPI.addChatMatcher(new RegexInlineMatcher(
            Pattern.compile("\\[mod:([^\\]]+)\\]"),
            (matcher, context) -> new ModIconData(matcher.group(1))));
        InlineAPI.addChatMatcher(new RegexInlineMatcher(
            Pattern.compile("\\[show:(hand|offhand)\\]"),
            InlineBuiltins::heldItem));
        registered = true;
    }

    private static InlineData<?> heldItem(java.util.regex.Matcher matcher, MatchContext context) {
        if (context == null || context.getViewer() == null) {
            return null;
        }
        net.minecraft.util.EnumHand hand = "offhand".equals(matcher.group(1))
            ? net.minecraft.util.EnumHand.OFF_HAND : net.minecraft.util.EnumHand.MAIN_HAND;
        ItemStack stack = context.getViewer().getHeldItem(hand);
        return stack.isEmpty() ? null : new ItemInlineData(stack);
    }

    private static ItemInlineData item(String value) {
        try {
            ResourceLocation id = new ResourceLocation(value);
            Item item = Item.REGISTRY.getObject(id);
            return item == null ? null : new ItemInlineData(new ItemStack(item));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static EntityInlineData entity(String value) {
        try {
            return new EntityInlineData(new ResourceLocation(value));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String renderItem(ItemInlineData data) {
        ResourceLocation id = Item.REGISTRY.getNameForObject(data.getStack().getItem());
        return "[item:" + (id == null ? "unknown" : id.toString()) + "]";
    }

    private static String renderEntity(EntityInlineData data) {
        return data.getEntity() == null
            ? "[entity:" + data.getTypeId() + "]"
            : "[entity:" + data.getEntity().getName() + "]";
    }

    private static String renderPlayer(PlayerHeadData data) {
        return "[face:" + data.getProfile().getName() + "]";
    }

    private static String renderMod(ModIconData data) {
        String name = I18n.format("mod." + data.getModId() + ".name");
        return "[" + (name.equals("mod." + data.getModId() + ".name")
            ? data.getModId() : name) + "]";
    }

    /** Client-only drawing helpers used by InlineClientEvents. */
    public static void renderItemGui(ItemInlineData data, int x, int y, float scale) {
        Minecraft minecraft = Minecraft.getMinecraft();
        RenderItem renderer = minecraft.getRenderItem();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0F);
        GlStateManager.scale(scale, scale, scale);
        RenderHelper.enableGUIStandardItemLighting();
        renderer.renderItemAndEffectIntoGUI(data.getStack(), 0, 0);
        renderer.renderItemOverlayIntoGUI(minecraft.fontRenderer, data.getStack(), 0, 0, null);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    public static void renderEntityGui(EntityInlineData data, int x, int y, float scale) {
        Minecraft minecraft = Minecraft.getMinecraft();
        Entity entity = data.getEntity();
        if (entity == null) {
            EntityEntry entry = ForgeRegistries.ENTITIES.getValue(data.getTypeId());
            if (entry == null || entry.getEntityClass() == null) return;
            try {
                entity = entry.getEntityClass().getConstructor(net.minecraft.world.World.class)
                    .newInstance(minecraft.world);
            } catch (ReflectiveOperationException ignored) {
                return;
            }
        }
        RenderManager manager = minecraft.getRenderManager();
        Render<Entity> render = manager.getEntityRenderObject(entity);
        if (render == null) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 50.0F);
        GlStateManager.scale(-scale, scale, scale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        RenderHelper.enableStandardItemLighting();
        render.doRender(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    public static void renderPlayerHeadGui(PlayerHeadData data, int x, int y, int size) {
        Minecraft minecraft = Minecraft.getMinecraft();
        java.util.Map<com.mojang.authlib.minecraft.MinecraftProfileTexture.Type,
            com.mojang.authlib.minecraft.MinecraftProfileTexture> textures =
            minecraft.getSessionService().getTextures(data.getProfile(), false);
        com.mojang.authlib.minecraft.MinecraftProfileTexture skinTexture =
            textures.get(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN);
        ResourceLocation skin = new ResourceLocation("textures/entity/steve.png");
        if (skinTexture != null) {
            skin = minecraft.getSkinManager().loadSkin(skinTexture,
                com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN);
        }
        if (skin == null) skin = new ResourceLocation("textures/entity/steve.png");
        minecraft.getTextureManager().bindTexture(skin);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawScaledCustomSizeModalRect(x, y, 8.0F, 8.0F, 8, 8, size, size, 64.0F, 64.0F);
        Gui.drawScaledCustomSizeModalRect(x, y, 40.0F, 8.0F, 8, 8, size, size, 64.0F, 64.0F);
    }

    public static void renderModIconGui(ModIconData data, int x, int y, int size) {
        ResourceLocation icon = new ResourceLocation(data.getModId(), "textures/\u0000");
        net.minecraftforge.fml.common.ModContainer container = net.minecraftforge.fml.common.Loader.instance()
            .getIndexedModList().get(data.getModId());
        if (container != null && container.getMetadata() != null && container.getMetadata().logoFile != null
            && !container.getMetadata().logoFile.isEmpty()) {
            icon = new ResourceLocation(data.getModId(), container.getMetadata().logoFile);
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(icon);
        Gui.drawScaledCustomSizeModalRect(x, y, 0.0F, 0.0F, 16, 16, size, size, 16.0F, 16.0F);
    }
}
