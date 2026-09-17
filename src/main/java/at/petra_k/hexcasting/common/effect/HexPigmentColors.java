package at.petra_k.hexcasting.common.effect;

import at.petra_k.hexcasting.api.capability.IHexCastingData;

import java.util.Random;
import java.util.UUID;

/** 1.20.1-compatible color sampling for frozen Hex pigments. */
public final class HexPigmentColors {
    private static final int[] MINIMUM_LUMINANCE = {
        0x200000, 0x202000, 0x002000, 0x002020, 0x000020, 0x200020
    };
    private static final int[] ANCIENT = {
        0x54398A, 0xCFA0F3, 0xFECBE6, 0xCFA0F3, 0xE77C56
    };

    private HexPigmentColors() {
    }

    public static int fromData(IHexCastingData data, float time,
                               double x, double y, double z) {
        if (data == null) {
            return 0xAA66FF;
        }
        return color(data.getPigmentVariant(), data.getPigment(),
            data.getPigmentOwner(), time, x, y, z);
    }

    public static int color(String variant, int fallback, UUID owner,
                            float time, double x, double y, double z) {
        int raw = fallback & 0xFFFFFF;
        if (variant == null) {
            return visible(raw, time, x, y, z);
        }
        if (variant.startsWith("pride_colorizer_")) {
            int[] palette = pridePalette(variant.substring("pride_colorizer_".length()));
            raw = morph(palette, time / 400.0F, x, y, z);
        } else if ("ancient_colorizer".equals(variant)) {
            raw = morph(ANCIENT, time / 600.0F, x, y, z);
        } else if ("uuid_colorizer".equals(variant)) {
            raw = uuidColor(owner, time, x, y, z);
        }
        return visible(raw, time, x, y, z);
    }

    /** Match ColorProvider's minimum-luminance safeguard for dark pigments. */
    private static int visible(int raw, float time,
                               double x, double y, double z) {
        int red = (raw >> 16) & 0xFF;
        int green = (raw >> 8) & 0xFF;
        int blue = raw & 0xFF;
        double luminance = (0.2126D * red + 0.7152D * green
            + 0.0722D * blue) / 255.0D;
        if (luminance < 0.05D) {
            int boost = morph(MINIMUM_LUMINANCE, time / 400.0F, x, y, z);
            red += (boost >> 16) & 0xFF;
            green += (boost >> 8) & 0xFF;
            blue += boost & 0xFF;
        }
        return (Math.min(255, red) << 16) | (Math.min(255, green) << 8)
            | Math.min(255, blue);
    }

    private static int uuidColor(UUID owner, float time,
                                 double x, double y, double z) {
        UUID safe = owner == null ? new UUID(0L, 0L) : owner;
        Random random = new Random(safe.getMostSignificantBits()
            ^ safe.getLeastSignificantBits());
        int first = hsv(random.nextFloat(), 0.4F + random.nextFloat() * 0.4F,
            0.7F + random.nextFloat() * 0.3F);
        int second = hsv(random.nextFloat(), 0.7F + random.nextFloat() * 0.3F,
            0.2F + random.nextFloat() * 0.5F);
        return morph(new int[] {first, second}, time / 400.0F, x, y, z);
    }

    private static int morph(int[] palette, float time,
                             double x, double y, double z) {
        if (palette == null || palette.length == 0) {
            return 0xAA66FF;
        }
        double phase = positiveModulo(time + 0.1D * (x + y + z), 1.0D)
            * palette.length;
        int start = (int) Math.floor(phase) % palette.length;
        int end = (start + 1) % palette.length;
        double raw = phase - Math.floor(phase);
        double t = raw < 0.5D ? 4.0D * raw * raw * raw
            : 1.0D - Math.pow(-2.0D * raw + 2.0D, 3.0D) / 2.0D;
        return blend(palette[start], palette[end], t);
    }

    private static int blend(int first, int second, double t) {
        int r = (int) Math.round(((first >> 16) & 0xFF) * (1.0D - t)
            + ((second >> 16) & 0xFF) * t);
        int g = (int) Math.round(((first >> 8) & 0xFF) * (1.0D - t)
            + ((second >> 8) & 0xFF) * t);
        int b = (int) Math.round((first & 0xFF) * (1.0D - t)
            + (second & 0xFF) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static int[] pridePalette(String name) {
        switch (name) {
            case "agender": return new int[] {0x16A10C, 0xFFFFFF, 0x7A8081, 0x302F30};
            case "aroace": return new int[] {0x7210BC, 0xEBF367, 0xFFFFFF, 0x82DCEB, 0x2F4DD8};
            case "aromantic": return new int[] {0x16A10C, 0x82EB8B, 0xFFFFFF, 0x7A8081, 0x302F30};
            case "asexual": return new int[] {0x333233, 0x9A9FA1, 0xFFFFFF, 0x7210BC};
            case "bisexual": return new int[] {0xDB45FF, 0x9C2BD0, 0x6894D4};
            case "demiboy": return new int[] {0x9A9FA1, 0xA9FFFF, 0xFFFFFF};
            case "demigirl": return new int[] {0x9A9FA1, 0xFCB1FF, 0xFFFFFF};
            case "gay": return new int[] {0xD82F3A, 0xE0883F, 0xEBF367, 0x2DB418, 0x2F4DD8};
            case "genderfluid": return new int[] {0xFBACF9, 0xFFFFFF, 0x9C2BD0, 0x333233, 0x2F4DD8};
            case "genderqueer": return new int[] {0xCA78EF, 0xFFFFFF, 0x2DB418};
            case "intersex": return new int[] {0xEBF367, 0x7210BC};
            case "lesbian": return new int[] {0xD82F3A, 0xEFB87D, 0xFFFFFF, 0xFBACF9, 0xA30262};
            case "nonbinary": return new int[] {0xEBF367, 0xFFFFFF, 0x7210BC, 0x333233};
            case "pansexual": return new int[] {0xE278EF, 0xEBF367, 0x6AC2E4};
            case "plural": return new int[] {0x30C69F, 0x347DDF, 0x6B3FBE, 0x000000};
            case "transgender": return new int[] {0xEB92EA, 0xFFFFFF, 0x6AC2E4};
            default: return new int[] {0xAA66FF};
        }
    }

    private static int hsv(float hue, float saturation, float value) {
        float h = (hue - (float) Math.floor(hue)) * 6.0F;
        int i = (int) Math.floor(h);
        float f = h - i;
        float p = value * (1.0F - saturation);
        float q = value * (1.0F - f * saturation);
        float t = value * (1.0F - (1.0F - f) * saturation);
        float r, g, b;
        switch (i % 6) {
            case 0: r = value; g = t; b = p; break;
            case 1: r = q; g = value; b = p; break;
            case 2: r = p; g = value; b = t; break;
            case 3: r = p; g = q; b = value; break;
            case 4: r = t; g = p; b = value; break;
            default: r = value; g = p; b = q; break;
        }
        return ((int) (r * 255.0F) << 16) | ((int) (g * 255.0F) << 8)
            | (int) (b * 255.0F);
    }

    private static double positiveModulo(double value, double modulo) {
        double result = value % modulo;
        return result < 0.0D ? result + modulo : result;
    }
}
