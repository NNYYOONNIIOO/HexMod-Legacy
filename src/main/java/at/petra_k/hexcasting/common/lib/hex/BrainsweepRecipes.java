package at.petra_k.hexcasting.common.lib.hex;

import at.petra_k.hexcasting.common.misc.AmethystCompat;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

import java.util.Locale;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The 1.12.2 brainsweep recipe table.
 *
 * <p>Modern Hex loads these recipes from data packs.  1.12.2 has no matching
 * recipe type, so this small table keeps the same matching rules in code and
 * only exposes recipes whose result blocks are actually registered in this
 * port.  In particular, the modern Allay/amethyst pair remains an optional
 * compatibility entry: it becomes usable when another mod supplies an
 * Allay-like entity and the corresponding result block.</p>
 */
public final class BrainsweepRecipes {
    public static final String BRAINSWEPT_TAG = "hexcasting:brainswept";
    private static final String LEGACY_BRAINSWEPT_TAG = "hexcasting.brainswept";
    private static final long VILLAGER_MEDIA_COST = 1_000_000L;
    private static final long ALLAY_MEDIA_COST = 100_000L;
    private static final List<CustomRecipe> CUSTOM_RECIPES = new ArrayList<>();
    private static final List<DisplayRecipe> BASE_DISPLAY_RECIPES = Collections.unmodifiableList(Arrays.asList(
        new DisplayRecipe("minecraft:amethyst_block", "raids:allay", null, 1,
            "hexcasting:quenched_allay", ALLAY_MEDIA_COST),
        new DisplayRecipe("minecraft:amethyst_block", "minecraft:villager", null, 3,
            "minecraft:budding_amethyst", VILLAGER_MEDIA_COST),
        new DisplayRecipe("hexcasting:akashic_connector", "minecraft:villager", "librarian", 5,
            "hexcasting:akashic_record", VILLAGER_MEDIA_COST),
        new DisplayRecipe("hexcasting:impetus/empty", "minecraft:villager", "fletcher", 2,
            "hexcasting:impetus/look", VILLAGER_MEDIA_COST),
        new DisplayRecipe("hexcasting:impetus/empty", "minecraft:villager", "toolsmith", 2,
            "hexcasting:impetus/rightclick", VILLAGER_MEDIA_COST),
        new DisplayRecipe("hexcasting:impetus/empty", "minecraft:villager", "cleric", 2,
            "hexcasting:impetus/redstone", VILLAGER_MEDIA_COST),
        new DisplayRecipe("hexcasting:directrix/empty", "minecraft:villager", "shepherd", 1,
            "hexcasting:directrix/boolean", VILLAGER_MEDIA_COST),
        new DisplayRecipe("hexcasting:directrix/empty", "minecraft:villager", "mason", 1,
            "hexcasting:directrix/redstone", VILLAGER_MEDIA_COST)
    ));

    private BrainsweepRecipes() {
    }

    /** Add or replace a script-defined block/entity transformation. */
    public static synchronized boolean addCustom(ItemStack input,
                                                 String entityType,
                                                 NBTTagCompound entityNbt,
                                                 long mediaCost,
                                                 ItemStack output) {
        return addCustom(input, entityType, entityNbt, mediaCost, output, null);
    }

    /** Add a custom recipe with an optional resource-pack translation key. */
    public static synchronized boolean addCustom(ItemStack input,
                                                 String entityType,
                                                 NBTTagCompound entityNbt,
                                                 long mediaCost,
                                                 ItemStack output,
                                                 String entityNameKey) {
        // CraftTweaker may evaluate scripts before a third-party dynamic
        // block has completed its registry setup. Keep the concrete stacks
        // and resolve their block states again when the spell is actually
        // matched instead of discarding an otherwise valid script recipe.
        if (input == null || output == null
            || entityType == null || entityType.trim().isEmpty() || mediaCost < 0L) {
            return false;
        }
        String normalizedEntityType = normalizeEntityType(entityType);
        if (normalizedEntityType == null) {
            return false;
        }
        ItemStack normalizedInput = input.copy();
        ItemStack normalizedOutput = output.copy();
        normalizedInput.setCount(1);
        normalizedOutput.setCount(1);
        NBTTagCompound nbt = entityNbt == null
            ? new NBTTagCompound() : entityNbt.copy();
        removeCustomInputs(normalizedInput, normalizedEntityType, nbt);
        CUSTOM_RECIPES.add(0, new CustomRecipe(normalizedInput,
            normalizedEntityType, nbt, mediaCost, normalizedOutput,
            RecipeStackMatcher.blockState(output), entityNameKey));
        return true;
    }

