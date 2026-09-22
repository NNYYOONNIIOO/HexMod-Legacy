package at.petra_k.hexcasting.mixin;

import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.patchouli.client.book.gui.button.GuiButtonBook;
import vazkii.patchouli.client.book.gui.button.GuiButtonBookBookmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Supplies localized tooltip text for Patchouli bookmark buttons. */
@Mixin(value = GuiButtonBook.class, remap = false)
public abstract class MixinPatchouliBookmarkTooltip {
    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
    private void hexcasting$localizeBookmarkTooltip(
        CallbackInfoReturnable<List<String>> info
    ) {
        if (!((Object) this instanceof GuiButtonBookBookmark)) {
            return;
        }

        GuiButtonBookBookmark button = (GuiButtonBookBookmark) (Object) this;
        boolean addBookmark = button.bookmark == null;
        String key = addBookmark
            ? "hexcasting.patchouli.bookmark.add"
            : "hexcasting.patchouli.bookmark.remove";
        String localized = I18n.format(key);
        if (localized.equals(key)) {
            localized = addBookmark ? "Add Bookmark"
                : "(Hold sneak and right-click to remove)";
        }

        if (addBookmark) {
            info.setReturnValue(Collections.singletonList(localized));
            return;
        }

        List<String> original = info.getReturnValue();
        List<String> tooltip = original == null
            ? new ArrayList<String>() : new ArrayList<>(original);
        if (tooltip.isEmpty()) {
            tooltip.add(localized);
        } else if (tooltip.size() == 1) {
            tooltip.add(localized);
        } else {
            tooltip.set(1, localized);
        }
        info.setReturnValue(tooltip);
    }
}
