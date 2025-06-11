package me.deecaad.weaponmechanics.weapon.placeholders;

import me.deecaad.core.placeholder.PlaceholderData;
import me.deecaad.core.placeholder.PlaceholderHandler;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.utils.CustomTag;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;

public class PAmmoLeft extends PlaceholderHandler {

    public PAmmoLeft() {
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(WeaponMechanics.getInstance(), "ammo_left");
    }

    @Override
    public @Nullable String onRequest(@NotNull PlaceholderData data) {
        if (data.item() == null)
            return null;

        return String.valueOf(CustomTag.AMMO_LEFT.getInteger(data.item()));
    }
}
