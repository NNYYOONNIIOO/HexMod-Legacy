package at.petra_k.hexcasting.mixin;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.item.ItemPatternScroll;
import at.petra_k.hexcasting.common.item.ItemSlate;
import at.petra_k.hexcasting.interop.inline.InlinePatternChatRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds Hex's purple Shift pattern overlay to rendered item stacks. */
@Mixin(RenderItem.class)
public abstract class MixinRenderItem {
    @Inject(method = "renderItemOverlayIntoGUI", at = @At("RETURN"))
    private void hexcasting$renderPatternOverlay(FontRenderer font,
                                                  ItemStack stack,
                                                  int x, int y, String text,
                                                  CallbackInfo info) {
        if (!GuiScreen.isShiftKeyDown() || stack == null || stack.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        HexPattern pattern = null;
        if (stack.getItem() instanceof ItemPatternScroll) {
            pattern = ItemPatternScroll.getPattern(stack,
                minecraft == null ? null : minecraft.world);
        } else if (stack.getItem() instanceof ItemSlate) {
            pattern = ItemSlate.getPattern(stack);
        }
        if (pattern == null) {
            return;
        }

        InlinePatternChatRenderer.drawItemPatternOverlay(pattern, x, y);
    }
}
