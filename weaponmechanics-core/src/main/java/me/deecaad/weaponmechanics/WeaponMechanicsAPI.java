package me.deecaad.weaponmechanics;

import me.deecaad.core.compatibility.CompatibilityAPI;
import me.deecaad.core.compatibility.ICompatibility;
import me.deecaad.weaponmechanics.compatibility.IWeaponCompatibility;
import me.deecaad.weaponmechanics.compatibility.WeaponCompatibilityAPI;
import me.deecaad.weaponmechanics.utils.CustomTag;
import me.deecaad.weaponmechanics.weapon.damage.BlockDamageData;
import me.deecaad.weaponmechanics.weapon.projectile.AProjectile;
import me.deecaad.weaponmechanics.weapon.reload.ReloadHandler;
import me.deecaad.weaponmechanics.weapon.reload.ammo.Ammo;
import me.deecaad.weaponmechanics.weapon.reload.ammo.AmmoConfig;
import me.deecaad.weaponmechanics.weapon.reload.ammo.ItemAmmo;
import me.deecaad.weaponmechanics.weapon.shoot.FullAutoTask;
import me.deecaad.weaponmechanics.weapon.skin.SkinHandler;
import me.deecaad.weaponmechanics.weapon.skin.SkinSelector;
import me.deecaad.weaponmechanics.weapon.stats.WeaponStat;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import me.deecaad.weaponmechanics.wrappers.HandData;
import me.deecaad.weaponmechanics.wrappers.PlayerWrapper;
import me.deecaad.weaponmechanics.wrappers.StatsData;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * This class outlines static utility methods to help developers find functions of WeaponMechanics
 * wrapped in one place. The following method's implementations exclusively call "internal" methods
 * to handle the function.
 *
 * <p>
 * The methods in this class are designed for "general use." If you are looking for a more specific
 * method, you should look at the implementation of the API method, and call the internal methods
 * instead.
 *
 * <p>
 * Are we missing a method you want? No problem, simply open an issue on GitHub, and we will add it!
 */
public final class WeaponMechanicsAPI {

    // Don't let anyone instantiate this class
    private WeaponMechanicsAPI() {
    }

    /**
     * Gets the stats for the given player, or null if the stats have not been loaded (Which can happen
     * randomly if the database falls out of sync, during reloads, player has never joined before, etc.)
     *
     * @param player The non-null player to get the stats for.
     * @return The nullable stats.
     */
    public static @Nullable StatsData getStats(@NotNull Player player) {
        return WeaponMechanics.getInstance().getPlayerWrapper(player).getStatsData();
    }

    /**
     * Gets the skin that will be applied to the weapon item for the given player. This takes the skin
     * override ({@link #setSkin(ItemStack, String)}) into account.
     *
     * <p>
     * This method will throw an exception if the weapon does not use skins.
     *
     * @param player The player holding the weapon.
     * @param weaponStack The weapon item to get the skin for.
     * @return Which skin should be applied, or "default" for no skin.
     */
    public static @NotNull String getSkinFor(@NotNull Player player, @NotNull ItemStack weaponStack) {
        String weaponTitle = getWeaponTitle(weaponStack);
        if (weaponTitle == null) {
            throw new IllegalArgumentException("Item is not a weapon");
        }

        // Check if the weapon uses skins
        SkinSelector skins = WeaponMechanics.getInstance().getWeaponConfigurations().getObject(weaponTitle + ".Skin", SkinSelector.class);
        if (skins == null)
            throw new IllegalArgumentException("Weapon " + weaponTitle + " does not use skins");

        // First check for a skin override
        String skinOverride = CustomTag.WEAPON_SKIN.getString(weaponStack);
        if (skinOverride != null)
            return skinOverride;

        // Check if the player has a skin preference
        StatsData stats = WeaponMechanics.getInstance().getPlayerWrapper(player).getStatsData();
        if (stats == null)
            return "default";

        String skin = (String) stats.get(weaponTitle, WeaponStat.SKIN, null);
        if (skin == null)
            return "default";

        // Check if the skin is valid. This happens when a skin is deleted from config.
        if (!"default".equals(skin) && !skins.getCustomSkins().contains(skin))
            return "default";

        return skin;
    }

