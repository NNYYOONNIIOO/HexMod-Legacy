package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Client-side lookup for the information shown while hovering a drawn Hex
 * pattern.  The 1.20.1 accessibility mod obtains this from Patchouli's
 * pattern pages; 1.12.2 has a smaller Patchouli API, so the portable part of
 * that data is kept in a tiny resource table and resolved against the same
 * action registry used by the caster.
 */
public final class HexPatternTooltip {
    private static final ResourceLocation ARGUMENTS_RESOURCE =
        new ResourceLocation("hexcasting", "pattern_tooltip_args.json");
    private static final Map<String, String> ARGUMENTS = new HashMap<>();
    private static boolean argumentsLoaded;

    private HexPatternTooltip() {
    }

    /** Resolve a drawable pattern into the data needed by the preview UI. */
    public static Preview resolve(HexPattern pattern, World world) {
        if (pattern == null || pattern.getAngles().isEmpty()) {
            return null;
        }

        HexActionRegistry.bootstrap();
        HexAction action = HexActionRegistry.get(pattern, world);
        ResourceLocation id = action == null ? null : HexActionRegistry.idFor(action);
        if (id == null) {
            return null;
        }

        String name = localizeAction(id);
        String args = arguments(id);
        return new Preview(pattern, id, pattern.anglesSignature(), name, args);
    }

    private static String localizeAction(ResourceLocation id) {
        String key = "hexcasting.action." + id.getResourcePath();
        String translated = I18n.format(key);
        return key.equals(translated) ? id.getResourcePath() : translated;
    }

    private static String arguments(ResourceLocation id) {
        loadArguments();
        String value = ARGUMENTS.get(id.toString());
        if (value != null) {
            return value;
        }
        // Optional/add-on actions may not have a Patchouli page in this port.
        // Keep the third row useful instead of silently hiding it.
        return id.toString();
    }

    private static synchronized void loadArguments() {
        if (argumentsLoaded) {
            return;
        }
        argumentsLoaded = true;

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.getResourceManager() == null) {
            return;
        }
        try (InputStream stream = minecraft.getResourceManager()
                .getResource(ARGUMENTS_RESOURCE).getInputStream();
             InputStreamReader reader = new InputStreamReader(stream,
                 StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    ARGUMENTS.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        } catch (Exception ignored) {
            // A missing optional metadata file must not make the casting GUI
            // unusable; the resource-path fallback above remains available.
        }
    }

    /** Immutable data object consumed by GuiHexStaff's renderer. */
    public static final class Preview {
        private final HexPattern pattern;
        private final ResourceLocation id;
        private final String signature;
        private final String name;
        private final String arguments;

        private Preview(HexPattern pattern, ResourceLocation id,
                        String signature, String name, String arguments) {
            this.pattern = pattern;
            this.id = id;
            this.signature = signature;
            this.name = name;
            this.arguments = arguments;
        }

        public HexPattern getPattern() {
            return pattern;
        }

        public ResourceLocation getId() {
            return id;
        }

        public String getSignature() {
            return signature;
        }

        public String getName() {
            return name;
        }

        public String getArguments() {
            return arguments;
        }
    }
}
