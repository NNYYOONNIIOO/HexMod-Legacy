package at.petra_k.hexcasting.api.casting.iota;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Objects;
import java.util.UUID;

/** A serializable entity reference, retaining a live entity when available. */
public final class EntityIota extends Iota {
    public static final String KEY_UUID_MOST = "uuid_most";
    public static final String KEY_UUID_LEAST = "uuid_least";
    public static final String KEY_NAME = "name";
    public static final IotaType<EntityIota> TYPE = new IotaType<>("entity", data ->
        new EntityIota(null,
            new UUID(data.getLong(KEY_UUID_MOST), data.getLong(KEY_UUID_LEAST)),
            data.getString(KEY_NAME)));

    private final Entity entity;
    private final UUID uuid;
    private final String name;

    public EntityIota(Entity entity) {
        this(Objects.requireNonNull(entity, "entity"),
            entity.getUniqueID(), entity.getName());
    }

    private EntityIota(Entity entity, UUID uuid, String name) {
        super(TYPE);
        this.entity = entity;
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = name == null ? "" : name;
    }

    public Entity getEntity() {
        return entity;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public Entity getPayload() {
        return entity;
    }

    @Override
    public boolean isTruthy() {
        return entity != null;
    }

    @Override
    public String display() {
        return name.isEmpty() ? uuid.toString() : name;
    }

    @Override
    protected void serializePayload(NBTTagCompound data) {
        data.setLong(KEY_UUID_MOST, uuid.getMostSignificantBits());
        data.setLong(KEY_UUID_LEAST, uuid.getLeastSignificantBits());
        data.setString(KEY_NAME, name);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EntityIota && uuid.equals(((EntityIota) other).uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