    /**
     * Sets the skin override for the given weapon item. This will override player preferences
     * ({@link #setSkin(Player, String, String)}).
     *
     * <p>
     * This method simply sets the skin override in the item's nbt data. You can set this value yourself
     * using {@link CustomTag#WEAPON_SKIN}. This method will throw an exception if the weapon does not
     * use skins.
     *
     * @param weaponStack The weapon item to set the skin for.
     * @param skin The skin to set. If null, the default skin will be used.
     */
    public static void setSkin(@NotNull ItemStack weaponStack, @Nullable String skin) {
        if (skin == null)
            skin = "default";

        String weaponTitle = getWeaponTitle(weaponStack);
        if (weaponTitle == null) {
            throw new IllegalArgumentException("Item is not a weapon");
        }

        // List valid skins, and check if the skin is valid
        SkinSelector skins = WeaponMechanics.getInstance().getWeaponConfigurations().getObject(weaponTitle + ".Skin", SkinSelector.class);
        if (skins == null)
            throw new IllegalArgumentException("Weapon " + weaponTitle + " does not use skins");
        if (!"default".equals(skin) && !skins.getCustomSkins().contains(skin))
            throw new IllegalArgumentException("Weapon " + weaponTitle + " does not have skin " + skin);

        CustomTag.WEAPON_SKIN.setString(weaponStack, skin);
    }

    /**
     * Attempts to set the player's skin preference for the given weapon.
     *
     * <p>
     * This method simply sets the player's skin preference in the database. For skins to work,
     * WeaponMechanicsCosmetics must be installed.
     *
     * @param player The player to set the skin preference for.
     * @param weaponTitle The weapon title to set the skin preference for.
     * @param skin The skin to set. If null, the default skin will be used.
     * @return <code>true</code> if the skin was set.
     */
    public static boolean setSkin(@NotNull Player player, @NotNull String weaponTitle, @Nullable String skin) {
        if (!WeaponMechanics.getInstance().getWeaponHandler().getInfoHandler().hasWeapon(weaponTitle))
            throw new IllegalArgumentException("Weapon " + weaponTitle + " does not exist");

        // If the skin is null, use the default skin
        if (skin == null)
            skin = "default";

        // List valid skins, and check if the skin is valid
        SkinSelector skins = WeaponMechanics.getInstance().getWeaponConfigurations().getObject(weaponTitle + ".Skin", SkinSelector.class);
        if (skins == null)
            throw new IllegalArgumentException("Weapon " + weaponTitle + " does not use skins");
        if (!"default".equals(skin) && !skins.getCustomSkins().contains(skin))
            throw new IllegalArgumentException("Weapon " + weaponTitle + " does not have skin " + skin);

        PlayerWrapper wrapper = WeaponMechanics.getInstance().getPlayerWrapper(player);
        StatsData stats = wrapper.getStatsData();
        if (stats == null)
            return false;

        // This set's the player's skin preference. The player's skin preference
        // is used by WeaponMechanicsCosmetics to determine what skin to use.
        // To add your own skin logic to override WMC, you can listen to the
        // WeaponSkinEvent and set the skin there.
        stats.set(weaponTitle, WeaponStat.SKIN, skin);

        // This check is done to prevent flashing. WeaponMechanicsCosmetics is
        // required for skins to work. Without it, running this code will cause
        // visual bugs
        if (Bukkit.getPluginManager().isPluginEnabled("WeaponMechanicsCosmetics")) {
            SkinHandler skinHandler = WeaponMechanics.getInstance().getWeaponHandler().getSkinHandler();
            EntityEquipment equipment = player.getEquipment();
            if (equipment == null)
                return true;

            // Update the main hand skin if the player is holding the gun.
            ItemStack mainHand = equipment.getItemInMainHand();
            if (weaponTitle.equals(getWeaponTitle(mainHand))) {
                skinHandler.tryUse(wrapper, weaponTitle, mainHand, EquipmentSlot.HAND);
            }

            // Update the off-hand skin if the player is holding the gun.
            ItemStack offHand = equipment.getItemInOffHand();
            if (weaponTitle.equals(getWeaponTitle(offHand))) {
                skinHandler.tryUse(wrapper, weaponTitle, offHand, EquipmentSlot.OFF_HAND);
            }
        }

        return true;
    }

