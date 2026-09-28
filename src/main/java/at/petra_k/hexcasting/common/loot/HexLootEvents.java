package at.petra_k.hexcasting.common.loot;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.PatternIota;
import at.petra_k.hexcasting.api.casting.math.HexDir;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.api.misc.MediaConstants;
import at.petra_k.hexcasting.common.item.ItemPackagedSpell;
import at.petra_k.hexcasting.common.item.ItemPatternScroll;
import at.petra_k.hexcasting.common.lib.HexItems;
import at.petra_k.hexcasting.common.world.PerWorldPatternData;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Forge 1.12.2 equivalent of Hex's loot-table injection layer. */
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID)
public final class HexLootEvents {
    private static final Logger LOGGER = LogManager.getLogger("HexCastingLoot");
    private static final double LORE_CHANCE = 0.4D;
    private static final double CYPHER_CHANCE = 0.4D;
    private static final UUID NIL_UUID = new UUID(0L, 0L);
    private static final Map<ResourceLocation, Integer> SCROLL_INJECTIONS =
        new HashMap<>();
    private static final List<ResourceLocation> LORE_INJECTIONS =
        new ArrayList<>();
    private static final List<ResourceLocation> CYPHER_INJECTIONS =
        new ArrayList<>();
    private static final List<AncientHex> LOOT_HEXES = createLootHexes();

    static {
        addScrollInjection("simple_dungeon", 1);
        addScrollInjection("abandoned_mineshaft", 1);
        addScrollInjection("bastion_other", 1);
        addScrollInjection("nether_bridge", 1);
        addScrollInjection("jungle_temple", 2);
        addScrollInjection("desert_pyramid", 2);
        addScrollInjection("village/village_cartographer", 2);
        addScrollInjection("shipwreck_map", 3);
        addScrollInjection("bastion_treasure", 3);
        addScrollInjection("end_city_treasure", 3);
        addScrollInjection("ancient_city", 4);
        addScrollInjection("pillager_outpost", 4);
        addScrollInjection("woodland_mansion", 5);
        addScrollInjection("stronghold_library", 5);

        addLoreInjection("simple_dungeon");
        addLoreInjection("abandoned_mineshaft");
        addLoreInjection("pillager_outpost");
        addLoreInjection("woodland_mansion");
        addLoreInjection("stronghold_library");
        addLoreInjection("village/village_desert_house");
        addLoreInjection("village/village_plains_house");
        addLoreInjection("village/village_savanna_house");
        addLoreInjection("village/village_snowy_house");
        addLoreInjection("village/village_taiga_house");

        addCypherInjection("simple_dungeon");
        addCypherInjection("abandoned_mineshaft");
        addCypherInjection("stronghold_corridor");
        addCypherInjection("jungle_temple");
        addCypherInjection("desert_pyramid");
        addCypherInjection("ancient_city");
        addCypherInjection("nether_bridge");
    }

    private HexLootEvents() {
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (event == null || event.getName() == null || event.getTable() == null) {
            return;
        }

        ResourceLocation tableId = event.getName();
        if (HexAPI.MOD_ID.equals(tableId.getResourceDomain())
            && "random_scroll".equals(tableId.getResourcePath())) {
            addPool(event.getTable(), "custom#hexcasting_random_scroll",
                new RandomScrollEntry("custom#hexcasting_random_scroll_entry", 1, true));
            return;
        }
        if (HexAPI.MOD_ID.equals(tableId.getResourceDomain())
            && "random_cypher".equals(tableId.getResourcePath())) {
            addPool(event.getTable(), "custom#hexcasting_random_cypher",
                new AncientCypherEntry("custom#hexcasting_random_cypher_entry", 1.0D));
            return;
        }

        Integer scrollRange = SCROLL_INJECTIONS.get(tableId);
        if (scrollRange != null) {
            addPool(event.getTable(), "custom#hexcasting_scroll_"
                    + tableId.getResourcePath().replace('/', '_'),
                new RandomScrollEntry("custom#hexcasting_scroll_entry",
                    scrollRange.intValue()));
        }
        if (LORE_INJECTIONS.contains(tableId)) {
            addPool(event.getTable(), "custom#hexcasting_lore_"
                    + tableId.getResourcePath().replace('/', '_'),
                new LoreFragmentEntry("custom#hexcasting_lore_entry",
                    LORE_CHANCE));
        }
        if (CYPHER_INJECTIONS.contains(tableId)) {
            addPool(event.getTable(), "custom#hexcasting_cypher_"
                    + tableId.getResourcePath().replace('/', '_'),
                new AncientCypherEntry("custom#hexcasting_cypher_entry",
                    CYPHER_CHANCE));
        }
    }

