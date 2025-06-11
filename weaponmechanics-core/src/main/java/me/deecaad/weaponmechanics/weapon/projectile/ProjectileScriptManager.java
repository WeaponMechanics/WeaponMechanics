package me.deecaad.weaponmechanics.weapon.projectile;

import me.deecaad.weaponmechanics.WeaponMechanics;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public abstract class ProjectileScriptManager {

    private final Plugin plugin;

    public ProjectileScriptManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public abstract void attach(@NotNull AProjectile projectile);

    public void register() {
        WeaponMechanics.getInstance().getProjectileSpawner().addScriptManager(this);
    }
}
