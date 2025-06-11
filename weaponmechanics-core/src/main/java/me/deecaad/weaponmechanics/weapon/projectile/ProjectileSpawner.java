package me.deecaad.weaponmechanics.weapon.projectile;

import me.deecaad.core.utils.LogLevel;
import me.deecaad.weaponmechanics.WeaponMechanics;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for adding projectiles into some runnable.
 *
 * <p>
 * The implementation varies depending on the server architecture. Typical Spigot/Paper servers will
 * use 1 runnable to handle all projectile ticking. Folia servers use 1 runnable per projectile
 */
public abstract class ProjectileSpawner {

    protected final Plugin plugin;
    protected final List<ProjectileScriptManager> managers;

    /**
     * Initializes the spawner with the given plugin.
     *
     * @param plugin The non-null plugin used to register runnables
     */
    public ProjectileSpawner(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.managers = new ArrayList<>();
    }

    /**
     * Adds the given projectile manager to the list of managers. This manager will be used to attach
     * scripts to projectiles.
     *
     * @param manager The non-null manager to add
     */
    public void addScriptManager(@NotNull ProjectileScriptManager manager) {
        synchronized (managers) {
            managers.add(manager);
        }
    }

    /**
     * Spawns the given projectile. If the thread has control over the region where the projectile is
     * spawned, the projectile will be ticked instantly.
     *
     * @param projectile The non-null projectile to spawn
     */
    public abstract void spawn(@NotNull AProjectile projectile);

    protected boolean doFirstTick(@NotNull AProjectile projectile) {
        synchronized (managers) {
            for (ProjectileScriptManager manager : managers) {
                manager.attach(projectile);
            }
        }

        try {
            if (projectile.tick()) {
                // if the projectile died on the first tick...
                projectile.remove();
                return true;
            }
        } catch (Throwable ex) {
            projectile.remove();
            WeaponMechanics.getInstance().debugger.warning("An unhandled exception occurred while ticking a projectile", ex);
            return false;
        }
        return false;
    }
}
