package com.samsthenerd.inline.api;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.12.2 port of Inline's data, renderer, and matcher registration boundary.
 * Modern Style/NBT codecs are intentionally replaced by Gson-compatible
 * type serializers and vanilla ITextComponent fallbacks.
 */
public final class InlineAPI {
    private static final Map<ResourceLocation, InlineDataType<?>> DATA_TYPES =
        new LinkedHashMap<>();
    private static final Map<Class<?>, InlineRenderer<?>> RENDERERS =
        new LinkedHashMap<>();
    private static final List<InlineMatcher> CHAT_MATCHERS = new ArrayList<>();

    private InlineAPI() {
    }

    public static synchronized <D extends InlineData<D>> void addDataType(
        InlineDataType<D> type) {
        DATA_TYPES.put(type.getId(), type);
    }

    public static synchronized <D extends InlineData<D>> void registerRenderer(
        Class<D> dataClass, InlineRenderer<D> renderer) {
        RENDERERS.put(dataClass, renderer);
    }

    public static synchronized void addChatMatcher(InlineMatcher matcher) {
        CHAT_MATCHERS.add(matcher);
    }

    public static synchronized boolean hasDataType(ResourceLocation id) {
        return DATA_TYPES.containsKey(id);
    }

    public static synchronized boolean hasRenderer(Class<?> dataClass) {
        return findRenderer(dataClass) != null;
    }

    public static String render(InlineData<?> data) {
        return render(data, new InlineRenderContext(null, false));
    }

    public static String render(InlineData<?> data, InlineRenderContext context) {
        if (data == null) {
            return "";
        }
        InlineRenderer<?> renderer;
        synchronized (InlineAPI.class) {
            renderer = findRenderer(data.getClass());
        }
        if (renderer == null) {
            return data.toString();
        }
        return renderRegistered(renderer, data, context);
    }

    public static ITextComponent asText(InlineData<?> data, MatchContext context) {
        EntityContext entityContext = new EntityContext(context);
        return new TextComponentString(render(data,
            new InlineRenderContext(entityContext.viewer, entityContext.client)));
    }

    /**
     * Rebuilds a vanilla chat component with Inline placeholders rendered as
     * deterministic text. The original component style is retained on the
     * root, while 1.12.2-compatible data stays independent of modern text
     * mixins.
     */
    public static ITextComponent formatChat(ITextComponent message, MatchContext context) {
        if (message == null) {
            return null;
        }
        String text = message.getUnformattedText();
        TextComponentString result = new TextComponentString("");
        result.setStyle(message.getStyle());
        int cursor = 0;
        while (cursor < text.length()) {
            InlineMatch next = findNext(text, cursor, context);
            if (next == null) {
                result.appendSibling(new TextComponentString(text.substring(cursor)));
                break;
            }
            if (next.getStart() > cursor) {
                result.appendSibling(new TextComponentString(
                    text.substring(cursor, next.getStart())));
            }
            result.appendSibling(asText(next.getData(), context));
            cursor = next.getEnd();
        }
        return result;
    }

    /** Formats Inline placeholders for 1.12.2 APIs that accept plain strings. */
    public static String formatPlainText(String text, MatchContext context) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder result = new StringBuilder(text.length());
        int cursor = 0;
        while (cursor < text.length()) {
            InlineMatch next = findNext(text, cursor, context);
            if (next == null) {
                result.append(text.substring(cursor));
                break;
            }
            result.append(text.substring(cursor, next.getStart()));
            EntityContext entityContext = new EntityContext(context);
            result.append(render(next.getData(),
                new InlineRenderContext(entityContext.viewer, entityContext.client)));
            cursor = next.getEnd();
        }
        return result.toString();
    }

    private static InlineMatch findNext(String text, int fromIndex, MatchContext context) {
        InlineMatch best = null;
        synchronized (InlineAPI.class) {
            for (InlineMatcher matcher : CHAT_MATCHERS) {
                InlineMatch candidate = matcher.find(text, fromIndex, context);
                if (candidate != null && (best == null
                    || candidate.getStart() < best.getStart())) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    private static InlineRenderer<?> findRenderer(Class<?> dataClass) {
        InlineRenderer<?> direct = RENDERERS.get(dataClass);
        if (direct != null) {
            return direct;
        }
        for (Map.Entry<Class<?>, InlineRenderer<?>> entry : RENDERERS.entrySet()) {
            if (entry.getKey().isAssignableFrom(dataClass)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String renderRegistered(InlineRenderer<?> renderer,
                                           InlineData<?> data,
                                           InlineRenderContext context) {
        return ((InlineRenderer) renderer).render(data, context);
    }

    private static final class EntityContext {
        private final net.minecraft.entity.player.EntityPlayer viewer;
        private final boolean client;

        private EntityContext(MatchContext context) {
            viewer = context == null ? null : context.getViewer();
            client = context != null && context.getWorld() != null
                && context.getWorld().isRemote;
        }
    }
}
