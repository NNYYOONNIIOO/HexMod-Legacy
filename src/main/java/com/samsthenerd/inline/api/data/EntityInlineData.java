package com.samsthenerd.inline.api.data;

import com.samsthenerd.inline.api.InlineData;
import com.samsthenerd.inline.api.InlineDataType;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/** Inline data for an entity instance or an entity type identifier. */
public final class EntityInlineData implements InlineData<EntityInlineData> {
    public static final ResourceLocation RENDERER_ID =
        new ResourceLocation("inline", "entity");
    public static final InlineDataType<EntityInlineData> TYPE =
        new InlineDataType<EntityInlineData>() {
            @Override
            public ResourceLocation getId() {
                return new ResourceLocation("inline", "entity");
            }
        };

    private final Entity entity;
    private final ResourceLocation typeId;

    public EntityInlineData(Entity entity) {
        this.entity = entity;
        this.typeId = new ResourceLocation("minecraft", "entity");
    }

    public EntityInlineData(ResourceLocation typeId) {
        this.entity = null;
        this.typeId = typeId;
    }

    public Entity getEntity() {
        return entity;
    }

    public ResourceLocation getTypeId() {
        return typeId;
    }

    @Override
    public InlineDataType<EntityInlineData> getType() {
        return TYPE;
    }

    @Override
    public ResourceLocation getRendererId() {
        return RENDERER_ID;
    }

    @Override
    public EntityInlineData copy() {
        return entity == null ? new EntityInlineData(typeId) : new EntityInlineData(entity);
    }
}
