package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.Mishap;
import at.petra_k.hexcasting.api.casting.iota.DoubleIota;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.Vec3Iota;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.lib.hex.BrainsweepRecipes;
import at.petra_k.hexcasting.common.lib.HexSounds;
import at.petra_k.hexcasting.interop.inline.HexInline;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.init.SoundEvents;
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

        // All casting carriers use this boundary, so the failure sound is
        // emitted exactly once even when the VM and the item entry point both
        // report the same Mishap.
        caster.world.playSound(null, caster.posX, caster.posY, caster.posZ,
            HexSounds.CAST_FAILURE, SoundCategory.PLAYERS, 1.0F, 1.0F);

        Entity target = mishap.getTargetEntity();
        switch (mishap.getKind()) {
            case BAD_ITEM:
                if (target instanceof EntityItem) {
                    EntityItem item = (EntityItem) target;
                    item.motionX += (caster.world.rand.nextDouble() - 0.5D) * 0.05D;
                    item.motionY += 0.75D;
                    item.motionZ += (caster.world.rand.nextDouble() - 0.5D) * 0.05D;
                    item.velocityChanged = true;
                }
                return;
            case BAD_OFFHAND_ITEM:
            case LACKING_HOTBAR_ITEM:
                // These are the two modern mishaps whose execute method drops
                // the caster's held stacks.  Keep them separate from BAD_ITEM:
                // an invalid data holder or another item-shaped error must not
                // eject unrelated items from the player's hands.
                dropHeldItems(caster);
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
                // modern MishapUnenlightened side effect.
                yeetHeldItemsTowards(caster,
                    caster.getPositionVector().add(caster.getLookVec()));
                caster.world.playSound(null, caster.posX, caster.posY, caster.posZ,
                    SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS,
                    0.5F, 0.7F);
                return;
            case NO_SPELL_CIRCLE:
                // A circle-only action cast without a circle ejects the
                // player's inventory. Binding-cursed armor remains equipped,
                // matching MishapNoSpellCircle in modern Hex.
                dropInventory(caster);
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

    private static void dropHeldItems(EntityPlayer caster) {
        yeetHeldItemsTowards(caster,
            caster.getPositionVector().add(caster.getLookVec()));
    }

    private static void dropInventory(EntityPlayer caster) {
        dropInventoryList(caster, caster.inventory.mainInventory, false);
        dropInventoryList(caster, caster.inventory.offHandInventory, false);
        dropInventoryList(caster, caster.inventory.armorInventory, true);
    }

    private static void dropInventoryList(EntityPlayer caster,
                                          java.util.List<ItemStack> inventory,
                                          boolean preserveBinding) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.get(i);
            if (stack == null || stack.isEmpty()
                || preserveBinding && EnchantmentHelper.hasBindingCurse(stack)) {
                continue;
            }
            inventory.set(i, ItemStack.EMPTY);
            caster.dropItem(stack, true, false);
        }
    }

    /** Match modern Hex's giveExperiencePoints(-100) through the 1.12 API. */
    private static void removeExperience(EntityPlayer player, int amount) {
        if (player == null || amount <= 0 || player.experienceTotal <= 0) {
            return;
        }
        int removed = Math.min(amount, player.experienceTotal);
        player.addScore(-removed);
        player.experienceTotal -= removed;
        player.experience -= (float) removed / Math.max(1, player.xpBarCap());

        // EntityPlayer.addExperience only handles positive amounts in 1.12.
        // Reproduce modern negative-XP handling so crossing a level boundary
        // preserves the remaining progress instead of making the bar negative.
        while (player.experience < 0.0F) {
            float remaining = player.experience * Math.max(1, player.xpBarCap());
            if (player.experienceLevel > 0) {
                player.addExperienceLevel(-1);
                player.experience = 1.0F + remaining
                    / Math.max(1, player.xpBarCap());
            } else {
                player.addExperienceLevel(-1);
                player.experience = 0.0F;
            }
        }
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
        if (mishap.getKind() == Mishap.Kind.UNESCAPED) {
            Iota perpetrator = mishap.getUnescapedPerpetrator();
            return I18n.translateToLocalFormatted("hexcasting.mishap.unescaped",
                perpetrator == null ? "?" : perpetrator.display());
        }
        if (mishap.getKind() == Mishap.Kind.IMMUNE_ENTITY
            || mishap.getKind() == Mishap.Kind.ENTITY_TOO_FAR) {
            Entity target = mishap.getTargetEntity();
            String name = target == null || target.getDisplayName() == null
                ? "?" : target.getDisplayName().getUnformattedText();
            return I18n.translateToLocalFormatted(mishap.getErrorKey(), name);
        }
        if (mishap.getKind() == Mishap.Kind.BAD_ENTITY) {
            return localizeBadEntity(mishap);
        }
        if (mishap.getKind() == Mishap.Kind.BAD_ITEM
            || mishap.getKind() == Mishap.Kind.BAD_OFFHAND_ITEM
            || mishap.getKind() == Mishap.Kind.LACKING_HOTBAR_ITEM) {
            return localizeBadItem(mishap);
        }
        if (mishap.getKind() == Mishap.Kind.BAD_BLOCK) {
            return localizeBadBlock(mishap);
        }
        if (mishap.getKind() == Mishap.Kind.BAD_LOCATION
            || mishap.getKind() == Mishap.Kind.PERMISSION_DENIED) {
            return localizeBadLocation(mishap);
        }
        if (mishap.getKind() == Mishap.Kind.NO_AKASHIC_RECORD
            && mishap.hasLocationContext()) {
            return I18n.translateToLocalFormatted(
                "hexcasting.mishap.no_akashic_record",
                locationDisplay(mishap));
        }
        if (mishap.getKind() == Mishap.Kind.BAD_BRAINSWEEP
            && mishap.getTargetEntity() != null) {
            return I18n.translateToLocalFormatted(
                "hexcasting.mishap.bad_brainsweep",
                blockDisplayAt(mishap));
        }
        if (mishap.getKind() == Mishap.Kind.ALREADY_BRAINSWEPT) {
            return localizeKey("hexcasting.mishap.already_brainswept");
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
            String expected = localizeInvalidValue(expectedKey, expectedArgs);
            Iota perpetrator = mishap.getInvalidPerpetrator();
            String actual = localizeInvalidValue(
                "hexcasting.mishap.invalid_value.class."
                    + perpetrator.getType().getId(), new Object[0]);
            return localizeFormattedWithFallback(null,
                new String[] {"hexcasting.mishap.invalid_value",
                    "hexcasting.mishap.invalid_value."},
                expected, mishap.getInvalidReverseIndex(), actual,
                perpetrator.display());
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

    /** Localize a modern bad-entity message while retaining old-key fallback. */
    private static String localizeBadEntity(Mishap mishap) {
        Entity target = mishap.getTargetEntity();
        String expectedKey = entityExpectationKey(mishap.getErrorKey());
        if (target == null || expectedKey == null) {
            return localizeKey(mishap.getErrorKey());
        }
        return I18n.translateToLocalFormatted("hexcasting.mishap.bad_entity",
            localizeKey(expectedKey), entityDisplay(target));
    }

    /** Localize item, offhand, and hotbar failures with actual stack data. */
    private static String localizeBadItem(Mishap mishap) {
        String expectedKey = itemExpectationKey(mishap.getErrorKey());
        if (expectedKey == null) {
            return localizeKey(mishap.getErrorKey());
        }
        String expected = expectedKey.endsWith("iota.readonly")
            && mishap.getDisplayArgs().length > 0
            ? I18n.translateToLocalFormatted(expectedKey,
                mishap.getDisplayArgs())
            : localizeKey(expectedKey);
        if (mishap.getKind() == Mishap.Kind.LACKING_HOTBAR_ITEM) {
            return I18n.translateToLocalFormatted(
                "hexcasting.mishap.bad_item.hotbar", expected);
        }

        if (mishap.getKind() == Mishap.Kind.BAD_OFFHAND_ITEM
            && mishap.getOffhandItem() != null) {
            ItemStack held = mishap.getOffhandItem();
            return I18n.translateToLocalFormatted(
                "hexcasting.mishap.bad_item.offhand", expected,
                held.getCount(), held.getDisplayName().getUnformattedText());
        }

        Entity target = mishap.getTargetEntity();
        if (mishap.getKind() == Mishap.Kind.BAD_OFFHAND_ITEM
            || !(target instanceof EntityItem)) {
            return I18n.translateToLocalFormatted(
                "hexcasting.mishap.no_item.offhand", expected);
        }
        ItemStack stack = ((EntityItem) target).getItem();
        if (stack == null || stack.isEmpty()) {
            return I18n.translateToLocalFormatted(
                "hexcasting.mishap.no_item", expected);
        }
        return I18n.translateToLocalFormatted("hexcasting.mishap.bad_item",
            expected, stack.getCount(), stack.getDisplayName().getUnformattedText());
    }

    /** Localize a block failure with the recorded position and actual state. */
    private static String localizeBadBlock(Mishap mishap) {
        String expectedKey = blockExpectationKey(mishap.getErrorKey());
        if (expectedKey == null || !mishap.hasLocationContext()) {
            return localizeKey(mishap.getErrorKey());
        }
        String actual = "?";
        EntityPlayer caster = mishap.getCaster();
        if (caster != null && caster.world != null
            && (mishap.getLocationDimension() == Integer.MIN_VALUE
                || mishap.getLocationDimension() == caster.dimension)) {
            net.minecraft.util.math.BlockPos position = locationBlock(mishap);
            actual = caster.world.getBlockState(position).getBlock()
                .getLocalizedName();
        }
        return I18n.translateToLocalFormatted("hexcasting.mishap.bad_block",
            localizeKey(expectedKey), locationDisplay(mishap), actual);
    }

    /** Localize a range/permission failure using the modern location keys. */
    private static String localizeBadLocation(Mishap mishap) {
        if (!mishap.hasLocationContext()) {
            return localizeKey(mishap.getErrorKey());
        }
        String suffix;
        String key = mishap.getErrorKey() == null ? "" : mishap.getErrorKey();
        if (mishap.getKind() == Mishap.Kind.PERMISSION_DENIED
            || key.endsWith("_forbidden") || key.endsWith("_disallowed")) {
            suffix = "forbidden";
        } else if (key.endsWith("_dimension") || key.contains("dimension")) {
            suffix = "bad_dimension";
        } else if (key.endsWith("_position") || key.contains("out_of_world")) {
            suffix = "out_of_world";
        } else {
            suffix = "too_far";
        }
        String modernKey = "hexcasting.mishap.location_." + suffix;
        return I18n.translateToLocalFormatted(modernKey, locationDisplay(mishap));
    }

    private static String entityDisplay(Entity entity) {
        return entity == null || entity.getDisplayName() == null
            ? "?" : entity.getDisplayName().getUnformattedText();
    }

    private static String locationDisplay(Mishap mishap) {
        return new Vec3Iota(new Vec3d(mishap.getLocationX(),
            mishap.getLocationY(), mishap.getLocationZ())).display();
    }

    private static String blockDisplayAt(Mishap mishap) {
        EntityPlayer caster = mishap.getCaster();
        if (caster != null && caster.world != null && mishap.hasLocationContext()
            && (mishap.getLocationDimension() == Integer.MIN_VALUE
                || mishap.getLocationDimension() == caster.dimension)) {
            return caster.world.getBlockState(locationBlock(mishap)).getBlock()
                .getLocalizedName();
        }
        return entityDisplay(mishap.getTargetEntity());
    }

    private static net.minecraft.util.math.BlockPos locationBlock(Mishap mishap) {
        return new net.minecraft.util.math.BlockPos(
            (int) Math.floor(mishap.getLocationX()),
            (int) Math.floor(mishap.getLocationY()),
            (int) Math.floor(mishap.getLocationZ()));
    }

    private static String entityExpectationKey(String errorKey) {
        if (errorKey == null) {
            return null;
        }
        if (errorKey.endsWith("ignite_target")) {
            return "hexcasting.mishap.invalid_value.class.entity_or_vector";
        }
        if (errorKey.endsWith("potion_target")) {
            return "hexcasting.mishap.invalid_value.class.entity.living";
        }
        if (errorKey.endsWith("flight_target")) {
            return "hexcasting.mishap.invalid_value.class.entity.player";
        }
        if (errorKey.endsWith("recharge_entity")) {
            return "hexcasting.mishap.invalid_value.class.entity.item";
        }
        if (errorKey.endsWith("entity_data_target")) {
            return "hexcasting.mishap.bad_item.iota.read";
        }
        return null;
    }

    private static String itemExpectationKey(String errorKey) {
        if (errorKey == null) {
            return null;
        }
        if (errorKey.endsWith("craft_battery_base")) {
            return "hexcasting.mishap.bad_item.bottle";
        }
        if (errorKey.endsWith("craft_battery_media_item")
            || errorKey.endsWith("craft_battery_media")) {
            return "hexcasting.mishap.bad_item.media_for_battery";
        }
        if (errorKey.endsWith("recharge_item")) {
            return "hexcasting.mishap.bad_item.media";
        }
        if (errorKey.endsWith("recharge_holder")) {
            return "hexcasting.mishap.bad_item.rechargable";
        }
        if (errorKey.endsWith("recharge_full")) {
            return "hexcasting.mishap.bad_item.rechargable";
        }
        if (errorKey.endsWith("erase_holder")) {
            return "hexcasting.mishap.bad_item.eraseable";
        }
        if (errorKey.endsWith("colorize_dye")) {
            return "hexcasting.mishap.bad_item.colorizer";
        }
        if (errorKey.endsWith("cycle_variant_item")) {
            return "hexcasting.mishap.bad_item.variant";
        }
        if (errorKey.endsWith("place_block_item")) {
            return "hexcasting.mishap.bad_item.placeable";
        }
        if (errorKey.endsWith("data_holder_missing")) {
            return "hexcasting.mishap.bad_item.iota";
        }
        if (errorKey.endsWith("data_holder_not_writable")) {
            return "hexcasting.mishap.bad_item.iota.write";
        }
        if (errorKey.endsWith("data_holder_readonly")) {
            return "hexcasting.mishap.bad_item.iota.readonly";
        }
        return null;
    }

    private static String blockExpectationKey(String errorKey) {
        if (errorKey == null) {
            return null;
        }
        if (errorKey.endsWith("edify_sapling")) {
            return "hexcasting.mishap.bad_block.sapling";
        }
        if (errorKey.endsWith("place_block_target")
            || errorKey.endsWith("conjure_block_target")
            || errorKey.endsWith("conjure_light_target")) {
            return "hexcasting.mishap.bad_block.replaceable";
        }
        return null;
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

    /**
     * Resource packs from different Hex versions use both a trailing-dot and
     * a non-trailing-dot spelling for invalid-value keys. Prefer the modern
     * spelling, but keep old packs and the bundled legacy translations usable.
     */
    private static String localizeInvalidValue(String key, Object[] args) {
        if (key == null || key.isEmpty()) {
            return localizeKey(key);
        }
        String legacyKey = key.endsWith(".") ? key : key + ".";
        return localizeFormattedWithFallback(null,
            new String[] {key, legacyKey}, args);
    }

    private static String localizeFormattedWithFallback(String fallback,
                                                         String[] keys,
                                                         Object... args) {
        if (keys != null) {
            for (String key : keys) {
                if (key == null || key.isEmpty()) {
                    continue;
                }
                String translated = I18n.translateToLocal(key);
                if (!key.equals(translated)) {
                    return args == null || args.length == 0
                        ? translated
                        : I18n.translateToLocalFormatted(key, args);
                }
            }
        }
        return fallback == null
            ? localizeKey(keys == null || keys.length == 0 ? null : keys[0])
            : fallback;
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
