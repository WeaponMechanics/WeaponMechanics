package me.deecaad.weaponmechanics.weapon.placeholders;

import me.deecaad.core.placeholder.PlaceholderData;
import me.deecaad.core.placeholder.PlaceholderHandler;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.wrappers.PlayerWrapper;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;

public class PReload extends PlaceholderHandler {

    public PReload() {
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(WeaponMechanics.getInstance(), "reload");
    }

    @Override
    public @Nullable String onRequest(@NotNull PlaceholderData data) {
        if (data.player() == null || data.slot() == null)
            return null;

        PlayerWrapper playerWrapper = WeaponMechanics.getInstance().getPlayerWrapper(data.player());
        if (data.slot() == EquipmentSlot.HAND ? playerWrapper.getMainHandData().isReloading() : playerWrapper.getOffHandData().isReloading()) {
            return WeaponMechanics.getInstance().getConfiguration().getString("Placeholder_Symbols.Reload");
        }

        return "";
    }
}
