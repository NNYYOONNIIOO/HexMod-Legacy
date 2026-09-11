package at.petra_k.hexcasting.api.misc;

/** Media conversion constants shared by actions and holders. */
public final class MediaConstants {
    public static final long DUST_UNIT = 10000L;
    public static final long SHARD_UNIT = 5L * DUST_UNIT;
    public static final long CRYSTAL_UNIT = 10L * DUST_UNIT;
    public static final long QUENCHED_SHARD_UNIT = 3L * CRYSTAL_UNIT;
    public static final long QUENCHED_BLOCK_UNIT = 4L * QUENCHED_SHARD_UNIT;
    public static final long DEFAULT_PLAYER_MAX_MEDIA = 100L * CRYSTAL_UNIT;

    private MediaConstants() {
    }
}

