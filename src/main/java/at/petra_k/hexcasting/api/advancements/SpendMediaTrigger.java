package at.petra_k.hexcasting.api.advancements;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

/** Criterion fired after a successful media extraction. */
public final class SpendMediaTrigger
    extends SimpleHexCriterionTrigger<SpendMediaTrigger.Instance> {
    private static final ResourceLocation ID =
        new ResourceLocation("hexcasting", "spend_media");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public Instance deserializeInstance(JsonObject json, JsonDeserializationContext context) {
        return new Instance(HexAdvancementBounds.read(json.get("media_spent")),
            HexAdvancementBounds.read(json.get("media_wasted")));
    }

    public void trigger(EntityPlayerMP player, long mediaSpent, long mediaWasted) {
        super.trigger(player, instance -> instance.matches(mediaSpent, mediaWasted));
    }

    public static final class Instance extends AbstractCriterionInstance {
        private final HexAdvancementBounds mediaSpent;
        private final HexAdvancementBounds mediaWasted;

        private Instance(HexAdvancementBounds mediaSpent,
                         HexAdvancementBounds mediaWasted) {
            super(ID);
            this.mediaSpent = mediaSpent;
            this.mediaWasted = mediaWasted;
        }

        private boolean matches(long spent, long wasted) {
            return mediaSpent.matches(spent) && mediaWasted.matches(wasted);
        }
    }
}