    /** Remove all script recipes for the given input block item. */
    public static synchronized boolean removeCustomInput(ItemStack input) {
        return removeCustomByStack(input, true);
    }

    /** Remove all script recipes producing the given output block item. */
    public static synchronized boolean removeCustomOutput(ItemStack output) {
        return removeCustomByStack(output, false);
    }

    public static synchronized boolean removeCustom(ItemStack input,
                                                    String entityType,
                                                    NBTTagCompound entityNbt,
                                                    long mediaCost,
                                                    ItemStack output) {
        String normalizedEntityType = normalizeEntityType(entityType);
        if (normalizedEntityType == null) {
            return false;
        }
        NBTTagCompound nbt = entityNbt == null
            ? new NBTTagCompound() : entityNbt;
        boolean removed = false;
        for (java.util.Iterator<CustomRecipe> iterator = CUSTOM_RECIPES.iterator();
             iterator.hasNext();) {
            CustomRecipe recipe = iterator.next();
            if (RecipeStackMatcher.same(recipe.input, input)
                && recipe.entityType.equals(normalizedEntityType)
                && recipe.entityNbt.equals(nbt)
                && recipe.mediaCost == mediaCost
                && RecipeStackMatcher.same(recipe.output, output)) {
                iterator.remove();
                removed = true;
            }
        }
        return removed;
    }

    public static synchronized List<CustomRecipe> customRecipes() {
        return Collections.unmodifiableList(new ArrayList<>(CUSTOM_RECIPES));
    }

    /**
     * Return the data-driven recipe descriptions used by the legacy JEI
     * adapter.  The action itself still uses {@link #find(IBlockState,
     * EntityLiving)} as its authoritative matcher; these descriptions only
     * provide the human-readable recipe list that 1.12.2 lacks.
     */
    public static List<DisplayRecipe> displayRecipes() {
        List<DisplayRecipe> recipes = new ArrayList<>();
        if (AmethystCompat.hasProvider()) {
            ResourceLocation block = AmethystCompat.blockId();
            ResourceLocation budding = AmethystCompat.buddingBlockId();
            if (block != null && budding != null) {
                recipes.add(new DisplayRecipe(block.toString(),
                    "minecraft:villager", null, 3, budding.toString(),
                    VILLAGER_MEDIA_COST));
                recipes.add(new DisplayRecipe(block.toString(),
                    "raids:allay", null, 1, "hexcasting:quenched_allay",
                    ALLAY_MEDIA_COST));
            }
        } else {
            // Preserve the pre-provider fallback for worlds using the
            // Hex/vanilla stand-in or another mod's vanilla amethyst blocks.
            recipes.addAll(BASE_DISPLAY_RECIPES);
        }

        if (AmethystCompat.hasProvider()) {
            recipes.addAll(BASE_DISPLAY_RECIPES.subList(2, BASE_DISPLAY_RECIPES.size()));
        }
        synchronized (BrainsweepRecipes.class) {
            for (CustomRecipe recipe : CUSTOM_RECIPES) {
                ResourceLocation inputId = RecipeStackMatcher.itemId(recipe.input);
                ResourceLocation outputId = RecipeStackMatcher.itemId(recipe.output);
                if (inputId != null && outputId != null) {
                    int minLevel = customVillagerMinLevel(recipe.entityType,
                        recipe.entityNbt);
                    recipes.add(new DisplayRecipe(inputId.toString(),
                        recipe.input.getMetadata(), recipe.entityType, null, minLevel,
                        outputId.toString(), recipe.output.getMetadata(),
                        recipe.entityNbt, recipe.mediaCost,
                        recipe.entityNameKey));
                }
            }
        }
        return Collections.unmodifiableList(recipes);
    }

