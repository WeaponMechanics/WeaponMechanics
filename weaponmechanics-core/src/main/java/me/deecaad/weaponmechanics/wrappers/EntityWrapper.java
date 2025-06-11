package me.deecaad.weaponmechanics.wrappers;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.deecaad.core.file.Configuration;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.events.EntityToggleInMidairEvent;
import me.deecaad.weaponmechanics.events.EntityToggleStandEvent;
import me.deecaad.weaponmechanics.events.EntityToggleSwimEvent;
import me.deecaad.weaponmechanics.events.EntityToggleWalkEvent;
import me.deecaad.weaponmechanics.utils.CustomTag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Wraps a {@link LivingEntity} object to simplify per-entity data/methods that are used by
 * WeaponMechanics. Also contains useful API functionality for plugins who want to check if an
 * entity is scoped, reloading, etc.
 */
public class EntityWrapper {

    private static final int MOVE_TASK_INTERVAL = 1;

    private final LivingEntity entity;

    private TaskImplementation<Void> moveTask;
    private boolean standing;
    private boolean walking;
    private boolean inMidair;
    private boolean swimming;
    private HandData mainHandData;
    private HandData offHandData;

    public EntityWrapper(LivingEntity livingEntity) {
        this.entity = livingEntity;

        Configuration config = WeaponMechanics.getInstance().getConfiguration();
        if (!config.getBoolean("Disabled_Trigger_Checks.In_Midair")
            || !config.getBoolean("Disabled_Trigger_Checks.Standing_And_Walking")
            || !config.getBoolean("Disabled_Trigger_Checks.Jump")
            || !config.getBoolean("Disabled_Trigger_Checks.Double_Jump")) {

            this.moveTask = WeaponMechanics.getInstance().getFoliaScheduler().entity(livingEntity).runAtFixedRate(new MoveTask(this), 1, MOVE_TASK_INTERVAL);
        }
    }

    public LivingEntity getEntity() {
        return this.entity;
    }

    public TaskImplementation<Void> getMoveTask() {
        return this.moveTask;
    }

    /**
     * Returns <code>true</code> when the entity is standing still. Returns <code>false</code> when the
     * entity is moving, swimming, or mid-air.
     *
     * @return <code>true</code> if the entity is standing still.
     */
    public boolean isStanding() {
        return this.standing;
    }

    void setStanding(boolean standing) {
        if (this.standing == standing)
            return;
        this.standing = standing;

        if (standing) {
            // -> Can't be walking, swimming, or mid-air at same time
            setWalking(false);
            setSwimming(false);
            setInMidair(false);
        }
        Bukkit.getPluginManager().callEvent(new EntityToggleStandEvent(entity, standing));
    }

    /**
     * Returns <code>true</code> when the entity is moving. Returns <code>false</code> when the entity
     * is standing still, swimming, or mid-air.
     *
     * @return <code>true</code> if the entity is moving.
     */
    public boolean isWalking() {
        return this.walking;
    }

    void setWalking(boolean walking) {
        if (this.walking == walking)
            return;
        this.walking = walking;

        if (walking) {
            // -> Can't be standing, swimming, in mid-air at same time
            setStanding(false);
            setSwimming(false);
            setInMidair(false);
        }
        Bukkit.getPluginManager().callEvent(new EntityToggleWalkEvent(entity, walking));
    }

    /**
     * Returns <code>true</code> when the entity is mid-air (not on the ground). Returns
     * <code>false</code> when the entity is standing still, swimming, or walking.
     *
     * @return <code>true</code> if the entity is mid-air.
     */
    public boolean isInMidair() {
        return this.inMidair;
    }

    void setInMidair(boolean inMidair) {
        if (this.inMidair == inMidair)
            return;
        this.inMidair = inMidair;

        if (inMidair) {
            // -> Can't be walking, swimming, standing at same time
            setWalking(false);
            setSwimming(false);
            setStanding(false);
        }
        Bukkit.getPluginManager().callEvent(new EntityToggleInMidairEvent(entity, inMidair));
    }

    /**
     * Returns <code>true</code> when the entity is swimming (legs and head are both in water, also
     * checks 1.13+ sprint swimming). Returns <code>false</code> when the entity is standing still,
     * mid-air, or walking.
     *
     * @return <code>true</code> if the entity is swimming.
     */
    public boolean isSwimming() {
        return swimming;
    }

