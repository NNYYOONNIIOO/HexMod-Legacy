package at.petra_k.hexcasting.common.lib;

import at.petra_k.hexcasting.api.misc.MediaConstants;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.stats.IStatType;
import net.minecraft.stats.StatBase;
import net.minecraft.util.text.TextComponentTranslation;

/** Custom statistics shared by the 1.12.2 casting paths. */
public final class HexStatistics {
    private static final IStatType MEDIA_IN_DUST = new IStatType() {
        @Override
        public String format(int value) {
            return Integer.toString(Math.max(0, value) / (int) MediaConstants.DUST_UNIT);
        }
    };

    public static final StatBase MEDIA_USED = make("media_used", MEDIA_IN_DUST);
    public static final StatBase MEDIA_OVERCAST = make("media_overcast", MEDIA_IN_DUST);
    public static final StatBase PATTERNS_DRAWN = make("patterns_drawn", StatBase.simpleStatType);
    public static final StatBase SPELLS_CAST = make("spells_cast", StatBase.simpleStatType);

    private HexStatistics() {
    }

    public static void register() {
        // Class initialization registers the StatBase instances.
    }

    public static void award(EntityPlayer player, StatBase stat, long amount) {
        if (player == null || stat == null || player.world == null
            || player.world.isRemote || amount <= 0L) {
            return;
        }
        int value = amount >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount;
        player.addStat(stat, value);
    }

    private static StatBase make(String key, IStatType type) {
        return new StatBase("hexcasting." + key,
            new TextComponentTranslation("stat.hexcasting." + key), type)
            .initIndependentStat().registerStat();
    }
}
