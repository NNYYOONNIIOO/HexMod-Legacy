package at.petra_k.hexcasting.interop.inline;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import mezz.jei.api.gui.IDrawable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/**
 * JEI icon for a registered Hex action.
 *
 * <p>The old JEI API still draws into Minecraft's immediate-mode GUI.  The
 * inline renderer already has the 1.20.1-compatible fitted line and dot
 * geometry, so sharing it keeps category icons from regressing to plain text
 * or blocky line art.</p>
 */
public final class PatternDrawable implements IDrawable {
    private final ResourceLocation action;
    private final int width;
    private final int height;
    private boolean drawDots = true;

    public PatternDrawable(ResourceLocation action, int width, int height) {
        // JEI constructs category tabs during client startup.  At that point
        // nothing may have queried the action registry yet, so force the
        // shared action table to be populated before resolving the icon.
        HexActionRegistry.bootstrap();
        this.action = action;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
    }

    public PatternDrawable strokeOrder(boolean enabled) {
        this.drawDots = enabled;
        return this;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void draw(Minecraft minecraft, int xOffset, int yOffset) {
        // HEI can invoke category icons after an item tooltip or an entity
        // preview has changed the immediate-mode colour.  Pattern vertices
        // carry their own colour, so a stale black current colour would make
        // every icon disappear.  Resolve at draw time as well: this keeps
        // the icon valid if the category was constructed before the action
        // table finished bootstrapping.
        HexActionRegistry.bootstrap();
        HexPattern pattern = HexActionRegistry.getPattern(action);
        if (pattern == null) {
            return;
        }
        try {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            HexPatternChatGeometry.drawPreview(pattern, xOffset, yOffset,
                Math.min(width, height), 255, 0xC80C0A0C, 0xFF333030, drawDots);
        } finally {
            GlStateManager.resetColor();
        }
    }
}