    /** Find the first recipe matching the target block and living entity. */
    public static Match find(IBlockState input, EntityLiving victim) {
        if (input == null || victim == null) {
            return null;
        }

        String blockId = blockId(input.getBlock());

        ResourceLocation victimId = EntityList.getKey(victim);
        if (victimId != null) {
            synchronized (BrainsweepRecipes.class) {
                for (CustomRecipe recipe : CUSTOM_RECIPES) {
                    if (recipe.entityType.equals(victimId.toString())
                        && RecipeStackMatcher.matchesBlock(recipe.input, input)
                        && matchesEntityNbt(recipe.entityType, recipe.entityNbt,
                            victim.writeToNBT(new NBTTagCompound()))) {
                        IBlockState outputState = recipe.outputState;
                        if (outputState == null) {
                            outputState = RecipeStackMatcher.blockState(recipe.output);
                        }
                        if (outputState != null) {
                            return new Match(copyProperties(input, outputState),
                                recipe.mediaCost);
                        }
                    }
                }
            }
        }

        // 1.12.2 does not contain the vanilla entries.  Keeping the recipe
        // conditional makes the port use the Raids Backport Allay and avoids
        // treating a fictional minecraft:allay entity as a valid target.
        if (isAllayLike(victim) && isAmethystInput(blockId)) {
            Match result = result(input, "hexcasting:quenched_allay", ALLAY_MEDIA_COST);
            if (result != null) {
                return result;
            }
        }

        if (!(victim instanceof EntityVillager)) {
            return null;
        }

        EntityVillager villager = (EntityVillager) victim;

        // Any sufficiently experienced villager can grow budding amethyst in
        // modern Hex.  The provider-specific pair is selected dynamically so
        // installing both backports cannot make the same recipe target both.
        if (isAmethystInput(blockId) && villagerLevel(villager) >= 3) {
            ResourceLocation budding = AmethystCompat.buddingBlockId();
            Match result = result(input,
                budding == null ? "minecraft:budding_amethyst" : budding.toString(),
                VILLAGER_MEDIA_COST);
            if (result != null) {
                return result;
            }
        }

        if ("hexcasting:akashic_connector".equals(blockId)
            && villagerLevel(villager) >= 5
            && hasProfession(villager, "librarian")) {
            return result(input, "hexcasting:akashic_record", VILLAGER_MEDIA_COST);
        }

        if ("hexcasting:impetus/empty".equals(blockId)
            && villagerLevel(villager) >= 2) {
            if (hasCareer(villager, "fletcher")) {
                return result(input, "hexcasting:impetus/look", VILLAGER_MEDIA_COST);
            }
            if (hasCareer(villager, "tool") || hasProfession(villager, "toolsmith")) {
                return result(input, "hexcasting:impetus/rightclick", VILLAGER_MEDIA_COST);
            }
            if (hasCareer(villager, "cleric") || hasProfession(villager, "cleric")) {
                return result(input, "hexcasting:impetus/redstone", VILLAGER_MEDIA_COST);
            }
        }

        if ("hexcasting:directrix/empty".equals(blockId)
            && villagerLevel(villager) >= 1) {
            if (hasCareer(villager, "shepherd") || hasProfession(villager, "shepherd")) {
                return result(input, "hexcasting:directrix/boolean", VILLAGER_MEDIA_COST);
            }
            // 1.12.2 has no mason profession.  Its legacy smith profession
            // is the closest built-in equivalent and is deliberately accepted
            // as the redstone-directrix source.  A later compatibility mod can
            // use the modern mason registry name directly.
            if (hasProfession(villager, "mason") || hasProfession(villager, "smith")) {
                return result(input, "hexcasting:directrix/redstone", VILLAGER_MEDIA_COST);
            }
        }

        return null;
    }

    public static boolean isBrainswept(EntityLiving living) {
        if (living == null) {
            return false;
        }
        NBTTagCompound data = living.getEntityData();
        return data.getBoolean(BRAINSWEPT_TAG) || data.getBoolean(LEGACY_BRAINSWEPT_TAG);
    }

    /**
     * Apply the one point of true damage used by the modern bad-brainsweep
     * mishap.  The action is evaluated on the server, but keeping this guard
     * here prevents a future caller from damaging a client-side mirror.
     */
    public static void hurtForFailedBrainsweep(EntityLiving living,
                                               EntityPlayer caster) {
        trulyHurt(living, caster, 1.0F);
    }

    /**
     * A second attempt to flay an already empty mind kills the subject in the
     * modern implementation.  Using its current health as the damage amount
     * keeps ordinary death handling (including totems) intact.
     */
    public static void killForRepeatedBrainsweep(EntityLiving living,
                                                 EntityPlayer caster) {
        if (living == null || living.isDead) {
            return;
        }
        trulyHurt(living, caster, living.getHealth());
    }

