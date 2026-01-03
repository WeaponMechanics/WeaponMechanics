package me.deecaad.weaponmechanics;

import com.cjcrafter.foliascheduler.TaskImplementation;
import com.cjcrafter.foliascheduler.util.ConstructorInvoker;
import com.cjcrafter.foliascheduler.util.ReflectionUtil;
import com.cjcrafter.foliascheduler.util.ServerVersions;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.EventManager;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import kotlin.UninitializedPropertyAccessException;
import me.deecaad.core.MechanicsCore;
import me.deecaad.core.MechanicsPlugin;
import me.deecaad.core.compatibility.CompatibilityAPI;
import me.deecaad.core.compatibility.worldguard.WorldGuardCompatibility;
import me.deecaad.core.database.Database;
import me.deecaad.core.database.MySQL;
import me.deecaad.core.database.SQLite;
import me.deecaad.core.events.QueueSerializerEvent;
import me.deecaad.core.file.*;
import me.deecaad.core.mechanics.Conditions;
import me.deecaad.core.mechanics.Mechanics;
import me.deecaad.core.mechanics.Targeters;
import me.deecaad.core.mechanics.conditions.Condition;
import me.deecaad.core.mechanics.defaultmechanics.Mechanic;
import me.deecaad.core.mechanics.targeters.Targeter;
import me.deecaad.core.utils.FileUtil;
import me.deecaad.weaponmechanics.lib.MythicMobsLoader;
import me.deecaad.weaponmechanics.listeners.ExplosionInteractionListeners;
import me.deecaad.weaponmechanics.listeners.ResourcePackListener;
import me.deecaad.weaponmechanics.listeners.WeaponListeners;
import me.deecaad.weaponmechanics.listeners.trigger.TriggerEntityListeners;
import me.deecaad.weaponmechanics.listeners.trigger.TriggerPlayerListeners;
import me.deecaad.weaponmechanics.packetlisteners.OutAbilitiesListener;
import me.deecaad.weaponmechanics.packetlisteners.OutEntityEffectListener;
import me.deecaad.weaponmechanics.packetlisteners.OutRemoveEntityEffectListener;
import me.deecaad.weaponmechanics.packetlisteners.OutSetSlotBobFix;
import me.deecaad.weaponmechanics.weapon.WeaponHandler;
import me.deecaad.weaponmechanics.weapon.WeaponSerializer;
import me.deecaad.weaponmechanics.weapon.damage.BlockDamageData;
import me.deecaad.weaponmechanics.weapon.placeholders.WeaponPlaceholderHandlers;
import me.deecaad.weaponmechanics.weapon.projectile.FoliaProjectileSpawner;
import me.deecaad.weaponmechanics.weapon.projectile.ProjectileSpawner;
import me.deecaad.weaponmechanics.weapon.projectile.SpigotProjectileSpawner;
import me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile.Projectile;
import me.deecaad.weaponmechanics.weapon.reload.ammo.Ammo;
import me.deecaad.weaponmechanics.weapon.stats.PlayerStat;
import me.deecaad.weaponmechanics.weapon.stats.WeaponStat;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import me.deecaad.weaponmechanics.wrappers.PlayerWrapper;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.jar.JarFile;

public class WeaponMechanics extends MechanicsPlugin {

    private static @Nullable WeaponMechanics INSTANCE;

    private final @NotNull Map<LivingEntity, EntityWrapper> entityWrappers = new HashMap<>();
    private @Nullable WeaponHandler weaponHandler;
    private @Nullable ResourcePackListener resourcePackListener;
    private @Nullable ProjectileSpawner projectileSpawner;
    private @Nullable Database database;

    private @NotNull Configuration ammoConfigurations = new FastConfiguration();
    private @NotNull Configuration projectileConfigurations = new FastConfiguration();
    private @NotNull Configuration weaponConfigurations = new FastConfiguration();

    public WeaponMechanics() {
        super(Style.style(NamedTextColor.GOLD), Style.style(NamedTextColor.GRAY), 14323);
    }

