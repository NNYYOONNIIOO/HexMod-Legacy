package at.petra_k.hexcasting.common.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;

/** Client-side particle fallback for Hex's invisible conjured block. */
public final class TileEntityConjured extends TileEntity implements ITickable {
    @Override
    public void update() {
        if (world == null || !world.isRemote || world.rand.nextInt(5) != 0) {
            return;
        }
        world.spawnParticle(
            EnumParticleTypes.SPELL_WITCH,
            pos.getX() + 0.15D + world.rand.nextDouble() * 0.70D,
            pos.getY() + 0.15D + world.rand.nextDouble() * 0.70D,
            pos.getZ() + 0.15D + world.rand.nextDouble() * 0.70D,
            0.0D, 0.0D, 0.0D);
    }
}
