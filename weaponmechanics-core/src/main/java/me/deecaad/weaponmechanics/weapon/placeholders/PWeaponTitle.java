package me.deecaad.weaponmechanics.weapon.placeholders;

import me.deecaad.core.placeholder.PlaceholderData;
import me.deecaad.core.placeholder.PlaceholderHandler;
import me.deecaad.weaponmechanics.WeaponMechanics;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;

public class PWeaponTitle extends PlaceholderHandler {

    public PWeaponTitle() {
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(WeaponMechanics.getInstance(), "weapon_title");
    }

    @Override
    public @Nullable String onRequest(@NotNull PlaceholderData data) {
        return data.itemTitle();
    }
}
