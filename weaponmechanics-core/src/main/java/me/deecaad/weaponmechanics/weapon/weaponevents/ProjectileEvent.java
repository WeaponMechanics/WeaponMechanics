package me.deecaad.weaponmechanics.weapon.weaponevents;

import me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile.WeaponProjectile;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * This class outlines convenient methods for any {@link WeaponEvent} that involves an
 * {@link WeaponProjectile}.
 */
public abstract class ProjectileEvent extends WeaponEvent {

    protected final WeaponProjectile projectile;

    protected ProjectileEvent(WeaponProjectile projectile) {
        super(projectile.getWeaponTitle(), projectile.getWeaponStack(), projectile.getShooter(), projectile.getHand());

        this.projectile = projectile;
    }

    /**
     * Returns the projectile involved in this event.
     *
     * @return The non-null projectile.
     */
    public WeaponProjectile getProjectile() {
        return projectile;
    }

    /**
     * Returns the weapon item which caused the event. This should always be an item in the player's
     * main hand, or off hand. If API is used to shoot projectile this will be null.
     *
     * @return The nullable weapon item.
     */
    @Nullable @Override
    public ItemStack getWeaponStack() {
        return super.getWeaponStack();
    }

    /**
     * Returns the current location of the projectile.
     *
     * @return The non-null location of the projectile
     */
    public Location getLocation() {
        return projectile.getLocation().toLocation(projectile.getWorld());
    }

    /**
     * Returns the location of the projectile before it's most recent tick.
     *
     * @return The non-null location the projectile was 1 tick ago.
     */
    public Location getLastLocation() {
        return projectile.getLastLocation().toLocation(projectile.getWorld());
    }
}
