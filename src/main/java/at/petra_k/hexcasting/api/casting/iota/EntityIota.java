package at.petra_k.hexcasting.api.casting.iota;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.translation.I18n;

import java.util.Objects;
import java.util.UUID;

/** A serializable entity reference, retaining a live entity when available. */
public final class EntityIota extends Iota {
    public static final String KEY_UUID_MOST = "uuid_most";
    public static final String KEY_UUID_LEAST = "uuid_least";
    public static final String KEY_NAME = "name";
    public static final String KEY_TRANSLATION_KEY = "translation_key";
    public static final IotaType<EntityIota> TYPE = new IotaType<>("entity", data ->
        new EntityIota(null,
            new UUID(data.getLong(KEY_UUID_MOST), data.getLong(KEY_UUID_LEAST)),
            data.getString(KEY_NAME), data.getString(KEY_TRANSLATION_KEY)));

    private final Entity entity;
    private final UUID uuid;
    private final String name;
    private final String translationKey;

    public EntityIota(Entity entity) {
        this(Objects.requireNonNull(entity, "entity"),
            entity.getUniqueID(), entity.getName(), translationKey(entity));
    }

    private EntityIota(Entity entity, UUID uuid, String name,
                       String translationKey) {
        super(TYPE);
        this.entity = entity;
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = name == null ? "" : name;
        this.translationKey = translationKey == null ? "" : translationKey;
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
        String localized = entity == null ? "" : localizedName(entity);
        if (!localized.isEmpty() && !isRawTranslation(localized)) {
            return localized;
        }

        if (!translationKey.isEmpty()) {
            String translated = I18n.translateToLocal(translationKey);
            if (!translated.equals(translationKey)) {
                return translated;
            }
        }
        return name.isEmpty() ? uuid.toString() : name;
    }

    @Override
    protected void serializePayload(NBTTagCompound data) {
        data.setLong(KEY_UUID_MOST, uuid.getMostSignificantBits());
        data.setLong(KEY_UUID_LEAST, uuid.getLeastSignificantBits());
        data.setString(KEY_NAME, name);
        data.setString(KEY_TRANSLATION_KEY, translationKey);
    }

    /**
     * Resolve the name on demand so item entities follow the client's current
     * language instead of keeping the server-side or creation-time text.
     */
    private static String localizedName(Entity entity) {
        if (entity instanceof EntityItem) {
            ItemStack stack = ((EntityItem) entity).getItem();
            return stack == null || stack.isEmpty() ? "" : stack.getDisplayName();
        }
        return entity.getDisplayName() == null
            ? "" : entity.getDisplayName().getUnformattedText();
    }

    /** Return the translation key used when the live entity is unavailable. */
    private static String translationKey(Entity entity) {
        if (entity.hasCustomName()) {
            return "";
        }
        if (entity instanceof EntityItem) {
            ItemStack stack = ((EntityItem) entity).getItem();
            return stack == null || stack.isEmpty()
                ? "entity.minecraft.item.name"
                : stack.getItem().getUnlocalizedName(stack) + ".name";
        }
        String entityId = EntityList.getEntityString(entity);
        return entityId == null || entityId.isEmpty()
            ? "" : "entity." + entityId + ".name";
    }

    private boolean isRawTranslation(String value) {
        return !translationKey.isEmpty() && value.equals(translationKey);
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
