package at.petra_k.hexcasting.mixin;

import at.petra_k.hexcasting.api.HexAPI;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Resolves Hex item names against the legacy 1.12 {@code .name} keys.
 *
 * <p>The migrated language files contain both the modern base keys and the
 * legacy item keys. Vanilla 1.12 can otherwise translate the base key first
 * and append {@code .name} to the translated text, producing names such as
 * {@code Charged Amethyst.name}.</p>
 */
@Mixin(Item.class)
public abstract class MixinHexItemDisplayName {
    @Inject(method = "getItemStackDisplayName", at = @At("HEAD"),
        cancellable = true)
    private void hexcasting$resolveLegacyName(
        ItemStack stack,
        CallbackInfoReturnable<String> cir
    ) {
        Item item = (Item) (Object) this;
        ResourceLocation registryName = item.getRegistryName();
        if (registryName == null
            || !HexAPI.MOD_ID.equals(registryName.getResourceDomain())) {
            return;
        }

        String key = item.getUnlocalizedName(stack) + ".name";
        String translated = I18n.translateToLocal(key);
        if (!key.equals(translated)) {
            cir.setReturnValue(translated.trim());
        }
    }
}
