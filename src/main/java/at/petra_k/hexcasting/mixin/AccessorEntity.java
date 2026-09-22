package at.petra_k.hexcasting.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes vanilla's fall-flying flag setter to the virtual Elytra bridge. */
@Mixin(Entity.class)
public interface AccessorEntity {
    @Invoker("setFlag")
    void hexcasting$setFlag(int flag, boolean state);
}
