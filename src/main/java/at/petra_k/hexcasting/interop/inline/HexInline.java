package at.petra_k.hexcasting.interop.inline;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import com.samsthenerd.inline.api.InlineAPI;
import com.samsthenerd.inline.client.InlineBuiltins;
import com.samsthenerd.inline.client.InlineClientEvents;
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
        if (!initialized) {
            InlineAPI.addDataType(InlinePatternData.TYPE);
            InlineAPI.registerRenderer(InlinePatternData.class,
                InlinePatternRenderer.INSTANCE);
            InlineBuiltins.register();
            MinecraftForge.EVENT_BUS.register(InlineClientEvents.class);
            initialized = true;
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
