package at.petra_k.hexcasting.api.advancements;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

/** Criterion fired when player health is converted into media. */
public final class OvercastTrigger
    extends SimpleHexCriterionTrigger<OvercastTrigger.Instance> {
    private static final ResourceLocation ID =
        new ResourceLocation("hexcasting", "overcast");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public Instance deserializeInstance(JsonObject json, JsonDeserializationContext context) {
        return new Instance(HexAdvancementBounds.read(json.get("media_generated")),
            HexAdvancementBounds.read(json.get("health_used")),
            HexAdvancementBounds.read(json.get(
                "mojang_i_am_begging_and_crying_please_add_an_entity_health_criterion")));
    }

    public void trigger(EntityPlayerMP player, long mediaGenerated,
                        double healthUsed, double healthLeft) {
        super.trigger(player, instance -> instance.matches(
            mediaGenerated, healthUsed, healthLeft));
    }

    public static final class Instance extends AbstractCriterionInstance {
        private final HexAdvancementBounds mediaGenerated;
        private final HexAdvancementBounds healthUsed;
        private final HexAdvancementBounds healthLeft;

        private Instance(HexAdvancementBounds mediaGenerated,
                         HexAdvancementBounds healthUsed,
                         HexAdvancementBounds healthLeft) {
            super(ID);
            this.mediaGenerated = mediaGenerated;
            this.healthUsed = healthUsed;
            this.healthLeft = healthLeft;
        }

        private boolean matches(long generated, double used, double left) {
            return mediaGenerated.matches(generated)
                && healthUsed.matches(used) && healthLeft.matches(left);
        }
    }
}
