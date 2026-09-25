package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.Mishap;
import at.petra_k.hexcasting.api.casting.iota.DoubleIota;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.Vec3Iota;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.lib.hex.BrainsweepRecipes;
import at.petra_k.hexcasting.interop.inline.HexInline;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;

import java.util.Locale;
import java.util.List;

/**
 * One message boundary for every 1.12.2 casting entry point.
 *
 * <p>The modern evaluator prefixes a mishap with the name of the action that
 * caused it.  Older port callers used a mixture of raw translation keys,
 * exception messages, and the generic staff error.  Keeping that policy in
 * one small server-safe helper makes scrolls, packaged spells, thought knots,
 * and the staff agree without coupling the evaluator to a particular GUI.</p>
 */
public final class MishapFeedback {
    private MishapFeedback() {
    }

    public static void send(EntityPlayer player, CastingException exception) {
        if (exception == null) {
            return;
        }
        // Scrolls, spellbooks, and older block entry points do not have an
        // action context at their catch site. Convert them here so every
        // carrier still gets the same classification, side effects, and
        // localized message as the VM and staff paths.
        Mishap mishap = exception instanceof Mishap
            ? (Mishap) exception
            : asMishap(exception, null, null, player, 0, 0);
        applySideEffects(mishap);
        if (player != null) {
            player.sendMessage(new TextComponentString(localizeMishap(mishap)));
        }
    }

    /**
     * Execute the small, deterministic gameplay effect associated with a
     * failed action.  The evaluator deliberately rolls back media and the
     * Iota stack first; this method is the single post-failure boundary for
     * the effects that modern Hex applies immediately (bad targets, bad
     * blocks, and brainsweep mishaps).
     */
    public static void applySideEffects(Mishap mishap) {
        if (mishap == null || !mishap.beginSideEffects()) {
            return;
        }
        EntityPlayer caster = mishap.getCaster();
        if (caster == null || caster.world == null || caster.world.isRemote) {
            return;
        }

        Entity target = mishap.getTargetEntity();
        switch (mishap.getKind()) {
            case BAD_ITEM:
                if (target instanceof EntityItem) {
                    EntityItem item = (EntityItem) target;
                    item.motionX += (caster.world.rand.nextDouble() - 0.5D) * 0.05D;
                    item.motionY += 0.75D;
                    item.motionZ += (caster.world.rand.nextDouble() - 0.5D) * 0.05D;
                    item.velocityChanged = true;
                } else {
                    // Bad offhand/hotbar items use the same drop effect as
                    // modern MishapBadOffhandItem and MishapLackingHotbarItem:
                    // both held stacks leave the caster, rather than being
                    // silently restored after the VM rolls back.
                    yeetHeldItemsTowards(caster,
                        caster.getPositionVector().add(caster.getLookVec()));
                }
                return;
            case BAD_ENTITY:
            case BAD_LOCATION:
            case IMMUNE_ENTITY:
            case ENTITY_TOO_FAR:
                Vec3d destination = target == null
                    ? location(mishap, caster) : target.getPositionVector();
                if (destination != null) {
                    yeetHeldItemsTowards(caster, destination);
                }
                return;
            case UNENLIGHTENED:
                // Great-spell rejection drops the active focus, matching the
                // modern MishapUnenlightened side effect.  The failure sound
                // remains the VM/effect boundary's responsibility.
                yeetHeldItemsTowards(caster,
                    caster.getPositionVector().add(caster.getLookVec()));
                return;
            case OTHERS_NAME:
                // Modern Hex blinds the caster after a true-name violation;
                // using a real potion effect keeps the consequence visible
                // after the failed VM has restored its data transaction.
                if (caster != null) {
                    int duration = mishap.getTargetEntity() == caster
                        ? 5 * 20 : 60 * 20;
                    caster.addPotionEffect(new PotionEffect(
                        MobEffects.BLINDNESS, duration, 0, false, true));
                }
                return;
            case PERMISSION_DENIED:
                // Permission failures are location failures in modern Hex:
                // throw the held focus toward the refused target, but do not
                // damage a protected block or entity.
                Vec3d denied = target == null
                    ? location(mishap, caster) : target.getPositionVector();
                if (denied != null) {
                    yeetHeldItemsTowards(caster, denied);
                }
                return;
            case BAD_BLOCK:
                if (mishap.hasLocationContext()
                    && (mishap.getLocationDimension() == Integer.MIN_VALUE
                        || mishap.getLocationDimension() == caster.dimension)) {
                    caster.world.newExplosion(null,
                        mishap.getLocationX(), mishap.getLocationY(),
                        mishap.getLocationZ(), 0.25F, false, false);
                }
                return;
            case NO_AKASHIC_RECORD:
                // Modern Hex charges a small experience penalty for trying to
                // use the Akashic interface where no record exists.  Keep it
                // separate from the harmless bad-block explosion effect.
                removeExperience(caster, 100);
                return;
            case BAD_BRAINSWEEP:
                if (target instanceof EntityLiving) {
                    BrainsweepRecipes.hurtForFailedBrainsweep(
                        (EntityLiving) target, caster);
                }
                return;
            case ALREADY_BRAINSWEPT:
                if (target instanceof EntityLiving) {
                    BrainsweepRecipes.killForRepeatedBrainsweep(
                        (EntityLiving) target, caster);
                }
                return;
            case EVALUATION_LIMIT:
                if (caster.getAir() < 200) {
                    caster.attackEntityFrom(net.minecraft.util.DamageSource.DROWN, 2.0F);
                }
                caster.setAir(0);
                return;
            case ARITHMETIC:
                OvercastHelper.mishapDamage(caster);
                return;
            default:
                // Media shortages, invalid values and context failures do
                // not have a world-side mishap effect.
        }
    }

