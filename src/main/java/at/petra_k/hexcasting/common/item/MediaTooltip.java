package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.misc.MediaConstants;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

/** The 1.20.1-style media amount line used by all portable media holders. */
final class MediaTooltip {
    private static final DecimalFormat DUST_AMOUNT = new DecimalFormat("###,###.##");
    private static final DecimalFormat PERCENTAGE = new DecimalFormat("####");

    static {
        PERCENTAGE.setRoundingMode(RoundingMode.DOWN);
    }

    private MediaTooltip() {
    }

    static void add(List<String> tooltip, long media, long maxMedia) {
        if (tooltip == null || maxMedia <= 0L) {
            return;
        }
        long clamped = Math.max(0L, Math.min(maxMedia, media));
        String amount = DUST_AMOUNT.format(clamped / (float) MediaConstants.DUST_UNIT);
        String capacity = I18n.translateToLocalFormatted(
            "hexcasting.tooltip.media", DUST_AMOUNT.format(
                maxMedia / (float) MediaConstants.DUST_UNIT));
        String percentage = PERCENTAGE.format(
            100.0D * clamped / (double) maxMedia) + "%";

        String mediaColor = TextFormatting.LIGHT_PURPLE.toString();
        tooltip.add(I18n.translateToLocalFormatted(
            "hexcasting.tooltip.media_amount.advanced",
            mediaColor + amount + TextFormatting.RESET,
            mediaColor + capacity + TextFormatting.RESET,
            mediaColor + percentage + TextFormatting.RESET));
    }
}
