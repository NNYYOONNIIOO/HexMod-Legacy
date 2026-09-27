package at.petra_k.hexcasting.mixin;

import net.minecraft.entity.projectile.EntityArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accesses vanilla's private arrow-in-ground flag for velocity queries. */
@Mixin(EntityArrow.class)
public interface AccessorEntityArrow {
    @Accessor("inGround")
    boolean hexcasting$isInGround();
}