    @Override
    public void onLoad() {
        INSTANCE = this;
        super.onLoad();

        // Make sure our "extension" registries are loaded (these statements have the side effect
        // of loading the class)
        WeaponPlaceholderHandlers.WEAPON_TITLE.getKey();

        // Register all WorldGuard flags
        WorldGuardCompatibility guard = CompatibilityAPI.getWorldGuardCompatibility();
        if (guard.isInstalled()) {
            debugger.info("Detected WorldGuard, registering flags");
            guard.registerFlag("weapon-shoot", WorldGuardCompatibility.FlagType.STATE_FLAG);
            guard.registerFlag("weapon-shoot-message", WorldGuardCompatibility.FlagType.STRING_FLAG);
            guard.registerFlag("weapon-explode", WorldGuardCompatibility.FlagType.STATE_FLAG);
            guard.registerFlag("weapon-explode-message", WorldGuardCompatibility.FlagType.STRING_FLAG);
            guard.registerFlag("weapon-break-block", WorldGuardCompatibility.FlagType.STATE_FLAG);
            guard.registerFlag("weapon-damage", WorldGuardCompatibility.FlagType.STATE_FLAG);
            guard.registerFlag("weapon-damage-message", WorldGuardCompatibility.FlagType.STRING_FLAG);
        } else {
            debugger.fine("No WorldGuard detected!");
        }

        try {
            JarSearcher searcher = new JarSearcher(new JarFile(getFile()));
            searcher.findAllSubclasses(Mechanic.class, getClassLoader(), SearchMode.ON_DEMAND)
                    .stream()
                    .map(ReflectionUtil::getConstructor)
                    .map(ConstructorInvoker::newInstance)
                    .forEach(Mechanics.REGISTRY::add);
            searcher.findAllSubclasses(Targeter.class, getClassLoader(), SearchMode.ON_DEMAND)
                    .stream()
                    .map(ReflectionUtil::getConstructor)
                    .map(ConstructorInvoker::newInstance)
                    .forEach(Targeters.REGISTRY::add);
            searcher.findAllSubclasses(Condition.class, getClassLoader(), SearchMode.ON_DEMAND)
                    .stream()
                    .map(ReflectionUtil::getConstructor)
                    .map(ConstructorInvoker::newInstance)
                    .forEach(Conditions.REGISTRY::add);
        } catch (Throwable ex) {
            debugger.severe("Failed to load mechanics/targeters/conditions", ex);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();

        handleDatabase();
        handleResourcePack();  // no need to .join() on this... let it run in background

        // Shameless self-promotion
        if (Bukkit.getPluginManager().getPlugin("WeaponMechanicsCosmetics") == null)
            debugger.info("Buy WeaponMechanicsCosmetics to support our development: https://pluginify.org/resources/13/");
        if (Bukkit.getPluginManager().getPlugin("WeaponMechanicsPlus") == null)
            debugger.info("Buy WeaponMechanicsPlus to support our development: https://pluginify.org/resources/14/");
    }

    @Override
    public @NotNull CompletableFuture<Void> handleCommands() {
        WeaponMechanicsCommand.registerCommands();
        return super.handleCommands();
    }

    public @NotNull CompletableFuture<Void> handleResourcePack() {
        if (!getConfig().getBoolean("Resource_Pack_Download.Enabled"))
            return CompletableFuture.completedFuture(null);

        File pack = new File(getDataFolder(), "WeaponMechanicsResourcePack.zip");
        if (pack.exists())
            return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            String link = configuration.getString("Resource_Pack_Download.Link");
            int connectionTimeout = configuration.getInt("Resource_Pack_Download.Connection_Timeout");
            int readTimeout = configuration.getInt("Resource_Pack_Download.Read_Timeout");

            ResourcePackListener listener = this.resourcePackListener;
            if (listener == null) {
                debugger.warning("ResourcePackListener is not initialized! Cannot download resource pack.");
                return;
            }

            try {
                if ("LATEST".equals(link))
                    link = resourcePackListener.getResourcePackLink();

                FileUtil.downloadFile(pack, link, connectionTimeout, readTimeout);
            } catch (Exception e) {
                // Wrapping in CompletionException causes the returned CompletableFuture to complete exceptionally.
                throw new CompletionException("Error downloading resource pack from " + link, e);
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Void> handleConfigs() {
        // Look for all serializers/validators in the jar
        List<Serializer<?>> serializers;
        List<IValidator> validators;
        try {
            serializers = new SerializerInstancer(new JarFile(getFile())).createAllInstances(getClassLoader(), SearchMode.ON_DEMAND);
            validators = new JarInstancer(new JarFile(getFile())).createAllInstances(IValidator.class, getClassLoader(), SearchMode.ON_DEMAND);
        } catch (IOException ex) {
            debugger.severe("Failed to load validators/serializers", ex);
            return CompletableFuture.completedFuture(null);
        }

        // Let other plugins register serializers/validators
        QueueSerializerEvent event = new QueueSerializerEvent(this, getDataFolder());
        event.addSerializers(serializers);
        event.addValidators(validators);
        Bukkit.getPluginManager().callEvent(event);
        serializers = event.getSerializers();
        validators = event.getValidators();

        ammoConfigurations = new RootFileReader<>(this, Ammo.class, "ammos")
                .withSerializers(serializers)
                .withValidators(validators)
                .assertFiles()
                .read();
        long ammos = ammoConfigurations.values().stream().filter(Ammo.class::isInstance).count();
        debugger.info("Loaded " + ammos + " ammos");

        projectileConfigurations = new RootFileReader<>(this, Projectile.class, "projectiles")
                .withSerializers(serializers)
                .withValidators(validators)
                .assertFiles()
                .read();
        long projectiles = projectileConfigurations.values().stream().filter(Projectile.class::isInstance).count();
        debugger.info("Loaded " + projectiles + " projectiles");

        weaponConfigurations = new RootFileReader<>(this, WeaponSerializer.class, "weapons")
                .withSerializers(serializers)
                .withValidators(validators)
                .assertFiles()
                .read();
        long weapons = weaponConfigurations.values().stream().filter(WeaponSerializer.class::isInstance).count();
        debugger.info("Loaded " + weapons + " weapons");

        return CompletableFuture.completedFuture(null);
    }

    public @NotNull CompletableFuture<Void> handleDatabase() {
        if (!configuration.getBoolean("Database.Enable", true))
            return CompletableFuture.completedFuture(null);

        debugger.fine("Setting up database");
        String databaseType = configuration.getString("Database.Type", "SQLITE");
        if ("SQLITE".equalsIgnoreCase(databaseType)) {
            String absolutePath = configuration.getString("Database.SQLite.Absolute_Path", "plugins/WeaponMechanics/weaponmechanics.db");
            try {
                database = new SQLite(absolutePath);
            } catch (IOException | SQLException e) {
                debugger.warning("Failed to initialized database!", e);
                return CompletableFuture.failedFuture(new CompletionException("Failed to initialize SQLite database", e));
            }
        } else {
            String hostname = configuration.getString("Database.MySQL.Hostname", "localhost");
            int port = configuration.getInt("Database.MySQL.Port", 3306);
            String databaseName = configuration.getString("Database.MySQL.Database", "weaponmechanics");
            String username = configuration.getString("Database.MySQL.Username", "root");
            String password = configuration.getString("Database.MySQL.Password", "");
            database = new MySQL(hostname, port, databaseName, username, password);
        }
        database.executeUpdate(true, PlayerStat.getCreateTableString(), WeaponStat.getCreateTableString());
        return CompletableFuture.completedFuture(null);
    }

    @NotNull
    @Override
    public CompletableFuture<Void> handleListeners() {
        debugger.fine("Registering listeners");

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new TriggerPlayerListeners(weaponHandler), this);
        pm.registerEvents(new TriggerEntityListeners(weaponHandler), this);
        pm.registerEvents(new WeaponListeners(weaponHandler), this);
        pm.registerEvents(new ExplosionInteractionListeners(), this);

        if (resourcePackListener == null) {
            debugger.warning("Failed to register ResourcePackListener... users will not automatically download the resource pack!");
        } else {
            pm.registerEvents(resourcePackListener, this);
        }

        if (pm.isPluginEnabled("MythicMobs")) {
            PluginDescriptionFile desc = pm.getPlugin("MythicMobs").getDescription();
            if (!desc.getVersion().split("\\.")[0].contains("5")) {
                debugger.warning("Could not hook into MythicMobs because it is outdated");
            } else {
                pm.registerEvents(new MythicMobsLoader(), this);
                debugger.info("Hooked in MythicMobs " + desc.getVersion());
            }
        }

        return super.handleListeners();
    }

    @Override
    public @NotNull CompletableFuture<Void> handlePacketListeners() {
        debugger.fine("Creating packet listeners");

        EventManager em = PacketEvents.getAPI().getEventManager();
        em.registerListener(new OutAbilitiesListener(), PacketListenerPriority.NORMAL);
        em.registerListener(new OutEntityEffectListener(), PacketListenerPriority.NORMAL);
        em.registerListener(new OutRemoveEntityEffectListener(), PacketListenerPriority.NORMAL);
        if (configuration.getBoolean("Fix_Bobbing_Legacy", false)) {
            debugger.info("Enabling legacy bobbing fix for clients older than 1.21.4");
            em.registerListener(new OutSetSlotBobFix(this), PacketListenerPriority.NORMAL);
        }
        return super.handlePacketListeners();
    }

    private int permissionFails;

    @Override
    public @NotNull CompletableFuture<Void> handlePermissions() {
        debugger.fine("Registering permissions");

        Permission parent = Bukkit.getPluginManager().getPermission("weaponmechanics.use.*");
        if (parent == null) {
            // Some older versions register permissions after onEnable...
            if (permissionFails++ > 1)
                debugger.warning("Could not register permissions... Waiting 1 tick and trying again.");
            return super.handlePermissions();
        }

        permissionFails = 0;

        for (String weaponTitle : weaponHandler.getInfoHandler().getSortedWeaponList()) {
            String permissionName = "weaponmechanics.use." + weaponTitle;
            Permission permission = Bukkit.getPluginManager().getPermission(permissionName);

            if (permission == null) {
                permission = new Permission(permissionName, "Permission to use " + weaponTitle);
                Bukkit.getPluginManager().addPermission(permission);
            }

            permission.addParent(parent, true);
        }
        return super.handlePermissions();
    }

    @Override
    public @NotNull CompletableFuture<Void> handleMetrics() {
        Metrics metrics = getMetrics();
        if (metrics == null) {
            return CompletableFuture.completedFuture(null);
        }

        debugger.fine("Registering bStats");

        // Tracks the number of weapons that are used in the plugin. Since each
        // server uses a relatively random number of weapons, we should track
        // ranges of weapons (As in, <10, >10 & <20, >20 & <30, etc). This way,
        // the pie chart will look tolerable.
        // https://bstats.org/help/custom-charts
        metrics.addCustomChart(new SimplePie("registered_weapons", () -> {
            int weapons = weaponHandler.getInfoHandler().getSortedWeaponList().size();

            if (weapons <= 10) {
                return "0-10";
            } else if (weapons <= 20) {
                return "11-20";
            } else if (weapons <= 30) {
                return "21-30";
            } else if (weapons <= 50) {
                return "31-50";
            } else if (weapons <= 100) {
                return "51-100";
            } else {
                return ">100";
            }
        }));

        metrics.addCustomChart(new SimplePie("core_version", () -> MechanicsCore.getInstance().getDescription().getVersion()));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void init() {
        super.init();

        // Cancel running tasks for wrappers before clearing (defensive redundancy)
        for (EntityWrapper ew : entityWrappers.values()) {
            TaskImplementation<Void> moveTask = ew.getMoveTask();
            if (moveTask != null) moveTask.cancel();
            ew.getMainHandData().cancelTasks();
            ew.getOffHandData().cancelTasks();
        }
        entityWrappers.clear();

        // Initialize core objects (recreate to ensure a clean state)
        weaponHandler = new WeaponHandler();
        resourcePackListener = new ResourcePackListener();
        if (ServerVersions.isFolia()) {
            projectileSpawner = new FoliaProjectileSpawner(this);
        } else {
            projectileSpawner = new SpigotProjectileSpawner(this);
        }
    }


    @Override
    public @NotNull CompletableFuture<TaskImplementation<Void>> reload() {
        for (EntityWrapper entity : entityWrappers.values()) {
            TaskImplementation<Void> moveTask = entity.getMoveTask();
            if (moveTask != null)
                moveTask.cancel();
        }

        return MechanicsCore.getInstance().reload()
                .thenCompose((ignore) -> super.reload())
                .thenCompose((ignore) -> handleDatabase())
                .thenCompose((ignore) -> {
                    if (weaponHandler == null) {
                        debugger.severe("WeaponHandler is null after reload! This should never happen.");
                        return CompletableFuture.failedFuture(new IllegalStateException("WeaponHandler is null after reload!"));
                    }

                    // Make sure each online player has a wrapper
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        PlayerWrapper playerWrapper = getPlayerWrapper(player);
                        weaponHandler.getStatsHandler().load(playerWrapper);
                    }
                    return CompletableFuture.completedFuture(null);
                });
    }

    @Override
    public void onDisable() {
        // Try to unregister events, just in case this is a reload and not a
        // full plugin disable (in which case this would be done already).
        HandlerList.unregisterAll(this);

        // This won't cancel move tasks on Folia... We have to do it manually
        foliaScheduler.cancelTasks();
        for (EntityWrapper entityWrapper : entityWrappers.values()) {
            TaskImplementation<Void> moveTask = entityWrapper.getMoveTask();
            if (moveTask != null)
                moveTask.cancel();
            entityWrapper.getMainHandData().cancelTasks();
            entityWrapper.getOffHandData().cancelTasks();
        }

        BlockDamageData.regenerateAll();

        // Close database and save data in SYNC
        if (database != null && weaponHandler != null) {
            for (EntityWrapper entityWrapper : entityWrappers.values()) {
                if (!entityWrapper.isPlayer())
                    continue;
                weaponHandler.getStatsHandler().save((PlayerWrapper) entityWrapper, true);
            }
            try {
                database.close();
            } catch (SQLException e) {
                debugger.warning("Couldn't close database properly...", e);
            }
        }

        database = null;
        weaponHandler = null;
        entityWrappers.clear();
        weaponConfigurations.clear();
        ammoConfigurations.clear();
        projectileConfigurations.clear();
        projectileSpawner = null;
    }

    public @NotNull WeaponHandler getWeaponHandler() {
        if (weaponHandler == null)
            throw new UninitializedPropertyAccessException();
        return weaponHandler;
    }

    public @NotNull Database getDatabase() {
        if (database == null)
            throw new UninitializedPropertyAccessException();
        return database;
    }

    public @NotNull ProjectileSpawner getProjectileSpawner() {
        if (projectileSpawner == null)
            throw new UninitializedPropertyAccessException();
        return projectileSpawner;
    }

    public @NotNull Configuration getAmmoConfigurations() {
        return ammoConfigurations;
    }

    public @NotNull Configuration getProjectileConfigurations() {
        return projectileConfigurations;
    }

    public @NotNull Configuration getWeaponConfigurations() {
        return weaponConfigurations;
    }

    public static @NotNull WeaponMechanics getInstance() {
        return INSTANCE;
    }

    /**
     * This method can't return null because new EntityWrapper is created if not found.
     *
     * @param entity the entity wrapper to get
     * @return the entity wrapper
     */
    public @NotNull EntityWrapper getEntityWrapper(@NotNull LivingEntity entity) {
        if (entity.getType() == EntityType.PLAYER) {
            return getPlayerWrapper((Player) entity);
        }
        return getEntityWrapper(entity, false);
    }

    /**
     * This method will return null if no auto add is set to true and EntityWrapper is not found. If no
     * auto add is false then new EntityWrapper is automatically created if not found and returned by
     * this method.
     *
     * @param entity    the entity
     * @param noAutoAdd true means that EntityWrapper wont be automatically added if not found
     * @return the entity wrapper or null if no auto add is true and EntityWrapper was not found
     */
    public @Nullable EntityWrapper getEntityWrapper(@NotNull LivingEntity entity, boolean noAutoAdd) {
        if (entity.getType() == EntityType.PLAYER) {
            return getPlayerWrapper((Player) entity);
        }
        EntityWrapper wrapper = entityWrappers.get(entity);
        if (wrapper == null) {
            if (noAutoAdd) {
                return null;
            }
            wrapper = new EntityWrapper(entity);
            entityWrappers.put(entity, wrapper);
        }
        return wrapper;
    }

    /**
     * This method can't return null because new PlayerWrapper is created if not found. Use mainly
     * getEntityWrapper() instead of this unless you especially need something from PlayerWrapper.
     *
     * @param player the player wrapper to get
     * @return the player wrapper
     */
    public @NotNull PlayerWrapper getPlayerWrapper(@NotNull Player player) {
        EntityWrapper wrapper = entityWrappers.get(player);
        if (wrapper == null) {
            wrapper = new PlayerWrapper(player);
            entityWrappers.put(player, wrapper);
        }
        if (!(wrapper instanceof PlayerWrapper)) {
            // Exception is better in this case as we need to know where this mistake happened
            throw new IllegalArgumentException("Tried to get PlayerWrapper from player which didn't have PlayerWrapper (only EntityWrapper)...?");
        }
        return (PlayerWrapper) wrapper;
    }

    /**
     * Removes entity (and player) wrapper and all of its content. Move task is also cancelled.
     *
     * @param entity the entity (or player)
     */
    public void removeEntityWrapper(@NotNull LivingEntity entity) {
        EntityWrapper oldWrapper = entityWrappers.remove(entity);
        if (oldWrapper != null) {
            TaskImplementation<Void> oldMoveTask = oldWrapper.getMoveTask();
            if (oldMoveTask != null) {
                oldMoveTask.cancel();
            }
            oldWrapper.getMainHandData().cancelTasks();
            oldWrapper.getOffHandData().cancelTasks();
        }
    }
}
