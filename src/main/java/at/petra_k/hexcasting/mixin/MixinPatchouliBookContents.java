package at.petra_k.hexcasting.mixin;

import at.petra_k.hexcasting.api.HexAPI;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.patchouli.client.book.BookContents;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;

/**
 * Restores the JSON i18n pass used by the modern Hex book.
 *
 * <p>Patchouli 1.0-23.6 selects the localized JSON tree, but does not
 * translate language keys contained in that tree. Hex's migrated book keeps
 * keys such as {@code hexcasting.page.*} in its JSON, so translate those
 * values before Patchouli deserializes the book.</p>
 */
@Mixin(BookContents.class)
public abstract class MixinPatchouliBookContents {
    @Inject(method = "loadLocalizedJson", at = @At("RETURN"),
        cancellable = true, remap = false)
    private void hexcasting$translateBookJson(
        ResourceLocation resource,
        CallbackInfoReturnable<Reader> cir
    ) {
        if (resource == null
            || !HexAPI.MOD_ID.equals(resource.getResourceDomain())) {
            return;
        }

        Reader source = cir.getReturnValue();
        if (source == null) {
            return;
        }

        try {
            JsonElement root = new JsonParser().parse(readAll(source));
            translate(root);
            cir.setReturnValue(new StringReader(root.toString()));
        } catch (IOException | RuntimeException ignored) {
            // Keep Patchouli's original reader when an unrelated JSON file
            // cannot be parsed by this compatibility layer.
        }
    }

    private static String readAll(Reader reader) throws IOException {
        char[] buffer = new char[2048];
        StringBuilder result = new StringBuilder();
        int read;
        while ((read = reader.read(buffer)) != -1) {
            result.append(buffer, 0, read);
        }
        return result.toString();
    }

    private static JsonElement translate(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return element;
        }

        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                entry.setValue(translate(entry.getValue()));
            }
            return element;
        }

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                array.set(i, translate(array.get(i)));
            }
            return element;
        }

        if (!element.isJsonPrimitive()
            || !element.getAsJsonPrimitive().isString()) {
            return element;
        }

        String key = element.getAsString();
        if (!isTranslationKey(key)) {
            return element;
        }

        String translated = format(key);
        if (key.startsWith("hexcasting.action.hexcasting:")) {
            // Modern Patchouli accepts an action id in this field and
            // resolves it through Hex's action-aware processor. The 1.12
            // reader treats it as an ordinary language key, so use the
            // equivalent legacy key as a compatibility fallback.
            String legacyKey = "hexcasting.action."
                + key.substring("hexcasting.action.hexcasting:".length());
            String legacyTranslated = format(legacyKey);
            if (!legacyKey.equals(legacyTranslated)) {
                translated = legacyTranslated;
            }
        }
        return key.equals(translated) ? element : new JsonPrimitive(translated);
    }

    private static String format(String key) {
        try {
            return I18n.format(key);
        } catch (RuntimeException ignored) {
            return key;
        }
    }

    private static boolean isTranslationKey(String value) {
        return value.startsWith("hexcasting.")
            || value.startsWith("patchouli.")
            || value.startsWith("item.")
            || value.startsWith("block.")
            || value.startsWith("entity.");
    }
}
