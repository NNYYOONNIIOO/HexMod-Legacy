package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.misc.MediaConstants;
import net.minecraft.util.text.translation.I18n;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

/** The 1.20.1-style media amount line used by all portable media holders. */
public final class MediaTooltip {
    private static final String TOKEN_PREFIX = "\uE000hexcasting:media:";
    private static final String TOKEN_SUFFIX = "\uE001";
    /** The exact RGB used by Hex for the current amount and capacity. */
    public static final int HEX_COLOR = 0xFFB38EF3;
    private static final DecimalFormat DUST_AMOUNT = new DecimalFormat("###,###.##");
    private static final DecimalFormat PERCENTAGE = new DecimalFormat("####");

    static {
        PERCENTAGE.setRoundingMode(RoundingMode.DOWN);
    }

    private MediaTooltip() {
    }

    public static void add(List<String> tooltip, long media, long maxMedia) {
        if (tooltip == null || maxMedia <= 0L) {
            return;
        }
        long clamped = Math.max(0L, Math.min(maxMedia, media));
        tooltip.add(token(clamped, maxMedia));
    }

    public static boolean containsToken(String text) {
        return text != null && text.startsWith(TOKEN_PREFIX)
            && text.endsWith(TOKEN_SUFFIX);
    }

    public static Entry parse(String text) {
        if (!containsToken(text)) {
            return null;
        }
        String encoded = text.substring(TOKEN_PREFIX.length(),
            text.length() - TOKEN_SUFFIX.length());
        int separator = encoded.indexOf('/');
        if (separator <= 0 || separator >= encoded.length() - 1) {
            return null;
        }
        try {
            long media = Long.parseLong(encoded.substring(0, separator));
            long maxMedia = Long.parseLong(encoded.substring(separator + 1));
            return maxMedia <= 0L ? null : new Entry(media, maxMedia);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /** Plain text used only to reserve the tooltip's vanilla width. */
    public static String plainText(Entry entry) {
        if (entry == null) {
            return "";
        }
        String[] values = formattedValues(entry);
        return I18n.translateToLocalFormatted(
            "hexcasting.tooltip.media_amount.advanced", values[0], values[1], values[2]);
    }

    public static String[] formattedValues(Entry entry) {
        if (entry == null) {
            return new String[] {"", "", ""};
        }
        String amount = DUST_AMOUNT.format(entry.media / (float) MediaConstants.DUST_UNIT);
        String capacity = I18n.translateToLocalFormatted(
            "hexcasting.tooltip.media", DUST_AMOUNT.format(
                entry.maxMedia / (float) MediaConstants.DUST_UNIT));
        String percentage = PERCENTAGE.format(
            100.0D * entry.media / (double) entry.maxMedia) + "%";
        return new String[] {amount, capacity, percentage};
    }

    public static String advancedTemplate() {
        return I18n.translateToLocal("hexcasting.tooltip.media_amount.advanced");
    }

    /** The vanilla 1.12.2 item-bar colour used by Hex's media holder. */
    public static int barColor(long media, long maxMedia) {
        float amount = maxMedia <= 0L ? 0.0F
            : (float) Math.max(0.0D, Math.min(1.0D,
                media / (double) maxMedia));
        int red = lerp(84, 254, amount);
        int green = lerp(57, 203, amount);
        int blue = lerp(138, 230, amount);
        return red << 16 | green << 8 | blue;
    }

    /** Vanilla's 13-pixel bar is rounded in the same way as Hex's helper. */
    public static int barWidth(long media, long maxMedia) {
        if (maxMedia <= 0L) {
            return 0;
        }
        double amount = Math.max(0.0D, Math.min(1.0D,
            media / (double) maxMedia));
        return (int) Math.round(13.0D * amount);
    }

    private static int lerp(int from, int to, float amount) {
        return Math.round(from + (to - from) * amount);
    }

    public static String blankText(Entry entry, net.minecraft.client.gui.FontRenderer font) {
        if (entry == null || font == null) {
            return "";
        }
        int width = font.getStringWidth(plainText(entry));
        return widthPreservingBlank(font, width);
    }

    /**
     * Build an invisible line whose width is measured by the active font.
     * SmoothFont changes the width of a space, so converting pixels to a
     * hard-coded number of four-pixel spaces makes the tooltip too narrow.
     * A non-breaking space survives the tooltip line trimming performed by
     * Forge and SmoothFont intentionally gives it the same width as a space.
     */
    private static String widthPreservingBlank(
        net.minecraft.client.gui.FontRenderer font, int targetWidth) {
        int target = Math.max(1, targetWidth);
        String reset = net.minecraft.util.text.TextFormatting.RESET.toString();
        StringBuilder result = new StringBuilder(reset);
        char blank = '\u00a0';
        int previousWidth = font.getStringWidth(result.toString());
        int blankWidth = font.getStringWidth(String.valueOf(blank));
        if (blankWidth <= 0) {
            blank = ' ';
        }

        for (int count = 0; count < 4096; count++) {
            if (count > 0 && previousWidth >= target) {
                break;
            }
            result.append(blank);
            int currentWidth = font.getStringWidth(result.toString());
            if (currentWidth <= previousWidth && blank != ' ') {
                // Some custom fonts do not expose a glyph width for NBSP.
                // Fall back to the ordinary space before giving up.
                result.setLength(result.length() - 1);
                blank = ' ';
                result.append(blank);
                currentWidth = font.getStringWidth(result.toString());
            }
            if (currentWidth <= previousWidth) {
                break;
            }
            previousWidth = currentWidth;
        }

        // Keep the line alive even for a font that reports zero-width blanks.
        if (result.length() == reset.length()) {
            result.append(blank);
        }
        return result.toString();
    }

    public static String token(long media, long maxMedia) {
        return TOKEN_PREFIX + Math.max(0L, media) + "/"
            + Math.max(1L, maxMedia) + TOKEN_SUFFIX;
    }

    /** Immutable media values decoded from a tooltip token. */
    public static final class Entry {
        private final long media;
        private final long maxMedia;

        private Entry(long media, long maxMedia) {
            this.media = Math.max(0L, Math.min(maxMedia, media));
            this.maxMedia = Math.max(1L, maxMedia);
        }

        public long getMedia() {
            return media;
        }

        public long getMaxMedia() {
            return maxMedia;
        }
    }
}
