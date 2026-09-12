package com.samsthenerd.inline.common;

import com.samsthenerd.inline.api.InlineAPI;
import com.samsthenerd.inline.api.InlineData;
import com.samsthenerd.inline.api.MatchContext;
import com.samsthenerd.inline.api.data.EntityInlineData;
import com.samsthenerd.inline.api.data.ItemInlineData;
import com.samsthenerd.inline.api.data.ModIconData;
import com.samsthenerd.inline.api.data.PlayerHeadData;
import com.samsthenerd.inline.matching.RegexInlineMatcher;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;

import java.util.regex.Pattern;

/** Common-side Inline registrations that do not load Minecraft client classes. */
public final class InlineCommonBuiltins {
    private static boolean registered;

    private InlineCommonBuiltins() {
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
            (data, context) -> "[mod:" + data.getModId() + "]");

        InlineAPI.addChatMatcher(new RegexInlineMatcher(
            Pattern.compile("\\[item:([^\\]]+)\\]"),
            (matcher, context) -> item(matcher.group(1))));
        InlineAPI.addChatMatcher(new RegexInlineMatcher(
            Pattern.compile("\\[entity:([^\\]]+)\\]"),
            (matcher, context) -> entity(matcher.group(1))));
        InlineAPI.addChatMatcher(new RegexInlineMatcher(
            Pattern.compile("\\[face:([^\\]]+)\\]"),
            (matcher, context) -> new PlayerHeadData(matcher.group(1))));
        InlineAPI.addChatMatcher(new RegexInlineMatcher(
            Pattern.compile("\\[mod:([^\\]]+)\\]"),
            (matcher, context) -> new ModIconData(matcher.group(1))));
        InlineAPI.addChatMatcher(new RegexInlineMatcher(
            Pattern.compile("\\[show:(hand|offhand)\\]"),
            InlineCommonBuiltins::heldItem));
        registered = true;
    }

    private static InlineData<?> heldItem(java.util.regex.Matcher matcher, MatchContext context) {
        if (context == null || context.getViewer() == null) {
            return null;
        }
        EnumHand hand = "offhand".equals(matcher.group(1))
            ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
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
        return "[entity:" + data.getDisplayName() + "]";
    }

    private static String renderPlayer(PlayerHeadData data) {
        return "[face:" + data.getProfile().getName() + "]";
    }
}
