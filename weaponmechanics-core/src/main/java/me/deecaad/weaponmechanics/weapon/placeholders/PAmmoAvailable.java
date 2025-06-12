package me.deecaad.weaponmechanics.weapon.placeholders;

import me.deecaad.core.placeholder.PlaceholderData;
import me.deecaad.core.placeholder.PlaceholderHandler;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.reload.ammo.AmmoConfig;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PAmmoAvailable extends PlaceholderHandler {

    public PAmmoAvailable() {
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(WeaponMechanics.getInstance(), "ammo_available");
    }

    @Override
    public @Nullable String onRequest(@NotNull PlaceholderData data) {
        if (data.item() == null || data.itemTitle() == null)
            return null;

        AmmoConfig ammo = WeaponMechanics.getInstance().getWeaponConfigurations().getObject(data.itemTitle() + ".Reload.Ammo", AmmoConfig.class);

        if (ammo == null)
            return null;

        return String.valueOf(ammo.getMaximumAmmo(data.item(), WeaponMechanics.getInstance().getPlayerWrapper(data.player()), WeaponMechanics.getInstance().getWeaponConfigurations().getInt(data.itemTitle() + ".Reload.Magazine_Size")));
    }
}
