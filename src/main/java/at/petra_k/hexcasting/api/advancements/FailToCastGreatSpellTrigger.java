package at.petra_k.hexcasting.api.advancements;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

/** Criterion fired when a world-specific great spell fails. */
public final class FailToCastGreatSpellTrigger
    extends SimpleHexCriterionTrigger<FailToCastGreatSpellTrigger.Instance> {
    private static final ResourceLocation ID =
        new ResourceLocation("hexcasting", "fail_to_cast_great_spell");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public Instance deserializeInstance(JsonObject json, JsonDeserializationContext context) {
        return new Instance();
    }

    public void trigger(EntityPlayerMP player) {
        super.trigger(player, ignored -> true);
    }

    public static final class Instance extends AbstractCriterionInstance {
        private Instance() {
            super(ID);
        }
    }
}