    private static Vec3d location(Mishap mishap, EntityPlayer caster) {
        if (!mishap.hasLocationContext()
            || mishap.getLocationDimension() != Integer.MIN_VALUE
                && mishap.getLocationDimension() != caster.dimension) {
            return null;
        }
        return new Vec3d(mishap.getLocationX(), mishap.getLocationY(),
            mishap.getLocationZ());
    }

    private static void yeetHeldItemsTowards(EntityPlayer caster, Vec3d destination) {
        Vec3d source = caster.getPositionVector();
        Vec3d delta = destination.subtract(source);
        double length = delta.lengthVector();
        if (length < 1.0E-6D) {
            delta = caster.getLookVec();
            length = delta.lengthVector();
        }
        if (length < 1.0E-6D) {
            delta = new Vec3d(0.0D, 1.0D, 0.0D);
            length = 1.0D;
        }
        delta = delta.scale(0.5D / length);

        for (EnumHand hand : EnumHand.values()) {
            ItemStack stack = caster.getHeldItem(hand);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            caster.setHeldItem(hand, ItemStack.EMPTY);
            EntityItem dropped = new EntityItem(caster.world,
                caster.posX, caster.posY + caster.getEyeHeight() * 0.5D,
                caster.posZ, stack);
            dropped.setPickupDelay(40);
            dropped.motionX = delta.x + (caster.world.rand.nextDouble() - 0.5D) * 0.1D;
            dropped.motionY = delta.y + (caster.world.rand.nextDouble() - 0.5D) * 0.1D;
            dropped.motionZ = delta.z + (caster.world.rand.nextDouble() - 0.5D) * 0.1D;
            caster.world.spawnEntity(dropped);
        }
    }

    /** Remove raw XP while keeping the vanilla level bar internally coherent. */
    private static void removeExperience(EntityPlayer player, int amount) {
        if (player == null || amount <= 0 || player.experienceTotal <= 0) {
            return;
        }
        int total = Math.max(0, player.experienceTotal - amount);
        player.experienceTotal = total;

        int level = 0;
        while (level < 32767 && experienceForLevel(level + 1) <= total) {
            level++;
        }
        player.experienceLevel = level;
        int base = experienceForLevel(level);
        int cap = Math.max(1, player.xpBarCap());
        player.experience = Math.max(0.0F,
            Math.min(0.999999F, (total - base) / (float) cap));
    }

    private static int experienceForLevel(int level) {
        if (level <= 15) {
            return level * level + 6 * level;
        }
        if (level <= 30) {
            return (int) (2.5D * level * level - 40.5D * level + 360.0D);
        }
        return (int) (4.5D * level * level - 162.5D * level + 2220.0D);
    }

    /** Convert an exception at an item/effect boundary and retain its context. */
    public static Mishap asMishap(CastingException exception, HexPattern pattern,
                                  ResourceLocation actionId, EntityPlayer player,
                                  int parenthesisDepth, int operation) {
        Mishap mishap = Mishap.from(exception, pattern, actionId, player,
            parenthesisDepth, operation);
        if (mishap.getPattern() == null || mishap.getActionId() == null
            || mishap.getCaster() == null) {
            mishap.withExecutionContext(
                mishap.getPattern() == null ? pattern : null,
                mishap.getActionId() == null ? actionId : null,
                mishap.getCaster() == null ? player : null,
                parenthesisDepth, operation);
        }
        return mishap;
    }

