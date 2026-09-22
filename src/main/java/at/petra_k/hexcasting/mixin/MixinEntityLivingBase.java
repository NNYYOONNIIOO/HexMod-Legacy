package at.petra_k.hexcasting.mixin;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps Altiora's virtual elytra active through vanilla's per-tick check. */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {
    @Inject(
        method = "updateElytra",
        at = @At("HEAD"),
        cancellable = true
    )
    private void hexcasting$updateVirtualElytra(CallbackInfo info) {
        EntityLivingBase entity = (EntityLivingBase) (Object) this;
        if (!hasAltiora(entity)) {
            return;
        }

        ItemStack chest = entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        if (chest != null && !chest.isEmpty() && chest.getItem() == Items.ELYTRA) {
            // A real Elytra still uses vanilla durability and flight checks.
            return;
        }

        // EntityLivingBase.updateElytra would otherwise clear flag 7 because
        // there is no physical Elytra in the chest slot.  Keep exactly the
        // same airborne/riding conditions as vanilla and skip that check for
        // this virtual ability on both logical sides.
        ((at.petra_k.hexcasting.mixin.AccessorEntity) (Object) entity)
            .hexcasting$setFlag(7, !entity.onGround && !entity.isRiding());
        info.cancel();
    }

    private static boolean hasAltiora(EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)
            || HexCapabilities.CASTING_DATA == null) {
            return false;
        }
        IHexCastingData data = entity.getCapability(
            HexCapabilities.CASTING_DATA, null);
        return data != null && data.isAltioraActive();
    }
}
