package at.petra_k.hexcasting.interop.crafttweaker;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.Mishap;
import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;
import at.petra_k.hexcasting.api.casting.iota.EntityIota;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.block.TileEntityImpetus;
import at.petra_k.hexcasting.common.lib.hex.HexActions;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import crafttweaker.annotations.ModOnly;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.entity.IEntity;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.player.IPlayer;
import crafttweaker.api.world.IWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

/** Casting context and operations exposed to a custom action. */
@ModOnly("crafttweaker")
@ZenClass("mods.hexcasting.api.HexCastingEnvironment")
@ZenRegister
public final class HexCastingEnvironment {
    private final CastingVM vm;
    private final ResourceLocation actionId;

    HexCastingEnvironment(CastingVM vm, ResourceLocation actionId) {
        this.vm = vm;
        this.actionId = actionId;
    }

    @ZenGetter("player")
    @ZenMethod
    public IPlayer getPlayer() {
        return CraftTweakerMC.getIPlayer(vm.getPlayer());
    }

    /** Return the caster as a general entity for scripts that do not need the
     * player-only CraftTweaker methods. */
    @ZenGetter("caster")
    @ZenMethod
    public IEntity getCaster() {
        return vm.getPlayer() == null ? null
            : CraftTweakerMC.getIEntity(vm.getPlayer());
    }

    @ZenGetter("world")
    @ZenMethod
    public IWorld getWorld() {
        return CraftTweakerMC.getIWorld(world());
    }

    @ZenGetter("actionId")
    @ZenMethod
    public String getActionId() {
        return actionId == null ? "" : actionId.toString();
    }

    @ZenGetter("hand")
    @ZenMethod
    public String getHand() {
        return vm.getCastingHand().name().toLowerCase(java.util.Locale.ROOT);
    }

    @ZenGetter("circle")
    @ZenMethod
    public boolean isCircle() {
        return vm.getCircleExecutionState() != null;
    }

    @ZenGetter("dimension")
    @ZenMethod
    public int getDimension() {
        World world = world();
        return world == null || world.provider == null
            ? Integer.MIN_VALUE : world.provider.getDimension();
    }

    @ZenGetter("patternSignature")
    @ZenMethod
    public String getPatternSignature() {
        HexPattern pattern = HexActionRegistry.getPattern(actionId, world());
        return pattern == null ? "" : pattern.signature();
    }

    @ZenMethod
    public String getPatternAngles() {
        HexPattern pattern = HexActionRegistry.getPattern(actionId, world());
        return pattern == null ? "" : pattern.anglesSignature();
    }

    @ZenGetter("availableMedia")
    @ZenMethod
    public long getAvailableMedia() {
        return vm.getAvailableMedia();
    }

    @ZenMethod
    public void consumeMedia(long amount) {
        try {
            vm.consumeMedia(amount);
        } catch (CastingException exception) {
            throw CustomSpellFailure.from(exception);
        }
    }

    /** Resolve a serialized entity Iota against the caster's loaded world. */
    @ZenMethod
    public IEntity resolveEntity(HexCastingIota target) {
        if (target == null || !target.isEntity()) {
            throw CustomSpellFailure.from(Mishap.invalidValue(
                "hexcasting.error.invalid_iota",
                "resolveEntity expects an entity Iota"));
        }
        try {
            Entity resolved = HexActions.resolveEntity((EntityIota) target.unwrap(), vm);
            if (resolved == null) {
                throw Mishap.badEntity("hexcasting.error.entity_unavailable");
            }
            return CraftTweakerMC.getIEntity(resolved);
        } catch (CastingException exception) {
            throw CustomSpellFailure.from(exception);
        } catch (RuntimeException exception) {
            throw CustomSpellFailure.mishap("hexcasting.error.invalid_value",
                "Could not expose the resolved entity to CraftTweaker: "
                    + exception.getClass().getSimpleName());
        }
    }

    @ZenMethod
    public void damageEntity(HexCastingIota target, float amount) {
        if (target == null || !target.isEntity()) {
            throw CustomSpellFailure.from(Mishap.invalidValue(
                "hexcasting.error.invalid_iota", "damageEntity expects an entity Iota"));
        }
        if (Float.isNaN(amount) || Float.isInfinite(amount) || amount <= 0.0F) {
            throw CustomSpellFailure.from(Mishap.invalidValue(
                "hexcasting.error.invalid_value", "damage amount must be finite and positive"));
        }

        EntityPlayer player = vm.getPlayer();
        if (player == null) {
            throw CustomSpellFailure.from(
                Mishap.invalidContext("hexcasting.error.entity_data_context"));
        }

        final Entity entity;
        try {
            entity = HexActions.resolveEntity((EntityIota) target.unwrap(), vm);
            if (!(entity instanceof EntityLivingBase)) {
                throw Mishap.invalidValue("hexcasting.error.invalid_iota",
                    "damageEntity expects a living entity");
            }
            HexActions.requireEntityInRange(vm, player, entity,
                "hexcasting.error.add_motion_range");
        } catch (CastingException exception) {
            throw CustomSpellFailure.from(exception);
        }

        EntityLivingBase living = (EntityLivingBase) entity;
        if (living.isDead || living.getHealth() <= 0.0F) {
            throw CustomSpellFailure.from(
                Mishap.badEntity("hexcasting.error.entity_unavailable"));
        }
        if (living.isEntityInvulnerable(DamageSource.MAGIC)) {
            throw CustomSpellFailure.from(Mishap.immuneEntity(living));
        }

        final float healthBefore = living.getHealth();
        final boolean deadBefore = living.isDead;
        final int hurtResistantBefore = living.hurtResistantTime;
        vm.addRollbackAction(() -> {
            living.setHealth(healthBefore);
            living.isDead = deadBefore;
            living.hurtResistantTime = hurtResistantBefore;
        });

        living.hurtResistantTime = 0;
        boolean damaged = living.attackEntityFrom(DamageSource.MAGIC, amount);
        if (!damaged && living.isEntityInvulnerable(DamageSource.MAGIC)) {
            throw CustomSpellFailure.from(Mishap.immuneEntity(living));
        }
        // Some 1.12 entities reject a normal hit for reasons unrelated to
        // immunity. Preserve the custom action's promised damage semantics.
        if (!damaged && !living.isDead) {
            living.setHealth(living.getHealth() - amount);
            if (living.getHealth() <= 0.0F) {
                living.setDead();
            }
        }
    }

    private World world() {
        EntityPlayer player = vm.getPlayer();
        if (player != null) {
            return player.world;
        }
        if (vm.getMediaHolder() instanceof TileEntityImpetus) {
            return ((TileEntityImpetus) vm.getMediaHolder()).getWorld();
        }
        return null;
    }
}
