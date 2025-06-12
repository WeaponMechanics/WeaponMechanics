package me.deecaad.weaponmechanics.weapon.placeholders;

import me.deecaad.core.placeholder.NumericPlaceholderHandler;
import me.deecaad.core.placeholder.PlaceholderData;
import me.deecaad.weaponmechanics.WeaponMechanics;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PMaxDurability extends NumericPlaceholderHandler {

    public PMaxDurability() {
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(WeaponMechanics.getInstance(), "max_durability");
    }

    @Override
    public @Nullable Number requestValue(@NotNull PlaceholderData data) {
        ItemStack item = data.item();
        if (item == null)
            return null;

        if (item.getItemMeta() instanceof Damageable damageable && damageable.hasMaxDamage()) {
            return damageable.getMaxDamage();
        }

        return null;
    }
}
