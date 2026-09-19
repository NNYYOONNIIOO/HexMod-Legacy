package at.petra_k.hexcasting.common.entity;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.item.ItemPatternScroll;
import at.petra_k.hexcasting.common.lib.HexItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/** A hanging, persistent display for a pattern scroll. */
public final class EntityWallScroll extends EntityHanging
    implements IEntityAdditionalSpawnData {
    private static final net.minecraft.network.datasync.DataParameter<Boolean>
        SHOWS_STROKE_ORDER = net.minecraft.network.datasync.EntityDataManager.createKey(
            EntityWallScroll.class,
            net.minecraft.network.datasync.DataSerializers.BOOLEAN);

    public static final int MIN_BLOCK_SIZE = 1;
    public static final int MAX_BLOCK_SIZE = 3;

    private ItemStack scroll = ItemStack.EMPTY;
    private HexPattern pattern;
    private boolean ancient;
    private int blockSize = 1;

    public EntityWallScroll(World world) {
        super(world);
    }

    public EntityWallScroll(World world, BlockPos position, EnumFacing facing,
                            ItemStack scroll, boolean showsStrokeOrder,
                            int blockSize) {
        super(world, position);
        this.blockSize = clampBlockSize(blockSize);
        this.scroll = scroll == null ? ItemStack.EMPTY : scroll.copy();
        this.scroll.setCount(Math.min(1, this.scroll.getCount()));
        this.dataManager.set(SHOWS_STROKE_ORDER, showsStrokeOrder);
        this.updateFacingWithBoundingBox(facing);
        recalculateDisplay();
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(SHOWS_STROKE_ORDER, false);
    }

    public void recalculateDisplay() {
        NBTTagCompound tag = scroll == null ? null : scroll.getTagCompound();
        if (tag == null || !tag.hasKey(ItemPatternScroll.TAG_PATTERN, 10)) {
            pattern = null;
            ancient = false;
            return;
        }
        try {
            pattern = HexPattern.fromNBT(
                tag.getCompoundTag(ItemPatternScroll.TAG_PATTERN));
            ancient = tag.hasKey(ItemPatternScroll.TAG_OP_ID, 8);
        } catch (RuntimeException ignored) {
            pattern = null;
            ancient = false;
        }
    }

    public boolean getShowsStrokeOrder() {
        return dataManager.get(SHOWS_STROKE_ORDER);
    }

    public void setShowsStrokeOrder(boolean value) {
        dataManager.set(SHOWS_STROKE_ORDER, value);
    }

    public HexPattern getPattern() {
        return pattern;
    }

    public boolean isAncient() {
        return ancient;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public ItemStack getScroll() {
        return scroll == null ? ItemStack.EMPTY : scroll;
    }

    @Override
    public int getWidthPixels() {
        return 16 * blockSize;
    }

    @Override
    public int getHeightPixels() {
        return 16 * blockSize;
    }

    @Override
    public void onBroken(@Nullable Entity brokenEntity) {
        if (!world.getGameRules().getBoolean("doEntityDrops")) {
            return;
        }
        playSound(SoundEvents.ENTITY_PAINTING_BREAK, 1.0F, 1.0F);
        if (brokenEntity instanceof EntityPlayer
            && ((EntityPlayer) brokenEntity).capabilities.isCreativeMode) {
            return;
        }
        if (scroll != null && !scroll.isEmpty()) {
            entityDropItem(scroll.copy(), 0.0F);
        }
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        Item dust = HexItems.EXTRA_ITEMS.get("amethyst_dust");
        if (!getShowsStrokeOrder() && dust != null && held.getItem() == dust) {
            if (!player.capabilities.isCreativeMode) {
                held.shrink(1);
            }
            setShowsStrokeOrder(true);
            return true;
        }
        return super.processInitialInteract(player, hand);
    }

    @Override
    public void playPlaceSound() {
        playSound(SoundEvents.ENTITY_PAINTING_PLACE, 1.0F, 1.0F);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        compound.setTag("Scroll", scroll == null
            ? new NBTTagCompound() : scroll.writeToNBT(new NBTTagCompound()));
        compound.setBoolean("ShowsStrokeOrder", getShowsStrokeOrder());
        compound.setInteger("BlockSize", blockSize);
        super.writeEntityToNBT(compound);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        NBTTagCompound scrollTag = compound.getCompoundTag("Scroll");
        scroll = scrollTag == null || scrollTag.hasNoTags()
            ? ItemStack.EMPTY : new ItemStack(scrollTag);
        blockSize = clampBlockSize(compound.getInteger("BlockSize"));
        setShowsStrokeOrder(compound.getBoolean("ShowsStrokeOrder"));
        super.readEntityFromNBT(compound);
        recalculateDisplay();
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        BlockPos position = getHangingPosition();
        buffer.writeInt(position == null ? 0 : position.getX());
        buffer.writeInt(position == null ? 0 : position.getY());
        buffer.writeInt(position == null ? 0 : position.getZ());
        buffer.writeByte(facingDirection == null
            ? EnumFacing.NORTH.getHorizontalIndex() : facingDirection.getHorizontalIndex());
        buffer.writeByte(blockSize);
        buffer.writeBoolean(getShowsStrokeOrder());
        ByteBufUtils.writeItemStack(buffer,
            scroll == null ? ItemStack.EMPTY : scroll);
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        hangingPosition = new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
        EnumFacing facing = EnumFacing.fromAngle(
            (buffer.readUnsignedByte() & 3) * 90.0D);
        blockSize = clampBlockSize(buffer.readByte());
        boolean showsStrokeOrder = buffer.readBoolean();
        scroll = ByteBufUtils.readItemStack(buffer);
        setShowsStrokeOrder(showsStrokeOrder);
        updateFacingWithBoundingBox(facing);
        recalculateDisplay();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double x, double y, double z,
                                              float yaw, float pitch,
                                              int increments, boolean teleport) {
        if (hangingPosition == null) {
            setPosition(x, y, z);
            return;
        }
        BlockPos position = hangingPosition.add(
            (int) (x - posX), (int) (y - posY), (int) (z - posZ));
        setPosition(position.getX(), position.getY(), position.getZ());
    }

    @Override
    public ItemStack getPickedResult(RayTraceResult target) {
        return getScroll().copy();
    }

    public static int clampBlockSize(int value) {
        return Math.max(MIN_BLOCK_SIZE, Math.min(MAX_BLOCK_SIZE,
            value <= 0 ? MIN_BLOCK_SIZE : value));
    }
}