    /**
     * Returns a copy of the skin set. This returned set may be empty if the weapon uses the Skins
     * feature, but does not have any defined skins (Which is very common!).
     *
     * <p>
     * Note that the default skin, <code>"default"</code>, is not included in the returned set.
     *
     * @param weaponTitle The non-null weapon title to get the skins for.
     * @return The non-null set of custom skins.
     */
    public static @NotNull Set<String> getCustomSkins(@NotNull String weaponTitle) {
        SkinSelector skins = WeaponMechanics.getInstance().getWeaponConfigurations().getObject(weaponTitle + ".Skin", SkinSelector.class);
        if (skins == null)
            throw new IllegalArgumentException("Weapon " + weaponTitle + " does not use skins");

        return skins.getCustomSkins();
    }

    /**
     * Attempts to set the full auto rate for the given entity. Will try to use the mainhand first, then
     * the offhand. Use shotsPerSecond=0 to stop the full auto.
     *
     * @param entity The non-null entity to set the full auto rate for.
     * @param shotsPerSecond The non-negative shots per second to set.
     * @return <code>true</code> if the full auto rate was set.
     */
    public static boolean setFullAutoShotsPerSecond(@NotNull LivingEntity entity, int shotsPerSecond) {
        EntityWrapper wrapper = WeaponMechanics.getInstance().getEntityWrapper(entity, true);
        if (wrapper == null)
            return true;

        // Try mainhand first
        if (setFullAutoShotsPerSecond(wrapper.getMainHandData(), shotsPerSecond))
            return true;
        if (setFullAutoShotsPerSecond(wrapper.getOffHandData(), shotsPerSecond))
            return true;

        return false;
    }

    /**
     * Attempts to set the full auto rate for the given hand.
     *
     * @param hand The non-null hand to set the full auto rate for.
     * @param shotsPerSecond The non-negative shots per second to set.
     * @return <code>true</code> if the full auto rate was set.
     */
    public static boolean setFullAutoShotsPerSecond(@NotNull HandData hand, int shotsPerSecond) {
        FullAutoTask fullAutoTask = hand.getFullAutoTask();
        if (fullAutoTask == null)
            return false;

        // Cancel the task for "invalid" shotsPerSecond values
        if (shotsPerSecond <= 0) {

            hand.getFullAutoWrappedTask().cancel();
            hand.setFullAutoTask(null, null);
            return true;
        }

        // Set the new shotsPerSecond
        fullAutoTask.setPerShot(shotsPerSecond / 20);
        fullAutoTask.setRate(shotsPerSecond % 20);
        return true;
    }

    /**
     * Returns how far the <code>entity</code> is zooming in. 0 means that the entity is not scoping at
     * all. Any other number means that the entity is scoping.
     *
     * @param entity The non-null living entity to check the scope state of.
     * @return The non-negative zoom amount.
     * @see EntityWrapper
     * @see PlayerWrapper
     */
    public static double getScopeLevel(@NotNull LivingEntity entity) {
        if (entity == null)
            throw new IllegalArgumentException("Expected an entity, got null");

        EntityWrapper wrapper = WeaponMechanics.getInstance().getEntityWrapper(entity, true);
        if (wrapper == null)
            return 0;

        return Math.max(
            wrapper.getMainHandData().getZoomData().getZoomAmount(),
            wrapper.getOffHandData().getZoomData().getZoomAmount());
    }

    /**
     * Returns <code>true</code> if the given <code>entity</code> is zooming in with their weapon. It
     * doesn't matter if the player is zoom-stacking or not, this method will return <code>true</code>
     * if the entity is zoomed in at all.
     *
     * @param entity The non-null living entity to check the scope state of.
     * @return <code>true</code> if the entity is scoping.
     */
    public static boolean isScoping(@NotNull LivingEntity entity) {
        return getScopeLevel(entity) != 0;
    }

