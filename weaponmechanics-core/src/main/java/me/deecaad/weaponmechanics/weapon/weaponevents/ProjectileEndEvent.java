package me.deecaad.weaponmechanics.weapon.weaponevents;

import me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile.WeaponProjectile;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called whenever a {@link WeaponProjectile} ends. A projectile may end due to flying too high in
 * the sky, flying too low in the void, entering an unloaded chunk, if 30 seconds have passed (since
 * the projectile was launched), after hitting blocks or entities, etc.
 *
 * <p>
 * For more control over the projectile, consider using a
 * {@link me.deecaad.weaponmechanics.weapon.projectile.ProjectileScript} instead.
 */
public class ProjectileEndEvent extends ProjectileEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public ProjectileEndEvent(WeaponProjectile projectile) {
        super(projectile);
    }

    @Override
    @NotNull public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
