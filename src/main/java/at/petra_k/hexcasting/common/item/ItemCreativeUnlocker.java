package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.item.MediaHolderItem;
import at.petra_k.hexcasting.api.misc.MediaConstants;
import at.petra_k.hexcasting.common.block.TileEntityImpetus;
import net.minecraft.advancements.Advancement;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/** Creative/debug item with the upstream infinite-media behavior. */
public final class ItemCreativeUnlocker extends ItemFood implements MediaHolderItem {
    private static final String KEY_UNLOCKED = "hexcasting_knowledge_unlocked";
    public static final String DISPLAY_MEDIA = "media";
    public static final String TAG_EXTRACTIONS = "extractions";
    public static final String TAG_INSERTIONS = "insertions";

    public ItemCreativeUnlocker() {
        super(0, 0.0F, false);
        setAlwaysEdible();
        setMaxStackSize(1);
    }

    public static boolean isDebug(ItemStack stack) {
        return isDebug(stack, null);
    }

    /** Debug flags are opt-in words in the item's custom display name. */
    public static boolean isDebug(ItemStack stack, String flag) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemCreativeUnlocker)
            || !stack.hasDisplayName()) {
            return false;
        }
        List<String> words = Arrays.asList(
            stack.getDisplayName().toLowerCase(Locale.ROOT).split(" "));
        if (!words.contains("debug")) {
            return false;
        }
        return flag == null || words.contains(flag);
    }

    @Override
    public long getMaxMedia(ItemStack stack) {
        return Long.MAX_VALUE;
    }

    @Override
    public long getMedia(ItemStack stack) {
        return Long.MAX_VALUE;
    }

    @Override
    public void setMedia(ItemStack stack, long media) {
        // Infinite media is intentionally not stored in NBT.
    }

    @Override
    public boolean canProvide(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canRecharge(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canConstructBattery(ItemStack stack) {
        return false;
    }

    @Override
    public int getConsumptionPriority(ItemStack stack) {
        return 10000;
    }

    @Override
    public long withdrawMedia(ItemStack stack, long amount, boolean simulate) {
        if (!simulate && isDebug(stack, DISPLAY_MEDIA)) {
            addToLongArray(stack, TAG_EXTRACTIONS, amount);
        }
        return amount < 0L ? Long.MAX_VALUE : Math.max(0L, amount);
    }

    @Override
    public long insertMedia(ItemStack stack, long amount, boolean simulate) {
        if (!simulate && isDebug(stack, DISPLAY_MEDIA)) {
            addToLongArray(stack, TAG_INSERTIONS, amount);
        }
        return amount < 0L ? Long.MAX_VALUE : Math.max(0L, amount);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        return super.onItemRightClick(world, player, hand);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
                                      EnumHand hand, EnumFacing facing, float hitX,
                                      float hitY, float hitZ) {
        if (world.getTileEntity(pos) instanceof TileEntityImpetus) {
            ((TileEntityImpetus) world.getTileEntity(pos)).setMedia(-1L);
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.PASS;
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase consumer) {
        if (!world.isRemote && consumer instanceof EntityPlayerMP) {
            consumer.getEntityData().setBoolean(KEY_UNLOCKED, true);
            grantAllAdvancements((EntityPlayerMP) consumer, world);
            consumer.sendMessage(new TextComponentString(
                I18n.translateToLocal("hexcasting.message.creative_unlocker")));
        }

        // The cube is a creative/debug item and is not consumed by eating.
        ItemStack copy = stack.copy();
        super.onItemUseFinish(stack, world, consumer);
        return copy;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.onUpdate(stack, world, entity, slot, selected);
        if (!world.isRemote && isDebug(stack, DISPLAY_MEDIA) && entity instanceof EntityPlayer) {
            debugDisplay(stack, TAG_EXTRACTIONS, "withdrawn", "all_media", (EntityPlayer) entity);
            debugDisplay(stack, TAG_INSERTIONS, "inserted", "infinite_media", (EntityPlayer) entity);
        }
    }

    private static void addToLongArray(ItemStack stack, String key, long value) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        NBTTagList list = tag.hasKey(key, 9) ? tag.getTagList(key, 4) : new NBTTagList();
        list.appendTag(new NBTTagLong(value));
        tag.setTag(key, list);
    }

    private static void debugDisplay(ItemStack stack, String key, String action,
                                     String amountKey, EntityPlayer player) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(key, 9)) {
            return;
        }
        NBTTagList list = tag.getTagList(key, 4);
        tag.removeTag(key);
        for (int index = 0; index < list.tagCount(); index++) {
            if (!(list.get(index) instanceof NBTTagLong)) {
                continue;
            }
            long value = ((NBTTagLong) list.get(index)).getLong();
            if (value < 0L) {
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocalFormatted(
                        "hexcasting.debug.media_" + action,
                        stack.getDisplayName(),
                        TextFormatting.GRAY + I18n.translateToLocal(
                            "hexcasting.debug." + amountKey))));
            } else {
                String dust = String.format(Locale.ROOT, "%.2f",
                    value / (double) MediaConstants.DUST_UNIT);
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocalFormatted(
                        "hexcasting.debug.media_" + action + ".with_dust",
                        stack.getDisplayName(), value, dust)));
            }
        }
    }

    private static void grantAllAdvancements(EntityPlayerMP player, World world) {
        if (!(world instanceof WorldServer)
            || ((WorldServer) world).getMinecraftServer() == null) {
            return;
        }
        Advancement root = ((WorldServer) world).getMinecraftServer()
            .getAdvancementManager().getAdvancement(
                new ResourceLocation("hexcasting", "root"));
        if (root == null) {
            return;
        }

        Deque<Advancement> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            Advancement advancement = pending.removeFirst();
            net.minecraft.advancements.AdvancementProgress progress =
                player.getAdvancements().getProgress(advancement);
            for (String criterion : progress.getRemaningCriteria()) {
                player.getAdvancements().grantCriterion(advancement, criterion);
            }
            for (Advancement child : advancement.getChildren()) {
                pending.addLast(child);
            }
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        tooltip.add(TextFormatting.LIGHT_PURPLE
            + I18n.translateToLocal("item.hexcasting.creative_unlocker.for_emphasis"));
        tooltip.add(I18n.translateToLocalFormatted(
            "item.hexcasting.creative_unlocker.tooltip",
            I18n.translateToLocal("item.hexcasting.creative_unlocker.mod_name")));
    }
}