    /**
     * Returns <code>true</code> if the given <code>entity</code> is reloading their weapon.
     *
     * @param entity The non-null living entity to check the reload state of.
     * @return <code>true</code> if the entity is reloading.
     */
    public static boolean isReloading(@NotNull LivingEntity entity) {
        EntityWrapper wrapper = WeaponMechanics.getInstance().getEntityWrapper(entity, true);
        if (wrapper == null)
            return false;

        return wrapper.getMainHandData().isReloading() || wrapper.getOffHandData().isReloading();
    }

    /**
     * Forces the given <code>entity</code> to reload their weapon. This method will return
     * <code>true</code> if the entity is now reloading.
     *
     * <p>
     * This method may return false if the entity is not holding a weapon, if the entity is already
     * reloading, or for any number of other possibilities.
     *
     * @param entity The non-null living entity to force reload.
     * @return true if the reload started
     */
    public static boolean tryReload(@NotNull LivingEntity entity) {
        EntityWrapper wrapper = WeaponMechanics.getInstance().getEntityWrapper(entity, true);
        if (wrapper == null)
            return false;
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null)
            return false;

        // Determine if held items are weapons
        ItemStack mainHand = equipment.getItemInMainHand();
        String mainWeapon = getWeaponTitle(mainHand);
        ItemStack offHand = equipment.getItemInOffHand();
        String offWeapon = getWeaponTitle(offHand);

        // Prepare reload stuff
        ReloadHandler reloadHandler = WeaponMechanics.getInstance().getWeaponHandler().getReloadHandler();
        boolean dualWield = mainWeapon != null && offWeapon != null;

        // Try to reload in both hands
        boolean isNowReloading = false;
        if (mainWeapon != null) {
            isNowReloading = reloadHandler.startReloadWithoutTrigger(wrapper, mainWeapon, mainHand, EquipmentSlot.HAND, dualWield, false);
        }
        if (offWeapon != null) {
            isNowReloading |= reloadHandler.startReloadWithoutTrigger(wrapper, offWeapon, offHand, EquipmentSlot.OFF_HAND, dualWield, false);
        }

