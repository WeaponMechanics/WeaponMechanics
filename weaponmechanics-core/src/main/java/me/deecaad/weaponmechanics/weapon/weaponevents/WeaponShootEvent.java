package me.deecaad.weaponmechanics.weapon.weaponevents;

import me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile.WeaponProjectile;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This class outlines the event of a weapon launching a projectile.
 */
public class WeaponShootEvent extends WeaponEvent {

    private static final @NotNull HandlerList HANDLERS = new HandlerList();

    private @NotNull WeaponProjectile projectile;

    public WeaponShootEvent(@NotNull WeaponProjectile projectile) {
        super(projectile.getWeaponTitle(), projectile.getWeaponStack(), projectile.getShooter(), projectile.getHand());

        this.projectile = projectile;
    }

    public @NotNull WeaponProjectile getProjectile() {
        return projectile;
    }

    public void setProjectile(@NotNull WeaponProjectile projectile) {
        if (projectile == null)
            throw new IllegalArgumentException("projectile cannot be null");

        this.projectile = projectile;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
