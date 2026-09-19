package at.petra_k.hexcasting.mixin;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes the protected vanilla death sound for Brainsweep feedback. */
@Mixin(EntityLivingBase.class)
public interface AccessorEntityLivingBase {
    @Invoker("getDeathSound")
    SoundEvent hexcasting$getDeathSound();
}