    /**
     * 1.12.2 has no equivalent of the modern Mishap.trulyHurt helper.  Reset
     * the normal hurt-resistance window, use a damage source that bypasses
     * armor, and fall back to direct health subtraction when vanilla refuses
     * to apply the hit for a non-invulnerability reason.
     */
    private static void trulyHurt(EntityLiving living, EntityPlayer caster,
                                  float amount) {
        if (living == null || living.world == null || living.world.isRemote
            || living.isDead || amount <= 0.0F) {
            return;
        }

        DamageSource source = overcastDamage(caster);
        living.hurtResistantTime = 0;
        if (!living.attackEntityFrom(source, amount)
            && !living.isEntityInvulnerable(source)
            && !living.isDead) {
            living.setHealth(living.getHealth() - amount);
            if (living.getHealth() <= 0.0F) {
                living.setDead();
            }
        }
    }

    private static DamageSource overcastDamage(EntityPlayer caster) {
        DamageSource source = caster == null
            ? DamageSource.MAGIC
            : new EntityDamageSource("hexcasting.overcast", caster);
        return source.setDamageBypassesArmor().setDamageIsAbsolute().setMagicDamage();
    }

    /** Apply the persistent no-AI state after a successful brainsweep. */
    public static void markBrainswept(EntityLiving living) {
        if (living == null) {
            return;
        }
        NBTTagCompound data = living.getEntityData();
        data.setBoolean(BRAINSWEPT_TAG, true);
        // Keep worlds written by the first 1.12.2 prototype readable.
        data.setBoolean(LEGACY_BRAINSWEPT_TAG, true);
        living.setNoAI(true);
        living.enablePersistence();
    }

    /** Undo a marker applied by a brainsweep that was later rolled back. */
    public static void unmarkBrainswept(EntityLiving living) {
        if (living == null) {
            return;
        }
        NBTTagCompound data = living.getEntityData();
        data.removeTag(BRAINSWEPT_TAG);
        data.removeTag(LEGACY_BRAINSWEPT_TAG);
    }

    private static Match result(IBlockState original, String resultId, long mediaCost) {
        Block resultBlock = Block.REGISTRY.getObject(new ResourceLocation(resultId));
        if (resultBlock == null || resultBlock == Blocks.AIR) {
            return null;
        }
        return new Match(copyProperties(original, resultBlock.getDefaultState()), mediaCost);
    }

