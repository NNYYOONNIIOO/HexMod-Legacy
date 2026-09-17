package at.petra_k.hexcasting.common.misc;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.common.lib.hex.BrainsweepRecipes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.EnumActionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Forge 1.12 interaction hooks for entities whose free will was removed. */
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID)
public final class BrainsweepingEvents {
    private BrainsweepingEvents() {
    }

    /** Consume ordinary right-click interactions before a mob handles them. */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        consumeIfBrainswept(event, event.getTarget());
    }

    /** Also consume precise hit interactions, which fire before EntityInteract. */
    @SubscribeEvent
    public static void onEntityInteractSpecific(
        PlayerInteractEvent.EntityInteractSpecific event) {
        consumeIfBrainswept(event, event.getTarget());
    }

    private static void consumeIfBrainswept(PlayerInteractEvent event, Entity target) {
        if (target instanceof EntityLiving
            && BrainsweepRecipes.isBrainswept((EntityLiving) target)) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
        }
    }

    /** Copy the marker when another 1.12.2 system replaces a living entity. */
    public static void copyMarker(EntityLiving original, EntityLiving outcome) {
        if (BrainsweepRecipes.isBrainswept(original)) {
            BrainsweepRecipes.markBrainswept(outcome);
        }
    }
}
