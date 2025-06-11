package me.deecaad.weaponmechanics.weapon.placeholders;

import me.deecaad.core.placeholder.PlaceholderData;
import me.deecaad.core.placeholder.PlaceholderHandler;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.reload.ammo.AmmoConfig;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PAmmoType extends PlaceholderHandler {

    public PAmmoType() {
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(WeaponMechanics.getInstance(), "ammo_type");
    }

    @Override
    public @Nullable String onRequest(@NotNull PlaceholderData data) {
        if (data.item() == null || data.itemTitle() == null)
            return null;

        AmmoConfig ammoTypes = WeaponMechanics.getInstance().getWeaponConfigurations().getObject(data.itemTitle() + ".Reload.Ammo", AmmoConfig.class);

        // Simply don't show anything
        if (ammoTypes == null)
            return null;

        return ammoTypes.getCurrentAmmo(data.item()).getDisplay();
    }
}
