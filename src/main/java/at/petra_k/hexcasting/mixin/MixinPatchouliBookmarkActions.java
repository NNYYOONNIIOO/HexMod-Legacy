package at.petra_k.hexcasting.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.patchouli.client.base.PersistentData;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookEntry;
import vazkii.patchouli.client.book.gui.button.GuiButtonBookBookmark;

/** Moves bookmark removal from Shift+left-click to Shift+right-click. */
@Mixin(value = GuiBook.class, remap = false)
public abstract class MixinPatchouliBookmarkActions {
    @Shadow
    boolean needsBookmarkUpdate;

    @Inject(method = "func_146284_a", at = @At("HEAD"), cancellable = true)
    private void hexcasting$keepShiftLeftAsOpen(net.minecraft.client.gui.GuiButton button,
                                                 CallbackInfo info) {
        if (!GuiScreen.isShiftKeyDown()
            || !(button instanceof GuiButtonBookBookmark)) {
            return;
        }

        GuiButtonBookBookmark bookmarkButton =
            (GuiButtonBookBookmark) button;
        if (bookmarkButton.bookmark == null || bookmarkButton.multiblock) {
            return;
        }

        GuiBook gui = (GuiBook) (Object) this;
        BookEntry entry = bookmarkButton.bookmark.getEntry(gui.book);
        if (entry == null) {
            return;
        }

        gui.displayLexiconGui(new GuiBookEntry(gui.book, entry,
            bookmarkButton.bookmark.page), true);
        GuiBook.playBookFlipSound(gui.book);
        info.cancel();
    }

    @Inject(method = "func_73864_a", at = @At("HEAD"), cancellable = true)
    private void hexcasting$removeBookmarkWithRightClick(int mouseX, int mouseY,
                                                          int mouseButton,
                                                          CallbackInfo info) {
        if (mouseButton != 1 || !GuiScreen.isShiftKeyDown()) {
            return;
        }

        GuiBook gui = (GuiBook) (Object) this;
        for (GuiButton candidate : gui.getButtonList()) {
            if (!(candidate instanceof GuiButtonBookBookmark)) {
                continue;
            }

            GuiButtonBookBookmark bookmarkButton =
                (GuiButtonBookBookmark) candidate;
            if (bookmarkButton.bookmark == null
                || bookmarkButton.multiblock
                || !bookmarkButton.mousePressed(Minecraft.getMinecraft(),
                    mouseX, mouseY)
                || bookmarkButton.bookmark.getEntry(gui.book) == null) {
                continue;
            }

            if (PersistentData.data != null) {
                PersistentData.DataHolder.BookData bookData =
                    PersistentData.data.getBookData(gui.book);
                if (bookData != null && bookData.bookmarks.remove(
                    bookmarkButton.bookmark)) {
                    PersistentData.save();
                    needsBookmarkUpdate = true;
                }
            }
            info.cancel();
            return;
        }
    }
}
