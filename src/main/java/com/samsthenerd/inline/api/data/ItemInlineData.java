package com.samsthenerd.inline.api.data;

import com.google.gson.JsonObject;
import com.samsthenerd.inline.api.InlineData;
import com.samsthenerd.inline.api.InlineDataType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** Inline data for a vanilla 1.12.2 ItemStack. */
public final class ItemInlineData implements InlineData<ItemInlineData> {
    public static final ResourceLocation RENDERER_ID =
        new ResourceLocation("inline", "item");
    public static final InlineDataType<ItemInlineData> TYPE =
        new InlineDataType<ItemInlineData>() {
            @Override
            public ResourceLocation getId() {
                return new ResourceLocation("inline", "item");
            }

            @Override
            public JsonObject serialize(ItemInlineData data) {
                JsonObject json = new JsonObject();
                ResourceLocation id = Item.REGISTRY.getNameForObject(data.stack.getItem());
                if (id != null) {
                    json.addProperty("id", id.toString());
                }
                json.addProperty("count", data.stack.getCount());
                json.addProperty("meta", data.stack.getMetadata());
                return json;
            }

            @Override
            public ItemInlineData deserialize(JsonObject json) {
                ResourceLocation id = new ResourceLocation(json.get("id").getAsString());
                Item item = Item.REGISTRY.getObject(id);
                if (item == null) {
                    throw new IllegalArgumentException("Unknown item " + id);
                }
                int count = json.has("count") ? json.get("count").getAsInt() : 1;
                int meta = json.has("meta") ? json.get("meta").getAsInt() : 0;
                return new ItemInlineData(new ItemStack(item, Math.max(1, count), meta));
            }
        };

    private final ItemStack stack;

    public ItemInlineData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("Inline item data needs a non-empty stack");
        }
        this.stack = stack.copy();
    }

    public ItemStack getStack() {
        return stack.copy();
    }

    @Override
    public InlineDataType<ItemInlineData> getType() {
        return TYPE;
    }

    @Override
    public ResourceLocation getRendererId() {
        return RENDERER_ID;
    }

    @Override
    public ItemInlineData copy() {
        return new ItemInlineData(stack);
    }
}
