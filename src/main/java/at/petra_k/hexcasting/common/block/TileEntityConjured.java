package at.petra_k.hexcasting.common.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;

/** Client-side particle fallback for Hex's invisible conjured block. */
public final class TileEntityConjured extends TileEntity implements ITickable {
    private static final String KEY_REMAINING_TICKS = "remaining_ticks";
    private static final int DEFAULT_LIFETIME = 200;
    private int remainingTicks = DEFAULT_LIFETIME;

    public void setLifetime(int ticks) {
        remainingTicks = Math.max(1, ticks);
        markDirty();
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    @Override
    public net.minecraft.nbt.NBTTagCompound writeToNBT(net.minecraft.nbt.NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger(KEY_REMAINING_TICKS, remainingTicks);
        return compound;
    }

    @Override
    public void readFromNBT(net.minecraft.nbt.NBTTagCompound compound) {
        super.readFromNBT(compound);
        remainingTicks = compound.hasKey(KEY_REMAINING_TICKS, 3)
            ? Math.max(1, compound.getInteger(KEY_REMAINING_TICKS)) : DEFAULT_LIFETIME;
    }

    @Override
    public void update() {
        if (world == null) {
            return;
        }
        if (!world.isRemote) {
            if (remainingTicks-- <= 0) {
                world.setBlockToAir(pos);
            } else if ((remainingTicks & 15) == 0) {
                markDirty();
            }
            return;
        }
        particleEffect();
    }

    /** Client-side particle hook used by both the tile tick and block callback. */
    public void particleEffect() {
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

    /** Client-side footstep hook retained for parity with modern Hex. */
    public void walkParticle(net.minecraft.entity.Entity entity) {
        if (world == null || !world.isRemote || entity == null) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            world.spawnParticle(
                EnumParticleTypes.SPELL_WITCH,
                entity.posX + (world.rand.nextDouble() - 0.5D) * 0.6D,
                entity.posY + 0.05D + world.rand.nextDouble() * 0.2D,
                entity.posZ + (world.rand.nextDouble() - 0.5D) * 0.6D,
                0.0D, 0.0D, 0.0D);
        }
    }
}
