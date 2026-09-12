package at.petra_k.hexcasting.interop.inline;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import com.samsthenerd.inline.api.InlineAPI;
import net.minecraftforge.common.MinecraftForge;

/**
 * Small Inline-compatible facade embedded for Minecraft 1.12.2.
 * It intentionally has no dependency on the 1.20.1 Inline, Pehkui, or Cloth
 * Config APIs; callers still get typed pattern data and stable text output.
 */
public final class HexInline {
    private static boolean initialized;

    private HexInline() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        InlineAPI.addDataType(InlinePatternData.TYPE);
        InlineAPI.registerRenderer(InlinePatternData.class,
            InlinePatternRenderer.INSTANCE);
        com.samsthenerd.inline.common.InlineCommonBuiltins.register();
        if (net.minecraftforge.fml.common.FMLCommonHandler.instance().getSide().isClient()) {
            loadClientIntegration();
        }
        initialized = true;
    }

    private static void loadClientIntegration() {
        try {
            Class<?> builtins = Class.forName("com.samsthenerd.inline.client.InlineBuiltins");
            builtins.getMethod("register").invoke(null);
            Class<?> events = Class.forName("com.samsthenerd.inline.client.InlineClientEvents");
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(events);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to initialize Inline client integration", exception);
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static InlinePatternData pattern(HexPattern pattern) {
        return new InlinePatternData(pattern);
    }

    public static String formatPattern(HexPattern pattern) {
        init();
        return InlineAPI.render(pattern(pattern));
    }
}