    /** Preserve shared facing/energized (and any future common) properties. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static IBlockState copyProperties(IBlockState original, IBlockState result) {
        // A custom recipe may intentionally transform one metadata variant
        // into another variant of the same block (for example, a dynamic
        // budding block).  Copying every property in that case would restore
        // the input variant and silently discard the recipe output.
        if (original.getBlock() == result.getBlock()) {
            return result;
        }
        for (Map.Entry<IProperty<?>, Comparable<?>> property : original.getProperties().entrySet()) {
            IProperty key = property.getKey();
            if (result.getPropertyKeys().contains(key)) {
                result = result.withProperty(key, (Comparable) property.getValue());
            }
        }
        return result;
    }

    private static String blockId(Block block) {
        ResourceLocation id = block == null ? null : block.getRegistryName();
        return id == null ? "" : id.toString();
    }

    private static boolean removeCustomByStack(ItemStack stack,
                                               boolean inputStack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        boolean removed = false;
        for (java.util.Iterator<CustomRecipe> iterator = CUSTOM_RECIPES.iterator();
             iterator.hasNext();) {
            CustomRecipe recipe = iterator.next();
            ItemStack candidate = inputStack ? recipe.input : recipe.output;
            if (sameItem(candidate, stack)) {
                iterator.remove();
                removed = true;
            }
        }
        return removed;
    }

    private static void removeCustomInputs(ItemStack input,
                                           String entityId,
                                           NBTTagCompound entityNbt) {
        for (java.util.Iterator<CustomRecipe> iterator = CUSTOM_RECIPES.iterator();
             iterator.hasNext();) {
            CustomRecipe recipe = iterator.next();
            if (sameItem(recipe.input, input)
                && recipe.entityType.equals(entityId)
                && recipe.entityNbt.equals(entityNbt)) {
                iterator.remove();
            }
        }
    }

    private static String normalizeEntityType(String entityType) {
        String normalized = entityType == null ? "" : entityType.trim();
        if (normalized.startsWith("entity:")) {
            normalized = normalized.substring("entity:".length());
        }
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return new ResourceLocation(normalized).toString();
        } catch (IllegalArgumentException ignored) {
            // Keep a namespaced-looking ID usable even if a third-party
            // registry uses a non-standard path character. The runtime
            // matcher will simply not select it unless the entity has the
            // same registry ID.
            return normalized;
        }
    }

    private static boolean sameItem(ItemStack left, ItemStack right) {
        return left != null && right != null && !left.isEmpty() && !right.isEmpty()
            && left.getItem() == right.getItem()
            && (right.getMetadata() == 32767
                || left.getMetadata() == right.getMetadata());
    }

    private static boolean matchesEntityNbt(String entityType,
                                            NBTTagCompound expected,
                                            NBTTagCompound actual) {
        return matchesNbtSubset(expected, actual,
            "minecraft:villager".equals(entityType));
    }

    private static boolean matchesNbtSubset(NBTTagCompound expected,
                                            NBTTagCompound actual,
                                            boolean villagerRoot) {
        if (expected == null || expected.getKeySet().isEmpty()) {
            return true;
        }
        if (actual == null) {
            return false;
        }
        for (String key : expected.getKeySet()) {
            NBTBase expectedTag = expected.getTag(key);
            NBTBase actualTag = actual.getTag(key);
            if (villagerRoot && "CareerLevel".equalsIgnoreCase(key)) {
                Integer minimumLevel = nbtInteger(expectedTag);
                Integer actualLevel = nbtInteger(actualTag);
                if (minimumLevel == null || actualLevel == null
                    || actualLevel < minimumLevel) {
                    return false;
                }
                continue;
            }
            if (actualTag == null || actualTag.getId() != expectedTag.getId()) {
                return false;
            }
            if (expectedTag instanceof NBTTagCompound) {
                if (!(actualTag instanceof NBTTagCompound)
                    || !matchesNbtSubset((NBTTagCompound) expectedTag,
                        (NBTTagCompound) actualTag, false)) {
                    return false;
                }
            } else if (!expectedTag.equals(actualTag)) {
                return false;
            }
        }
        return true;
    }

    private static int customVillagerMinLevel(String entityType,
                                             NBTTagCompound entityNbt) {
        if (!"minecraft:villager".equals(entityType) || entityNbt == null) {
            return 1;
        }
        for (String key : entityNbt.getKeySet()) {
            if ("CareerLevel".equalsIgnoreCase(key)) {
                Integer level = nbtInteger(entityNbt.getTag(key));
                if (level != null) {
                    return Math.max(1, level);
                }
            }
        }
        return 1;
    }

    private static Integer nbtInteger(NBTBase tag) {
        if (tag instanceof net.minecraft.nbt.NBTPrimitive) {
            return ((net.minecraft.nbt.NBTPrimitive) tag).getInt();
        }
        if (tag instanceof net.minecraft.nbt.NBTTagString) {
            try {
                return Integer.parseInt(((net.minecraft.nbt.NBTTagString) tag).getString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static boolean isAmethystInput(String blockId) {
        ResourceLocation selected = AmethystCompat.blockId();
        if (selected != null) {
            return selected.toString().equals(blockId);
        }
        // amethyst_dust_block is this port's 1.12.2 replacement for the
        // post-1.12 vanilla amethyst block used by the upstream recipes.
        return "minecraft:amethyst_block".equals(blockId)
            || "hexcasting:amethyst_dust_block".equals(blockId)
            || "farmers_future_delight:amethyst_block".equals(blockId);
    }

    private static boolean isAllayLike(Entity entity) {
        ResourceLocation id = net.minecraft.entity.EntityList.getKey(entity);
        return new ResourceLocation("raids", "allay").equals(id);
    }

    private static int villagerLevel(EntityVillager villager) {
        NBTTagCompound data = new NBTTagCompound();
        villager.writeEntityToNBT(data);
        // 1.12.2 writes zero before the first trade; modern villager data
        // represents that same adult, untraded state as level one.
        return Math.max(1, data.getInteger("CareerLevel"));
    }

    private static boolean hasProfession(EntityVillager villager, String expectedPath) {
        VillagerRegistry.VillagerProfession profession = villager.getProfessionForge();
        if (profession == null || profession.getRegistryName() == null) {
            return false;
        }
        String path = profession.getRegistryName().getResourcePath();
        return expectedPath.equals(path == null ? "" : path.toLowerCase(Locale.ROOT));
    }

    private static boolean hasCareer(EntityVillager villager, String expectedName) {
        VillagerRegistry.VillagerProfession profession = villager.getProfessionForge();
        if (profession == null) {
            return false;
        }
        NBTTagCompound data = new NBTTagCompound();
        villager.writeEntityToNBT(data);
        int careerId = data.getInteger("Career") - 1;
        VillagerRegistry.VillagerCareer career = profession.getCareer(careerId);
        return career != null && expectedName.equalsIgnoreCase(career.getName());
    }

    /** A matched recipe result and its media cost. */
    public static final class Match {
        private final IBlockState result;
        private final long mediaCost;