    private static void addPool(LootTable table, String poolName, LootEntry entry) {
        if (table.getPool(poolName) != null) {
            return;
        }
        table.addPool(new LootPool(
            new LootEntry[] {entry},
            new LootCondition[0],
            new RandomValueRange(1.0F),
            new RandomValueRange(0.0F),
            poolName));
    }

    private static void addScrollInjection(String tablePath, int range) {
        SCROLL_INJECTIONS.put(new ResourceLocation("minecraft", "chests/" + tablePath), range);
    }

    private static void addLoreInjection(String tablePath) {
        LORE_INJECTIONS.add(new ResourceLocation("minecraft", "chests/" + tablePath));
    }

    private static void addCypherInjection(String tablePath) {
        CYPHER_INJECTIONS.add(new ResourceLocation("minecraft", "chests/" + tablePath));
    }

    private static WorldServer getOverworld(LootContext context) {
        if (context == null || context.getWorld() == null) {
            return null;
        }
        WorldServer world = context.getWorld();
        return world.getMinecraftServer() == null
            ? world : world.getMinecraftServer().getWorld(0);
    }

    private static ItemStack createPerWorldScroll(WorldServer overworld,
                                                    ResourceLocation action) {
        Item scroll = HexItems.EXTRA_ITEMS.get("scroll");
        if (scroll == null || overworld == null || action == null) {
            return ItemStack.EMPTY;
        }

        HexPattern pattern = PerWorldPatternData.patternFor(overworld, action);
        ItemStack stack = new ItemStack(scroll);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(ItemPatternScroll.TAG_OP_ID, action.toString());
        tag.setBoolean(ItemPatternScroll.TAG_ANCIENT, true);
        if (pattern != null) {
            tag.setTag(ItemPatternScroll.TAG_PATTERN, pattern.serializeToNBT());
        } else {
            tag.setString(ItemPatternScroll.TAG_RECALC_WARNING, action.toString());
            LOGGER.warn("No per-world pattern was available for loot scroll {}", action);
        }
        stack.setTagCompound(tag);
        return stack;
    }

