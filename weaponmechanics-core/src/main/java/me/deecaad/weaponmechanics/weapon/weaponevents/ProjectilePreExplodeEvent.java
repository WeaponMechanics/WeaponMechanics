package me.deecaad.weaponmechanics.weapon.weaponevents;

import me.deecaad.weaponmechanics.weapon.explode.Explosion;
import me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile.WeaponProjectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called right before calculations for an explosion occurs. This is useful if you want to:
 * <ul>
 * <li>Completely override everything about an explosion</li>
 * <li>Cancel an explosion early</li>
 * </ul>
 *
 * <p>
 * For more general usage, see {@link ProjectileExplodeEvent}.
 */
public class ProjectilePreExplodeEvent extends ProjectileEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private Explosion explosion;
    private boolean isCancelled;

    public ProjectilePreExplodeEvent(WeaponProjectile projectile, Explosion explosion) {
        super(projectile);
        this.explosion = explosion;
    }

    public Explosion getExplosion() {
        return explosion;
    }

    public void setExplosion(Explosion explosion) {
        if (explosion == null)
            throw new NullPointerException("Explosion can't be null");
        this.explosion = explosion;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    @Override
    @NotNull public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