    void setSwimming(boolean swimming) {
        if (this.swimming == swimming)
            return;
        this.swimming = swimming;

        if (swimming) {
            // -> Can't be walking, standing, in mid-air at same time
            setWalking(false);
            setStanding(false);
            setInMidair(false);
        }
        Bukkit.getPluginManager().callEvent(new EntityToggleSwimEvent(entity, swimming));
    }

    /**
     * Returns <code>true</code> if the entity is a player, and the player is in sneak mode.
     *
     * @return <code>true</code> when the player is sneaking.
     * @see Player#isSneaking()
     */
    public boolean isSneaking() {
        return false;
    }

    /**
     * Returns <code>true</code> if the entity is a player, and the player is sprinting.
     *
     * @return <code>true</code> when the player is sprinting.
     * @see Player#isSprinting()
     */
    public boolean isSprinting() {
        return false;
    }

    /**
     * Returns <code>true</code> if the entity is gliding using an elytra. Apparently, non-player
     * entities CAN glide.
     *
     * @return <code>true</code> when the entity is gliding.
     * @see LivingEntity#isGliding()
     */
    public boolean isGliding() {
        return entity.isGliding();
    }

    /**
     * Returns <code>true</code> if the entity is a player, and the player has right-clicked in the past
     * 4 ticks (+- 25 milliseconds). This method also considers the player's ping
     * ({@link Player#getPing()}) to determine if they are still right-clicking.
     *
     * <p>
     * While this method is usually quite inaccurate (can be up to 4 ticks late!), it is 100% accurate
     * when the player is blocking (swords in 1.8, shields in 1.9+).
     *
     * @return <code>true</code> when the player is right-clicking.
     * @see Player#isBlocking()
     */
    public boolean isRightClicking() {
        // Always false for other entities than players
        // PlayerWrapper actually checks these
        return false;
    }

    /**
     * Returns <code>true</code> if the entity is dual wielding, meaning when they have items equipped
     * in both hands.
     *
     * @return <code>true</code> when the entity is dual wielding.
     */
    public boolean isDualWielding() {
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null)
            return false; // never occurs, but lets be safe
        return equipment.getItemInMainHand().getType() != Material.AIR
            && equipment.getItemInOffHand().getType() != Material.AIR;
    }

    /**
     * Returns <code>true</code> if the entity is dual wielding two weapons from WeaponMechanics.
     *
     * @return <code>true</code> when the entity is dual wielding two weapons.
     * @see #isDualWielding()
     */
    public boolean isDualWieldingWeapons() {
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null)
            return false; // never occurs, but lets be safe

        ItemStack main = equipment.getItemInMainHand();
        ItemStack off = equipment.getItemInOffHand();
        return main.getType() != Material.AIR && off.getType() != Material.AIR && CustomTag.WEAPON_TITLE.hasString(main)
            && CustomTag.WEAPON_TITLE.hasString(off);
    }

    /**
     * @return <code>true</code> when the entity is riding.
     */
    public boolean isRiding() {
        return entity.isInsideVehicle();
    }

    @NotNull public HandData getHandData(boolean mainHand) {
        return mainHand ? getMainHandData() : getOffHandData();
    }

    @NotNull public HandData getMainHandData() {
        return mainHandData == null ? mainHandData = new HandData(this, true) : mainHandData;
    }

    @NotNull public HandData getOffHandData() {
        return offHandData == null ? offHandData = new HandData(this, false) : offHandData;
    }

    public boolean isReloading() {
        return getMainHandData().isReloading() || getOffHandData().isReloading();
    }

    public boolean isZooming() {
        return getMainHandData().getZoomData().isZooming() || getOffHandData().getZoomData().isZooming();
    }

    public boolean isAmmoEmpty() {
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null)
            return false; // never occurs, but lets be safe
        ItemStack main = equipment.getItemInMainHand();
        ItemStack off = equipment.getItemInOffHand();

        boolean isEmpty = false;
        if (main.getType() != Material.AIR && CustomTag.WEAPON_TITLE.hasString(main))
            isEmpty = CustomTag.AMMO_LEFT.getInteger(main) == 0;
        if (off.getType() != Material.AIR && CustomTag.WEAPON_TITLE.hasString(off))
            isEmpty = CustomTag.AMMO_LEFT.getInteger(off) == 0;

        return isEmpty;
    }

    public boolean isPlayer() {
        return false;
    }
}
