package at.petra_k.hexcasting.common.command;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.item.ItemPatternScroll;
import at.petra_k.hexcasting.common.lib.hex.BrainsweepRecipes;
import at.petra_k.hexcasting.common.lib.HexItems;
import at.petra_k.hexcasting.common.network.MsgPerWorldPatternsS2C;
import at.petra_k.hexcasting.common.world.PerWorldPatternData;
import at.petrak.paucal.api.PaucalAPI;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 1.12.2 command facade for Hex's brainsweep and pattern diagnostics. */
public final class HexCommand extends CommandBase {
    private static final String NAME = "hexcasting";
    private static final String[] SUBCOMMANDS = new String[] {
        "brainsweep", "perWorldPatterns", "recalcPatterns",
        "textureToggle", "textureRepaint"
    };
    private static boolean textureDebugEnabled;
    private static long textureRepaintGeneration;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/hexcasting <brainsweep|perWorldPatterns|recalcPatterns|textureToggle|textureRepaint>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
        throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException("hexcasting.command.usage.root");
        }
        if ("brainsweep".equals(args[0])) {
            executeBrainsweep(server, sender, args);
        } else if ("perWorldPatterns".equals(args[0])) {
            executePerWorldPatterns(server, sender, args);
        } else if ("recalcPatterns".equals(args[0])) {
            requireLength(args, 1);
            WorldServer overworld = server.getWorld(0);
            PerWorldPatternData.recalculate(overworld);
            PaucalAPI.sendToAll(new MsgPerWorldPatternsS2C(overworld));
            sender.sendMessage(new TextComponentTranslation(
                "hexcasting.command.patterns.recalculated", overworld.getSeed()));
        } else if ("textureToggle".equals(args[0])) {
            requireLength(args, 1);
            textureDebugEnabled = !textureDebugEnabled;
            sender.sendMessage(new TextComponentTranslation(
                "hexcasting.command.patterns.texture_toggle",
                new TextComponentTranslation(textureDebugEnabled
                    ? "hexcasting.command.enabled"
                    : "hexcasting.command.disabled")));
        } else if ("textureRepaint".equals(args[0])) {
            requireLength(args, 1);
            textureRepaintGeneration++;
            sender.sendMessage(new TextComponentTranslation(
                "hexcasting.command.patterns.texture_repaint",
                textureRepaintGeneration));
        } else {
            throw new CommandException(
                "hexcasting.command.error.unknown_subcommand", args[0]);
        }
    }

    private static void executeBrainsweep(MinecraftServer server,
                                          ICommandSender sender, String[] args)
        throws CommandException {
        if (args.length != 2) {
            throw new WrongUsageException(
                "hexcasting.command.usage.brainsweep");
        }
        Entity target = getEntity(server, sender, args[1]);
        if (!(target instanceof EntityLiving)) {
            sender.sendMessage(new TextComponentTranslation(
                "hexcasting.command.brainsweep.requires_living", target.getName()));
            return;
        }
        EntityLiving living = (EntityLiving) target;
        if (BrainsweepRecipes.isBrainswept(living)) {
            sender.sendMessage(new TextComponentTranslation(
                "hexcasting.command.brainsweep.already", target.getName()));
            return;
        }
        BrainsweepRecipes.markBrainswept(living);
        sender.sendMessage(new TextComponentTranslation(
            "hexcasting.command.brainsweep.applied", target.getName()));
    }

    private static void executePerWorldPatterns(MinecraftServer server,
                                                ICommandSender sender, String[] args)
        throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException(
                "hexcasting.command.usage.per_world");
        }
        if ("list".equals(args[1])) {
            requireLength(args, 2);
            listPatterns(sender, server.getWorld(0));
            return;
        }
        if ("give".equals(args[1])) {
            if (args.length < 3 || args.length > 4) {
                throw new WrongUsageException(
                    "hexcasting.command.usage.give");
            }
            ResourceLocation action = parseAction(args[2]);
            List<EntityPlayerMP> targets = resolveTargets(server, sender,
                args.length == 4 ? args[3] : null);
            givePattern(sender, server.getWorld(0), action, targets);
            return;
        }
        if ("giveAll".equals(args[1])) {
            if (args.length > 3) {
                throw new WrongUsageException(
                    "/hexcasting perWorldPatterns giveAll [targets]");
            }
            List<EntityPlayerMP> targets = resolveTargets(server, sender,
                args.length == 3 ? args[2] : null);
            int count = 0;
            for (ResourceLocation action : PerWorldPatternData.perWorldActionIds()) {
                count += givePattern(sender, server.getWorld(0), action, targets);
            }
            sender.sendMessage(new TextComponentTranslation(
                "hexcasting.command.patterns.gave_all", count));
            return;
        }
        throw new WrongUsageException(
            "hexcasting.command.usage.per_world");
    }

    private static void listPatterns(ICommandSender sender, WorldServer overworld) {
        List<ResourceLocation> actions = new ArrayList<>(
            PerWorldPatternData.perWorldActionIds());
        Collections.sort(actions, Comparator.comparing(ResourceLocation::toString));
        for (ResourceLocation action : actions) {
            if (PerWorldPatternData.patternFor(overworld, action) != null) {
                sender.sendMessage(new TextComponentTranslation(
                    "hexcasting.command.patterns.entry", action.toString(),
                    PerWorldPatternData.patternFor(overworld, action).toString()));
            }
        }
    }

    private static ResourceLocation parseAction(String value) throws CommandException {
        final ResourceLocation action;
        try {
            action = new ResourceLocation(value);
        } catch (RuntimeException exception) {
            throw new CommandException(
                "hexcasting.command.error.invalid_action", value);
        }
        if (!PerWorldPatternData.isPerWorldAction(action)) {
            throw new CommandException(
                "hexcasting.command.error.not_per_world", action.toString());
        }
        return action;
    }

    private static int givePattern(ICommandSender sender, WorldServer overworld,
                                   ResourceLocation action,
                                   List<EntityPlayerMP> targets) {
        if (targets.isEmpty()) {
            return 0;
        }
        HexPattern pattern = PerWorldPatternData.patternFor(overworld, action);
        Item scroll = HexItems.EXTRA_ITEMS.get("scroll");
        if (pattern == null || scroll == null) {
            sender.sendMessage(new TextComponentTranslation(
                "hexcasting.command.patterns.unavailable", action.toString()));
            return 0;
        }
        int count = 0;
        for (EntityPlayerMP target : targets) {
            ItemStack stack = new ItemStack(scroll);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString(ItemPatternScroll.TAG_OP_ID, action.toString());
            tag.setBoolean(ItemPatternScroll.TAG_ANCIENT, true);
            tag.setTag(ItemPatternScroll.TAG_PATTERN, pattern.serializeToNBT());
            stack.setTagCompound(tag);
            EntityItem dropped = target.dropItem(stack, false, false);
            if (dropped != null) {
                dropped.setNoPickupDelay();
                dropped.setThrower(target.getName());
            }
            count++;
        }
        sender.sendMessage(new TextComponentTranslation(
            "hexcasting.command.patterns.gave", action.toString(), targets.size()));
        return count;
    }

    private static List<EntityPlayerMP> resolveTargets(MinecraftServer server,
                                                       ICommandSender sender,
                                                       String selector)
        throws CommandException {
        if (selector != null) {
            return getPlayers(server, sender, selector);
        }
        if (sender.getCommandSenderEntity() instanceof EntityPlayerMP) {
            return Collections.singletonList((EntityPlayerMP) sender.getCommandSenderEntity());
        }
        return Collections.emptyList();
    }

    private static void requireLength(String[] args, int expected)
        throws WrongUsageException {
        if (args.length != expected) {
            throw new WrongUsageException(
                "hexcasting.command.error.unexpected_arguments");
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
                                          String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
        }
        if (args.length == 2 && "perWorldPatterns".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args,
                "list", "give", "giveAll");
        }
        if (args.length == 3 && "give".equals(args[1])
            && "perWorldPatterns".equals(args[0])) {
            List<String> ids = new ArrayList<>();
            for (ResourceLocation action : PerWorldPatternData.perWorldActionIds()) {
                ids.add(action.toString());
            }
            return getListOfStringsMatchingLastWord(args, ids);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return args.length > 1 && (("brainsweep".equals(args[0]) && index == 1)
            || ("perWorldPatterns".equals(args[0])
                && "give".equals(args[1]) && index == 3)
            || ("perWorldPatterns".equals(args[0])
                && "giveAll".equals(args[1]) && index == 2));
    }
}
