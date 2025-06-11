package me.deecaad.weaponmechanics.weapon.placeholders;

import me.deecaad.core.placeholder.PlaceholderHandler;
import me.deecaad.core.placeholder.PlaceholderHandlers;
import org.jetbrains.annotations.NotNull;

/**
 * WeaponMechanics' placeholders, added to {@link PlaceholderHandlers}
 */
public class WeaponPlaceholderHandlers {
    public static final @NotNull PlaceholderHandler AMMO_AVAILABLE = register(new PAmmoAvailable());
    public static final @NotNull PlaceholderHandler AMMO_LEFT = register(new PAmmoLeft());
    public static final @NotNull PlaceholderHandler AMMO_TYPE = register(new PAmmoType());
    public static final @NotNull PlaceholderHandler DURABILITY = register(new PDurability());
    public static final @NotNull PlaceholderHandler FIREARM_STATE = register(new PFirearmState());
    public static final @NotNull PlaceholderHandler MAX_DURABILITY = register(new PMaxDurability());
    public static final @NotNull PlaceholderHandler RELOAD = register(new PReload());
    public static final @NotNull PlaceholderHandler SELECTIVE_FIRE_STATE = register(new PSelectiveFireState());
    public static final @NotNull PlaceholderHandler WEAPON_TITLE = register(new PWeaponTitle());

    private WeaponPlaceholderHandlers() {
    }

    private static @NotNull PlaceholderHandler register(@NotNull PlaceholderHandler handler) {
        PlaceholderHandlers.REGISTRY.add(handler);
        return handler;
    }
}
