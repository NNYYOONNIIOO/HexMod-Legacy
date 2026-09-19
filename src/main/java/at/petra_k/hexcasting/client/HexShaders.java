package at.petra_k.hexcasting.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.shader.ShaderLinkHelper;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Client shader bridge for the 1.12.2 renderer.
 *
 * <p>Modern Hex registers shaders through the post-1.13 rendering API.  The
 * legacy client exposes the same GLSL program through {@link ShaderManager}
 * and a resource-reload listener instead.  Keeping lifecycle management here
 * makes the grayscale renderer safe during resource-pack reloads and lets
 * entity renderers opt in without touching OpenGL program creation.</p>
 */
@SideOnly(Side.CLIENT)
public final class HexShaders implements IResourceManagerReloadListener {
    private static final Logger LOGGER = LogManager.getLogger("HexCastingShaders");
    private static final HexShaders INSTANCE = new HexShaders();

    private static ShaderManager grayscale;
    private static boolean registered;

    private HexShaders() {
    }

    /** Register the reload listener and create the initial shader program. */
    public static void register() {
        Minecraft minecraft = Minecraft.getMinecraft();
        IResourceManager resources = minecraft.getResourceManager();
        if (!registered && resources instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) resources)
                .registerReloadListener(INSTANCE);
            registered = true;
        }
        INSTANCE.onResourceManagerReload(resources);
    }

    @Override
    public void onResourceManagerReload(IResourceManager resources) {
        if (grayscale != null) {
            try {
                grayscale.deleteShader();
            } catch (RuntimeException ignored) {
                // A reload can invalidate the old GL context before the
                // resource listener sees it; the new program is still safe.
            }
            grayscale = null;
        }

        try {
            ShaderLinkHelper.setNewStaticShaderLinkHelper();
            grayscale = new ShaderManager(resources, "hexcasting__grayscale");
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Unable to load Hex Casting grayscale shader", exception);
        }
    }

    public static ShaderManager grayscale() {
        return grayscale;
    }

    /**
     * Bind a texture and begin grayscale rendering.  The caller must pair a
     * successful call with {@link #endGrayscale()} after drawing.
     */
    public static boolean beginGrayscale(ResourceLocation texture) {
        ShaderManager shader = grayscale;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (shader == null || minecraft == null || texture == null) {
            return false;
        }

        minecraft.getTextureManager().bindTexture(texture);
        ITextureObject textureObject = minecraft.getTextureManager()
            .getTexture(texture);
        if (textureObject == null) {
            return false;
        }
        shader.addSamplerTexture("Sampler0", textureObject);
        shader.useShader();
        return true;
    }

    public static void endGrayscale() {
        if (grayscale != null) {
            grayscale.endShader();
        }
    }
}
