package at.petra_k.hexcasting.interop.inline;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import mezz.jei.api.gui.IDrawable;
import net.minecraft.client.Minecraft;
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
    private final HexPattern pattern;
    private final int width;
    private final int height;
    private boolean drawDots = true;

    public PatternDrawable(ResourceLocation action, int width, int height) {
        this.pattern = HexActionRegistry.getPattern(action);
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
        if (pattern == null) {
            return;
        }
        HexPatternChatGeometry.drawPreview(pattern, xOffset, yOffset,
            Math.min(width, height), 255, 0xC80C0A0C, 0xFF333030, drawDots);
    }
}
