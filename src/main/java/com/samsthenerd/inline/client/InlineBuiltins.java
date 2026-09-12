package com.samsthenerd.inline.client;

import com.samsthenerd.inline.api.InlineAPI;
import com.samsthenerd.inline.api.InlineData;
import com.samsthenerd.inline.api.InlineRenderContext;
import com.samsthenerd.inline.api.MatchContext;
import com.samsthenerd.inline.api.data.EntityInlineData;
import com.samsthenerd.inline.api.data.ItemInlineData;
import com.samsthenerd.inline.api.data.ModIconData;
import com.samsthenerd.inline.api.data.PlayerHeadData;
import com.samsthenerd.inline.matching.RegexInlineMatcher;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.resources.I18n;

import java.util.UUID;
import java.util.regex.Pattern;

/** Registers the player-facing matchers shipped by the Inline port. */
public final class InlineBuiltins {
    private static boolean registered;

    private InlineBuiltins() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        InlineAPI.addDataType(ItemInlineData.TYPE);
        InlineAPI.addDataType(EntityInlineData.TYPE);
        InlineAPI.addDataType(PlayerHeadData.TYPE);
        InlineAPI.addDataType(ModIconData.TYPE);
        InlineAPI.registerRenderer(ItemInlineData.class,
            (data, context) -> renderItem(data));
        InlineAPI.registerRenderer(EntityInlineData.class,
            (data, context) -> renderEntity(data));
        InlineAPI.registerRenderer(PlayerHeadData.class,
            (data, context) -> renderPlayer(data));
        InlineAPI.registerRenderer(ModIconData.class,
            (data, context) -> renderMod(data));

        InlineAPI.addChatMatcher(new RegexInlineMatcher(
            Pattern.compile("\\[item:([^\\]]+)\\]"),
            (matcher, context) -> item(matcher.group(1))));
        InlineAPI.addChatMatcher(new RegexInlineMatcher(
            Pattern.compile("\\[entity:([^\\]]+)\\]"),
            (matcher, context) -> entity(matcher.group(1))));
        InlineAPI.addChatMatcher(new RegexInlineMatcher(
            Pattern.compile("\\[face:([^\\]]+)\\]"),
            (matcher, context) -> new PlayerHeadData(
                new com.mojang.authlib.GameProfile(
                    UUID.nameUUIDFromBytes(matcher.group(1).getBytes()), matcher.group(1)))));
        InlineAPI.addChatMatcher(new RegexInlineMatcher(
            Pattern.compile("\\[mod:([^\\]]+)\\]"),
            (matcher, context) -> new ModIconData(matcher.group(1))));
        InlineAPI.addChatMatcher(new RegexInlineMatcher(
            Pattern.compile("\\[show:(hand|offhand)\\]"),
            InlineBuiltins::heldItem));
        registered = true;
    }

    private static InlineData<?> heldItem(java.util.regex.Matcher matcher, MatchContext context) {
        if (context == null || context.getViewer() == null) {
            return null;
        }
        net.minecraft.util.EnumHand hand = "offhand".equals(matcher.group(1))
            ? net.minecraft.util.EnumHand.OFF_HAND : net.minecraft.util.EnumHand.MAIN_HAND;
        ItemStack stack = context.getViewer().getHeldItem(hand);
        return stack.isEmpty() ? null : new ItemInlineData(stack);
    }

    private static ItemInlineData item(String value) {
        try {
            ResourceLocation id = new ResourceLocation(value);
            Item item = Item.REGISTRY.getObject(id);
            return item == null ? null : new ItemInlineData(new ItemStack(item));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static EntityInlineData entity(String value) {
        try {
            return new EntityInlineData(new ResourceLocation(value));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String renderItem(ItemInlineData data) {
        ResourceLocation id = Item.REGISTRY.getNameForObject(data.getStack().getItem());
        return "[item:" + (id == null ? "unknown" : id.toString()) + "]";
    }

    private static String renderEntity(EntityInlineData data) {
        return data.getEntity() == null
            ? "[entity:" + data.getTypeId() + "]"
            : "[entity:" + data.getEntity().getName() + "]";
    }

    private static String renderPlayer(PlayerHeadData data) {
        return "[face:" + data.getProfile().getName() + "]";
    }

    private static String renderMod(ModIconData data) {
        String name = I18n.format("mod." + data.getModId() + ".name");
        return "[" + (name.equals("mod." + data.getModId() + ".name")
            ? data.getModId() : name) + "]";
    }
}