        return isNowReloading;
    }

    /**
     * Returns an item corresponding to the given <code>weaponTitle</code>. The item will have a custom
     * name, lore, enchantments, flags, nbt data, etc.
     *
     * @param weaponTitle The non-null weapon-title of the weapon to generate.
     * @return The non-null weapon item.
     * @see me.deecaad.weaponmechanics.weapon.info.InfoHandler
     */
    public static @NotNull ItemStack generateWeapon(@NotNull String weaponTitle) {
        return WeaponMechanics.getInstance().getWeaponHandler().getInfoHandler().generateWeapon(weaponTitle, 1);
    }

    /**
     * Gives an item corresponding to the given <code>weaponTitle</code> to the given
     * <code>player</code>. If the player's inventory is full, the item will be dropped via
     * {@link org.bukkit.World#dropItemNaturally(Location, ItemStack)}.
     *
     * @param weaponTitle The non-null weapon-title of the weapon to generate.
     * @param player The non-null weapon item.
     */
    public static void giveWeapon(@NotNull String weaponTitle, @NotNull Player player) {
        WeaponMechanics.getInstance().getWeaponHandler().getInfoHandler().giveOrDropWeapon(weaponTitle, player, 1);
    }

    /**
     * Adds the projectile to WeaponMechanic's projectile spawner.
     *
     * @param projectile The non-null projectile to add.
     */
    public static void addProjectile(@NotNull AProjectile projectile) {
        WeaponMechanics.getInstance().getProjectileSpawner().spawn(projectile);
    }

    /**
     * Returns <code>true</code> if the block at the given location is broken by an explosion, block
     * damage, or otherwise.
     *
     * @param block The non-null block to check.
     * @return <code>true</code> if the block is broken.
     * @see BlockDamageData
     */
    public static boolean isBroken(@NotNull Block block) {
        return BlockDamageData.isBroken(block);
    }

    /**
     * Regenerates all blocks broken by this plugin. This is a "dangerous" method to call because this
     * may cause players/entities to get stuck underground, and may cause lag spikes if there are many
     * blocks to regenerate. Consider regenerating a few chunks instead of all blocks
     * {@link BlockDamageData#regenerate(Chunk)}.
     */
    public static void regenerateAllBlocks() {
        BlockDamageData.regenerateAll();
    }

    /**
     * Returns the weapon-title associated with the given item. If the given item is not a
     * WeaponMechanics weapon, this method will return <code>null</code>.
     *
     * <p>
     * Note that a weapon-title is the config name of a weapon, and you can use a weapon-title to pull
     * values from config easily.
     *
     * @param item The non-null item to get the weapon title from.
     * @return The item's weapon title, or null.
     */
    public static @Nullable String getWeaponTitle(@NotNull ItemStack item) {
        if (!item.hasItemMeta())
            return null;
        else
            return CustomTag.WEAPON_TITLE.getString(item);
    }

    /**
     * Returns the ammo currently loaded in the weapon, or null if it doesn't use ammo.
     *
     * @param weaponStack The non-null weapon item stack.
     * @return The current ammo, or null.
     */
    public static @Nullable Ammo getCurrentAmmo(@NotNull ItemStack weaponStack) {
        String weaponTitle = getWeaponTitle(weaponStack);
        AmmoConfig ammo = WeaponMechanics.getInstance().getWeaponConfigurations().getObject(weaponTitle + ".Reload.Ammo", AmmoConfig.class);
        if (ammo == null)
            return null;

        return ammo.getCurrentAmmo(weaponStack);
    }

    /**
     * Generates the item for the given ammo, or returns null if the ammo does not use items for ammo.
     *
     * @param ammoTitle The ammo to generate.
     * @param magazine true=generate magazine, false=generate bullet.
     * @return The generated item, or null.
     */
    public static @Nullable ItemStack generateAmmo(@NotNull String ammoTitle, boolean magazine) {
        Ammo ammo = WeaponMechanics.getInstance().getAmmoConfigurations().getObject(ammoTitle, Ammo.class);
        if (ammo == null)
            return null;

        if (ammo.getType() instanceof ItemAmmo itemAmmo) {
            return magazine ? itemAmmo.getMagazineItem() : itemAmmo.getBulletItem();
        }

        return null;
    }

    /**
     * Shorthand for an entity to shoot a weapon at the given target location.
     *
     * @param shooter The non-null entity to shoot the weapon.
     * @param weaponTitle The non-null weapon title to shoot.
     * @param target The non-null target location to shoot at.
     */
    public static void shoot(@NotNull LivingEntity shooter, @NotNull String weaponTitle, @NotNull Location target) {
        shoot(shooter, weaponTitle, target.toVector().subtract(shooter.getEyeLocation().toVector()));
    }

    /**
     * Shorthand for an entity to shoot a weapon in the direction the entity is currently facing.
     *
     * @param shooter The non-null entity to shoot the weapon.
     * @param weaponTitle The non-null weapon title to shoot.
     */
    public static void shoot(@NotNull LivingEntity shooter, @NotNull String weaponTitle) {
        shoot(shooter, weaponTitle, shooter.getLocation().getDirection());
    }

    /**
     * Shorthand for an entity to shoot a weapon in the given direction.
     *
     * @param shooter The non-null entity to shoot the weapon.
     * @param weaponTitle The non-null weapon title to shoot.
     * @param direction The non-null direction to shoot the weapon.
     */
    public static void shoot(@NotNull LivingEntity shooter, @NotNull String weaponTitle, @NotNull Vector direction) {
        if (!WeaponMechanics.getInstance().getWeaponHandler().getInfoHandler().hasWeapon(weaponTitle)) {
            throw new IllegalArgumentException("Weapon " + weaponTitle + " does not exist");
        }

        WeaponMechanics.getInstance().getWeaponHandler().getShootHandler().shoot(shooter, weaponTitle, direction.clone().normalize());
    }

    /**
     * Returns MechanicsCore's compatibility version. Useful for dealing with NMS code or otherwise
     * version dependent code.
     *
     * @return The non-null compatibility version.
     */
    public static @NotNull ICompatibility getCompatibility() {
        return CompatibilityAPI.getCompatibility();
    }

    /**
     * Returns WeaponMechanics' compatibility version. Useful for dealing with NMS code or otherwise
     * version dependent code.
     *
     * @return The non-null weapon compatibility version.
     */
    public static @NotNull IWeaponCompatibility getWeaponCompatibility() {
        return WeaponCompatibilityAPI.getWeaponCompatibility();
    }
}