    public static String localize(CastingException exception) {
        if (exception instanceof Mishap) {
            return localizeMishap((Mishap) exception);
        }
        return localizeRaw(exception == null ? null : exception.getMessage());
    }

    private static String localizeMishap(Mishap mishap) {
        if (mishap.getKind() == Mishap.Kind.UNENLIGHTENED) {
            return localizeKey("hexcasting.message.cant_great_spell");
        }
        if (mishap.getKind() == Mishap.Kind.INVALID_PATTERN) {
            String pattern = mishap.getPattern() == null ? "?"
                : HexInline.formatPattern(mishap.getPattern());
            return I18n.translateToLocalFormatted(
                "hexcasting.message.pattern_unregistered", pattern);
        }

        String detail = localizeMishapDetail(mishap);
        String action = localizeAction(mishap.getActionDisplayKey(),
            mishap.getActionName());
        if (action == null || action.isEmpty()) {
            return detail;
        }
        String wrapper = I18n.translateToLocal("hexcasting.mishap");
        if ("hexcasting.mishap".equals(wrapper)) {
            return action + ": " + detail;
        }
        return I18n.translateToLocalFormatted("hexcasting.mishap", action, detail);
    }

    private static String localizeMishapDetail(Mishap mishap) {
        if (mishap.getKind() == Mishap.Kind.DISALLOWED_SPELL) {
            String suffix = mishap.getActionDisplayKey() == null
                ? "_generic" : "";
            String key = mishap.getErrorKey() + suffix;
            if (suffix.isEmpty()) {
                return I18n.translateToLocalFormatted(key,
                    localizeAction(mishap.getActionDisplayKey(),
                        mishap.getActionName()));
            }
            return localizeKey(key);
        }
        if (mishap.getKind() == Mishap.Kind.BAD_CASTER) {
            return localizeKey("hexcasting.mishap.bad_caster");
        }
        if (mishap.getKind() == Mishap.Kind.IMMUNE_ENTITY
            || mishap.getKind() == Mishap.Kind.ENTITY_TOO_FAR) {
            Entity target = mishap.getTargetEntity();
            String name = target == null || target.getDisplayName() == null
                ? "?" : target.getDisplayName().getUnformattedText();
            return I18n.translateToLocalFormatted(mishap.getErrorKey(), name);
        }
        if (mishap.getKind() == Mishap.Kind.OTHERS_NAME) {
            Entity target = mishap.getTargetEntity();
            EntityPlayer caster = mishap.getCaster();
            if (target == caster) {
                return I18n.translateToLocal("hexcasting.mishap.others_name.self");
            }
            String name = target == null || target.getDisplayName() == null
                ? "?" : target.getDisplayName().getUnformattedText();
            return I18n.translateToLocalFormatted(
                "hexcasting.mishap.others_name", name);
        }
        List<Iota> invalidOperators = mishap.getInvalidOperatorPerpetrators();
        if (invalidOperators != null && !invalidOperators.isEmpty()) {
            if (invalidOperators.size() == 1) {
                return I18n.translateToLocalFormatted(
                    "hexcasting.mishap.invalid_operator_args.one", 0,
                    invalidOperators.get(0).display());
            }
            StringBuilder values = new StringBuilder();
            for (int i = 0; i < invalidOperators.size(); i++) {
                if (i > 0) {
                    values.append(", ");
                }
                values.append(invalidOperators.get(i).display());
            }
            return I18n.translateToLocalFormatted(
                "hexcasting.mishap.invalid_operator_args.many",
                invalidOperators.size(), 0, invalidOperators.size() - 1,
                values.toString());
        }

        if (mishap.getInvalidPerpetrator() != null
            && mishap.getInvalidExpected() != null) {
            String expectedSuffix = mishap.getInvalidExpected();
            String expectedKey = expectedSuffix.startsWith("class.")
                ? "hexcasting.mishap.invalid_value." + expectedSuffix
                : expectedSuffix.indexOf('.') >= 0
                    ? "hexcasting.mishap.invalid_value." + expectedSuffix
                    : "hexcasting.mishap.invalid_value.class." + expectedSuffix;
            Object[] expectedArgs = mishap.getInvalidExpectedArgs();
            String expected = expectedArgs.length == 0
                ? localizeKey(expectedKey)
                : I18n.translateToLocalFormatted(expectedKey, expectedArgs);
            Iota perpetrator = mishap.getInvalidPerpetrator();
            String actual = localizeKey(
                "hexcasting.mishap.invalid_value.class."
                    + perpetrator.getType().getId());
            return I18n.translateToLocalFormatted(
                "hexcasting.mishap.invalid_value", expected,
                mishap.getInvalidReverseIndex(), actual, perpetrator.display());
        }

        if (mishap.getKind() == Mishap.Kind.ARITHMETIC
            && mishap.getArithmeticLeft() != null) {
            return localizeArithmeticMishap(mishap);
        }

        String key = mishap.getDisplayKey();
        Object[] displayArgs = mishap.getDisplayArgs();
        if (displayArgs.length > 0) {
            return I18n.translateToLocalFormatted(key, displayArgs);
        }
        if (mishap.getKind() == Mishap.Kind.NOT_ENOUGH_ARGUMENTS) {
            if (mishap.getArgumentsGot() == 0) {
                return I18n.translateToLocalFormatted(key,
                    mishap.getArgumentsExpected());
            }
            return I18n.translateToLocalFormatted(key,
                mishap.getArgumentsExpected(), mishap.getArgumentsGot());
        }
        if (key == null || key.isEmpty() || !key.startsWith("hexcasting.")) {
            return localizeRaw(mishap.getErrorKey());
        }
        return localizeKey(key);
    }

