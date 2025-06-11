package me.deecaad.weaponmechanics.weapon.projectile;

import me.deecaad.core.utils.ray.RayTraceResult;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * A projectile script is a class that can be attached to a projectile. The script can listen for
 * projectile related events (colliding with blocks, for example). The {@link AProjectile} class
 * stores a list of scripts, and each script is updated for each event.
 *
 * <p>
 * In order for you to attach a script to a projectile, you need to register your
 * {@link ProjectileScriptManager} (which handles which projectiles to use) using
 * {@link ProjectileSpawner#addScriptManager(ProjectileScriptManager)}.
 *
 * <p>
 * While you can use bukkit events and scripts interchangeably, scripts have the added advantage of
 * better OOP and less overhead from the EventHandler. Scripts may also allow better control over
 * when you want code to execute.
 *
 * @param <T> The projectile type the script expects.
 */
public abstract class ProjectileScript<T extends AProjectile> {

    private final @NotNull Plugin owner;
    protected final @NotNull T projectile;
    protected boolean removeScript;
    protected boolean removeProjectile;

    public ProjectileScript(@NotNull Plugin owner, @NotNull T projectile) {
        this.owner = owner;
        this.projectile = projectile;
        this.removeScript = false;
        this.removeProjectile = false;
    }

    @NotNull public Plugin getOwner() {
        return owner;
    }

    @NotNull public T getProjectile() {
        return projectile;
    }

    /**
     * Each time a projectile iterates through its list of scripts, it will check this method
     * <i>BEFORE</i> calling any api methods [{@link #onStart()}, {@link #onTickStart()},
     * {@link #onTickEnd()}, {@link #onEnd()}, {@link #onCollide(RayTraceResult)}]. This can be used to
     * remove a script from a projectile without removing the projectile.
     *
     * @return <code>true</code> will remove the script from the projectile.
     */
    public boolean isRemoveScript() {
        return removeScript;
    }

    /**
     * Each time a projectile iterates through its list of scripts, it will check this method
     * <i>AFTER</i> calling any api methods [{@link #onStart()}, {@link #onTickStart()},
     * {@link #onTickEnd()}, {@link #onEnd()}, {@link #onCollide(RayTraceResult)}]. This can be used to
     * completely remove a projectile.
     *
     * @return <code>true</code> will remove the projectile.
     */
    public boolean isRemoveProjectile() {
        return removeProjectile;
    }

    /**
     * Called while the projectile is spawning. It is guaranteed that this method will be called before
     * all other methods in this class. This method is called after the projectile is shot, so getting
     * the shooter (for WeaponProjectile) is a safe operation.
     */
    public void onStart() {
    }

    /**
     * Called towards the beginning of {@link AProjectile#tick()}. This method <i>probably</i> should be
     * used instead of {@link #onTickEnd()} when you are modifying projectile values (Changing
     * speed/location, for example).
     */
    public void onTickStart() {
    }

    /**
     * Called towards the end of {@link AProjectile#tick()}. This method <i>probably</i> should be used
     * instead of {@link #onTickStart()} when you only want to read the result of a tick method (See how
     * a projectile moved, for example).
     */
    public void onTickEnd() {
    }

    /**
     * Called when the projectile is removed.
     */
    public void onEnd() {
    }

    /**
     * Called when the projectile collides with a block or living entity.
     *
     * @param hit The non-null ray trace result of block or living entity.
     */
    public void onCollide(@NotNull RayTraceResult hit) {
    }
}
