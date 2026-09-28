package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.common.config.HexConfig;
import at.petra_k.hexcasting.common.effect.HexPigmentSource;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Client-only view of pigments, including the optional pride colour filter. */
@SideOnly(Side.CLIENT)
public final class HexClientPigment {
    private HexClientPigment() {
    }

    public static boolean isPrideVariant(String variant) {
        return variant != null && variant.startsWith("pride_colorizer_");
    }

    public static boolean hides(String variant) {
        return HexConfig.hidePrideColors() && isPrideVariant(variant);
    }

    public static boolean hides(HexPigmentSource source) {
        return source != null && hides(source.getVariant());
    }

    /** Replace only the client-side view; the source's server NBT is untouched. */
    public static HexPigmentSource normalize(HexPigmentSource source) {
        return hides(source) ? HexPigmentSource.defaultSource() : source;
    }

    public static int defaultColor(float time, double x, double y, double z) {
        return HexPigmentSource.defaultSource().sample(time, x, y, z);
    }
}