        private Match(IBlockState result, long mediaCost) {
            this.result = result;
            this.mediaCost = mediaCost;
        }

        public IBlockState getResult() {
            return result;
        }

        public long getMediaCost() {
            return mediaCost;
        }
    }

    /** Immutable script-defined brainsweep entry. */
    public static final class CustomRecipe {
        private final ItemStack input;
        private final String entityType;
        private final NBTTagCompound entityNbt;
        private final long mediaCost;
        private final ItemStack output;
        private final IBlockState outputState;
        private final String entityNameKey;

        private CustomRecipe(ItemStack input, String entityType,
                             NBTTagCompound entityNbt, long mediaCost,
                             ItemStack output, IBlockState outputState,
                             String entityNameKey) {
            this.input = input;
            this.entityType = entityType;
            this.entityNbt = entityNbt;
            this.mediaCost = mediaCost;
            this.output = output;
            this.outputState = outputState;
            this.entityNameKey = entityNameKey == null
                || entityNameKey.trim().isEmpty() ? null : entityNameKey.trim();
        }

        public ItemStack getInput() {
            return input.copy();
        }

        public String getEntityType() {
            return entityType;
        }

        public NBTTagCompound getEntityNbt() {
            return entityNbt.copy();
        }

        public long getMediaCost() {
            return mediaCost;
        }

        public ItemStack getOutput() {
            return output.copy();
        }

        public String getEntityNameKey() {
            return entityNameKey;
        }
    }

    /** Immutable JEI-facing description of one brainsweep recipe. */
    public static final class DisplayRecipe {
        private final String blockInputId;
        private final int blockInputMeta;
        private final String entityTypeId;
        private final String profession;
        private final int minLevel;
        private final String resultId;
        private final int resultMeta;
        private final NBTTagCompound entityNbt;
        private final long mediaCost;
        private final String entityNameKey;

        private DisplayRecipe(String blockInputId, String entityTypeId,
                              String profession, int minLevel,
                              String resultId, long mediaCost) {
            this(blockInputId, 0, entityTypeId, profession, minLevel,
                resultId, 0, null, mediaCost, null);
        }

        private DisplayRecipe(String blockInputId, int blockInputMeta,
                              String entityTypeId, String profession,
                              int minLevel, String resultId, int resultMeta,
                              NBTTagCompound entityNbt, long mediaCost,
                              String entityNameKey) {
            this.blockInputId = blockInputId;
            this.blockInputMeta = blockInputMeta;
            this.entityTypeId = entityTypeId;
            this.profession = profession;
            this.minLevel = minLevel;
            this.resultId = resultId;
            this.resultMeta = resultMeta;
            this.entityNbt = entityNbt == null ? new NBTTagCompound() : entityNbt.copy();
            this.mediaCost = mediaCost;
            this.entityNameKey = entityNameKey == null
                || entityNameKey.trim().isEmpty() ? null : entityNameKey.trim();
        }

        public String getBlockInputId() {
            return blockInputId;
        }

        public int getBlockInputMeta() {
            return blockInputMeta;
        }

        public String getEntityTypeId() {
            return entityTypeId;
        }

        public String getProfession() {
            return profession;
        }

        public int getMinLevel() {
            return minLevel;
        }

        public String getResultId() {
            return resultId;
        }

        public int getResultMeta() {
            return resultMeta;
        }

        public NBTTagCompound getEntityNbt() {
            return entityNbt.copy();
        }

        public long getMediaCost() {
            return mediaCost;
        }

        public String getEntityNameKey() {
            return entityNameKey;
        }
    }
}
