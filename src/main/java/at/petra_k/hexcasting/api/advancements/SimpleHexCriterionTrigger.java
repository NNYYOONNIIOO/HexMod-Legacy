package at.petra_k.hexcasting.api.advancements;

import net.minecraft.advancements.ICriterionInstance;
import net.minecraft.advancements.ICriterionTrigger;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Small 1.12.2 equivalent of modern SimpleCriterionTrigger. */
abstract class SimpleHexCriterionTrigger<T extends ICriterionInstance>
    implements ICriterionTrigger<T> {
    private final Map<PlayerAdvancements, Set<Listener<T>>> listeners = new HashMap<>();

    @Override
    public final void addListener(PlayerAdvancements advancements, Listener<T> listener) {
        listeners.computeIfAbsent(advancements, ignored -> new HashSet<>()).add(listener);
    }

    @Override
    public final void removeListener(PlayerAdvancements advancements, Listener<T> listener) {
        Set<Listener<T>> current = listeners.get(advancements);
        if (current == null) {
            return;
        }
        current.remove(listener);
        if (current.isEmpty()) {
            listeners.remove(advancements);
        }
    }

    @Override
    public final void removeAllListeners(PlayerAdvancements advancements) {
        listeners.remove(advancements);
    }

    protected final void trigger(EntityPlayerMP player, Predicate<T> predicate) {
        if (player == null || player.getAdvancements() == null || predicate == null) {
            return;
        }
        Set<Listener<T>> current = listeners.get(player.getAdvancements());
        if (current == null || current.isEmpty()) {
            return;
        }
        ArrayList<Listener<T>> matched = new ArrayList<>();
        for (Listener<T> listener : current) {
            if (predicate.test(listener.getCriterionInstance())) {
                matched.add(listener);
            }
        }
        for (Listener<T> listener : matched) {
            listener.grantCriterion(player.getAdvancements());
        }
    }
}