    private static String localizeArithmeticMishap(Mishap mishap) {
        String suffix = mishap.getArithmeticSuffix();
        String left = arithmeticDisplay(mishap.getArithmeticLeft(), false);
        String right = arithmeticDisplay(mishap.getArithmeticRight(),
            "exponent".equals(suffix));
        if ("tan".equals(suffix)) {
            String sine = I18n.translateToLocalFormatted(
                "hexcasting.mishap.divide_by_zero.sin", left);
            String cosine = I18n.translateToLocalFormatted(
                "hexcasting.mishap.divide_by_zero.cos", left);
            return I18n.translateToLocalFormatted(
                "hexcasting.mishap.divide_by_zero.divide", sine, cosine);
        }
        String key = "hexcasting.mishap.divide_by_zero."
            + (suffix == null || suffix.isEmpty() ? "divide" : suffix);
        return I18n.translateToLocalFormatted(key, left, right);
    }

    private static String arithmeticDisplay(Iota value, boolean exponent) {
        if (value == null) {
            return "?";
        }
        if (value instanceof DoubleIota
            && ((DoubleIota) value).getValue() == 0.0D) {
            return I18n.translateToLocal(
                exponent ? "hexcasting.mishap.divide_by_zero.zero.power"
                    : "hexcasting.mishap.divide_by_zero.zero");
        }
        if (value instanceof Vec3Iota
            && ((Vec3Iota) value).getValue().lengthVector() == 0.0D) {
            return I18n.translateToLocal(
                "hexcasting.mishap.divide_by_zero.zero.vec");
        }
        return value.display();
    }

    private static String localizeRaw(String message) {
        if (message == null || message.isEmpty()) {
            return I18n.translateToLocal("hexcasting.message.staff_error");
        }
        String lower = message.toLowerCase(Locale.ROOT);
        String marker = "no action is registered for pattern";
        int markerIndex = lower.indexOf(marker);
        if (markerIndex >= 0) {
            String signature = message.substring(markerIndex + marker.length()).trim();
            try {
                signature = HexInline.formatPattern(
                    at.petra_k.hexcasting.api.casting.math.HexPattern
                        .fromSignature(signature));
            } catch (IllegalArgumentException ignored) {
                // Keep compatibility with older saved/error messages.
            }
            return I18n.translateToLocalFormatted(
                "hexcasting.message.pattern_unregistered", signature);
        }
        return localizeKey(message);
    }

    private static String localizeKey(String key) {
        if (key == null || key.isEmpty()) {
            return I18n.translateToLocal("hexcasting.message.staff_error");
        }
        String translated = I18n.translateToLocal(key);
        return key.equals(translated)
            ? I18n.translateToLocal("hexcasting.message.staff_error") : translated;
    }

    private static String localizeAction(String displayKey, String fallbackId) {
        if (displayKey != null) {
            String translated = I18n.translateToLocal(displayKey);
            if (!displayKey.equals(translated)) {
                return translated;
            }
        }
        if (fallbackId == null || fallbackId.isEmpty()) {
            return null;
        }
        try {
            ResourceLocation id = new ResourceLocation(fallbackId);
            return id.getResourcePath();
        } catch (RuntimeException ignored) {
            return fallbackId;
        }
    }
}
