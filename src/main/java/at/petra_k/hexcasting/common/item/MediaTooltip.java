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

    public static String blankText(Entry entry, net.minecraft.client.gui.FontRenderer font) {
        if (entry == null || font == null) {
            return "";
        }
        int width = font.getStringWidth(plainText(entry));
        int spaces = Math.max(1, (width + 3) / 4);
        StringBuilder result = new StringBuilder(spaces + 2);
        // Keep a zero-width formatting code so GuiUtils does not trim the
        // placeholder line before PostText gets a chance to paint RGB text.
        result.append(net.minecraft.util.text.TextFormatting.RESET);
        for (int i = 0; i < spaces; i++) {
            result.append(' ');
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
