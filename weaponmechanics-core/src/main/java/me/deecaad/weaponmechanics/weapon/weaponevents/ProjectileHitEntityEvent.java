package me.deecaad.weaponmechanics.weapon.weaponevents;

import me.deecaad.weaponmechanics.weapon.damage.DamagePoint;
import me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile.WeaponProjectile;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;

/**
 * Called whenever a projectile hits an entity. This may be called multiple times for some
 * projectiles (that use
 * {@link me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile.Through} or
 * {@link me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile.Bouncy}).
 *
 * <p>
 * For more control over the projectile, consider using a
 * {@link me.deecaad.weaponmechanics.weapon.projectile.ProjectileScript} instead.
 */
public class ProjectileHitEntityEvent extends ProjectileEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity entity;
    private final Vector exactLocation;
    private DamagePoint point;
    private boolean isBackStab;
    private boolean isCancelled;

    public ProjectileHitEntityEvent(WeaponProjectile projectile, LivingEntity entity, Vector exactLocation, DamagePoint point, boolean isBackStab) {
        super(projectile);
        this.entity = entity;
        this.exactLocation = exactLocation;
        this.point = point;
        this.isBackStab = isBackStab;
    }

    @NotNull public LivingEntity getEntity() {
        return entity;
    }

    @NotNull public EntityType getEntityType() {
        return entity.getType();
    }

    /**
     * Returns the exact point that the entity was hit.
     *
     * @return The non-null hit location.
     */
    public Location getHitLocation() {
        return exactLocation.toLocation(projectile.getWorld());
    }

    /**
     * Returns the body part of the entity that was hit (head/body/arms/etc.)
     *
     * @return The nullable body part.
     */
    public DamagePoint getPoint() {
        return point;
    }

    public void setPoint(@Nullable DamagePoint point) {
        this.point = point;
    }

    public boolean isBackStab() {
        return isBackStab;
    }

    public void setBackStab(boolean backStab) {
        isBackStab = backStab;
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
