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
    /** 1.20.1's default readable pattern outer-stroke colour. */
    public static final int DEFAULT_PATTERN_COLOR = 0xFFD2C8C8;
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
        InlineAPI.addChatMatcher(new HexPatternInlineMatcher());
        com.samsthenerd.inline.common.InlineCommonBuiltins.register();
        if (net.minecraftforge.fml.common.FMLCommonHandler.instance().getSide().isClient()) {
            loadClientIntegration();
            try {
                Class<?> patternPage = Class.forName(
                    "at.petra_k.hexcasting.client.HexPatternPage");
                patternPage.getMethod("register").invoke(null);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(
                    "Unable to initialize Patchouli pattern page", exception);
            }
            try {
                Class<?> renderer = Class.forName(
                    "at.petra_k.hexcasting.interop.inline.InlinePatternChatRenderer");
                renderer.getMethod("register").invoke(null);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to initialize pattern chat renderer", exception);
            }
        }
        initialized = true;
    }

    private static void loadClientIntegration() {
        try {
            Class<?> builtins = Class.forName("com.samsthenerd.inline.client.InlineBuiltins");
            builtins.getMethod("register").invoke(null);
            Class<?> events = Class.forName("com.samsthenerd.inline.client.InlineClientEvents");
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(events);
            Class<?> tooltip = Class.forName("com.samsthenerd.inline.client.InlineTooltipEvents");
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(tooltip);
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

    /** Format a pattern token with the ARGB colour used by its source UI. */
    public static String formatPattern(HexPattern pattern, int argb) {
        init();
        return InlinePatternRenderer.render(pattern, argb);
    }
}
