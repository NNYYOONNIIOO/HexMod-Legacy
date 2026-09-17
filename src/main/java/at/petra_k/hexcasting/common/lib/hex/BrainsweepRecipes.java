package at.petra_k.hexcasting.common.lib.hex;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

import java.util.Locale;
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

    private BrainsweepRecipes() {
    }

    /** Find the first recipe matching the target block and living entity. */
    public static Match find(IBlockState input, EntityLiving victim) {
        if (input == null || victim == null) {
            return null;
        }

        String blockId = blockId(input.getBlock());

        // 1.12.2 does not contain either of these vanilla entries.  Keeping
        // them conditional makes the port interoperate with a compatibility
        // mod without making an otherwise impossible recipe appear to work.
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
        // modern Hex.  The output is only enabled when a 1.12.2 compatibility
        // block with that registry name is present.
        if (isAmethystInput(blockId) && villagerLevel(villager) >= 3) {
            Match result = result(input, "minecraft:budding_amethyst", VILLAGER_MEDIA_COST);
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

    private static boolean isAmethystInput(String blockId) {
        // amethyst_dust_block is this port's 1.12.2 replacement for the
        // post-1.12 vanilla amethyst block used by the upstream recipes.
        return "minecraft:amethyst_block".equals(blockId)
            || "hexcasting:amethyst_dust_block".equals(blockId);
    }

    private static boolean isAllayLike(Entity entity) {
        String id = EntityListName(entity);
        return "allay".equalsIgnoreCase(id);
    }

    private static String EntityListName(Entity entity) {
        String id = net.minecraft.entity.EntityList.getEntityString(entity);
        return id == null ? "" : id;
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
}