    private static ItemStack createAncientCypher(Random random) {
        Item item = HexItems.EXTRA_ITEMS.get("ancient_cypher");
        if (item == null || LOOT_HEXES.isEmpty()) {
            return ItemStack.EMPTY;
        }

        AncientHex hex = LOOT_HEXES.get(random.nextInt(LOOT_HEXES.size()));
        List<Iota> program = new ArrayList<>();
        for (String encoded : hex.patterns) {
            String[] pieces = encoded.split(" ", 2);
            if (pieces.length != 2) {
                continue;
            }
            HexPattern pattern = HexPattern.fromAngles(
                pieces[1], HexDir.fromString(pieces[0]));
            program.add(new PatternIota(pattern));
        }
        if (program.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(item);
        ItemPackagedSpell.writePackagedProgram(
            stack, program, 32L * MediaConstants.SHARD_UNIT);
        ItemPackagedSpell.setPigment(
            stack, 0xE6B84A, "ancient_colorizer", NIL_UUID);
        ItemPackagedSpell.setVariant(stack, random.nextInt(8));
        return stack;
    }

    private abstract static class HexLootEntry extends LootEntry {
        private HexLootEntry(String entryName) {
            super(1, 0, new LootCondition[0], entryName);
        }

        @Override
        protected void serialize(JsonObject json, JsonSerializationContext context) {
            json.addProperty("type", "empty");
        }
    }

    private static final class RandomScrollEntry extends HexLootEntry {
        private final int range;
        private final boolean fixedCount;

        private RandomScrollEntry(String entryName, int range) {
            this(entryName, range, false);
        }

        private RandomScrollEntry(String entryName, int range, boolean fixedCount) {
            super(entryName);
            this.range = Math.max(0, range);
            this.fixedCount = fixedCount;
        }

        @Override
        public void addLoot(Collection<ItemStack> stacks, Random random,
                            LootContext context) {
            int count = fixedCount ? range : HexLootHandler.getScrollCount(range, random);
            WorldServer overworld = getOverworld(context);
            List<ResourceLocation> actions = PerWorldPatternData.perWorldActionIds();
            if (overworld == null || actions.isEmpty()) {
                return;
            }
            for (int i = 0; i < count; i++) {
                ResourceLocation action = actions.get(random.nextInt(actions.size()));
                ItemStack stack = createPerWorldScroll(overworld, action);
                if (!stack.isEmpty()) {
                    stacks.add(stack);
                }
            }
        }
    }

    private static final class AncientCypherEntry extends HexLootEntry {
        private final double chance;

        private AncientCypherEntry(String entryName, double chance) {
            super(entryName);
            this.chance = Math.max(0.0D, Math.min(1.0D, chance));
        }

        @Override
        public void addLoot(Collection<ItemStack> stacks, Random random,
                            LootContext context) {
            if (random.nextDouble() <= chance) {
                ItemStack stack = createAncientCypher(random);
                if (!stack.isEmpty()) {
                    stacks.add(stack);
                }
            }
        }
    }

    private static final class LoreFragmentEntry extends HexLootEntry {
        private final double chance;

        private LoreFragmentEntry(String entryName, double chance) {
            super(entryName);
            this.chance = Math.max(0.0D, Math.min(1.0D, chance));
        }

        @Override
        public void addLoot(Collection<ItemStack> stacks, Random random,
                            LootContext context) {
            if (random.nextDouble() <= chance) {
                Item item = HexItems.EXTRA_ITEMS.get("lore_fragment");
                if (item != null) {
                    stacks.add(new ItemStack(item));
                }
            }
        }
    }

    private static final class AncientHex {
        private final String[] patterns;

        private AncientHex(String... patterns) {
            this.patterns = patterns;
        }
    }

    private static List<AncientHex> createLootHexes() {
        return Collections.unmodifiableList(Arrays.asList(
            new AncientHex("NORTH_EAST qaq", "EAST aa", "NORTH_EAST qaq",
                "NORTH_EAST wa", "EAST wqaawdd", "EAST qaqqqqq"),
            new AncientHex("NORTH_EAST qaq", "EAST aa", "NORTH_EAST qaq",
                "NORTH_EAST wa", "EAST wqaawdd", "SOUTH_EAST aaqawawa"),
            new AncientHex("NORTH_EAST qaq", "EAST aa", "NORTH_EAST qaq",
                "NORTH_EAST wa", "EAST aadadaaw", "EAST wqaawdd",
                "NORTH_EAST ddqdd", "EAST weddwaa", "NORTH_EAST waaw",
                "NORTH_EAST qqd"),
            new AncientHex("NORTH_EAST qaq", "EAST aa", "NORTH_EAST qaq",
                "NORTH_EAST wa", "EAST aadadaaw", "EAST wqaawdd",
                "NORTH_EAST ddqdd", "EAST weddwaa", "NORTH_EAST waaw",
                "SOUTH_EAST aqaaedwd", "EAST aadaadaa",
                "NORTH_EAST wqaqwawqaqw", "NORTH_EAST wqaqwawqaqw",
                "NORTH_EAST wqaqwawqaqw"),
            new AncientHex("NORTH_EAST qaq", "EAST aadaa", "NORTH_EAST wa",
                "SOUTH_EAST aqaawa", "SOUTH_EAST waqaw", "SOUTH_WEST awqqqwaqw"),
            new AncientHex("NORTH_EAST qaq", "EAST aadaa", "NORTH_EAST wa",
                "NORTH_WEST eqqq", "SOUTH_EAST aqaawd", "SOUTH_EAST e",
                "NORTH_WEST qqqqqew", "SOUTH_WEST eeeeeqw", "SOUTH_EAST awdd",
                "NORTH_EAST wdedw", "SOUTH_WEST awqqqwaqw"),
            new AncientHex("NORTH_EAST qaq", "SOUTH_EAST aqaae",
                "WEST qqqqqawwawawd"),
            new AncientHex("NORTH_EAST qaq", "EAST aadaa", "EAST aa",
                "NORTH_EAST qaq", "NORTH_EAST wa", "EAST wqaawdd",
                "NORTH_EAST qaq", "EAST aa", "NORTH_WEST wddw",
                "NORTH_EAST wqaqw", "SOUTH_EAST aqaaw", "NORTH_WEST wddw",
                "SOUTH_WEST awqqqwaq"),
            new AncientHex("NORTH_EAST qaq", "NORTH_WEST qqqqqew",
                "SOUTH_EAST aqaawaa", "SOUTH_EAST waqaw",
                "SOUTH_WEST awqqqwaqw"),
            new AncientHex("WEST qqq", "EAST aadaa", "EAST aa",
                "SOUTH_EAST aqaawa", "SOUTH_WEST ewdqdwe", "NORTH_EAST de",
                "EAST eee", "NORTH_EAST qaq", "EAST aa",
                "SOUTH_EAST aqaaeaqq", "SOUTH_EAST qqqqqwdeddwd",
                "NORTH_EAST dadad"),
            new AncientHex("NORTH_EAST qaq", "EAST aa", "NORTH_EAST qaq",
                "NORTH_EAST wa", "EAST weaqa", "EAST aadaa", "EAST dd",
                "NORTH_EAST qaq", "EAST aa", "EAST aawdd", "NORTH_WEST wddw",
                "EAST aadaa", "NORTH_EAST wqaqw", "NORTH_EAST wdedw",
                "SOUTH_EAST aqaawa", "SOUTH_EAST waqaw",
                "SOUTH_WEST awqqqwaqw"),
            new AncientHex("NORTH_EAST qaq", "EAST aa", "SOUTH_EAST aqaaedwd",
                "EAST ddwddwdd"),
            new AncientHex("NORTH_EAST qaq", "EAST aa", "SOUTH_EAST aqaawaa",
                "EAST aadaadaa", "SOUTH_EAST aqawqadaq", "SOUTH_EAST aqaaedwd",
                "EAST aawaawaa", "NORTH_EAST qqa", "EAST qaqqqqq"),
            new AncientHex("WEST qqq", "SOUTH_EAST aaqawawa", "EAST eee",
                "NORTH_EAST qaq", "EAST aa", "SOUTH_EAST aqaae",
                "SOUTH_EAST qqqqqwded", "SOUTH_WEST aaqwqaa", "SOUTH_EAST a",
                "NORTH_EAST dadad"),
            new AncientHex("WEST qqq", "SOUTH_EAST aqaae", "SOUTH_EAST aqaaw",
                "SOUTH_WEST qqqqqaewawawe", "EAST eee", "NORTH_EAST qaq",
                "EAST aa", "SOUTH_EAST aqaae", "SOUTH_EAST qqqqqwdeddwd",
                "SOUTH_WEST aaqwqaa", "SOUTH_EAST a", "NORTH_EAST dadad"),
            new AncientHex("NORTH_EAST qaq", "SOUTH_EAST aqaaq",
                "SOUTH_WEST awawaawq")
        ));
    }
}
