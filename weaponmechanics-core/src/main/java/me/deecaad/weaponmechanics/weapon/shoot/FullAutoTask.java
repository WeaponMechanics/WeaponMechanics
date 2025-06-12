package me.deecaad.weaponmechanics.weapon.shoot;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.utils.CustomTag;
import me.deecaad.weaponmechanics.weapon.WeaponHandler;
import me.deecaad.weaponmechanics.weapon.trigger.Trigger;
import me.deecaad.weaponmechanics.weapon.trigger.TriggerType;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import me.deecaad.weaponmechanics.wrappers.HandData;
import org.bukkit.Location;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * This task handles firing projectiles every <code>N</code> ticks. 1 of these tasks is created
 * every time you start firing a fully automatic weapon. The task is cancelled when the user is no
 * longer shooting.
 */
public class FullAutoTask implements Consumer<TaskImplementation<Void>> {

    /**
     * Hardcoded full auto values. For every 1 in the array, the gun will fire on that tick. Some
     * indexes are marked as <i>"perfect"</i>. This means that the delay between shots is exactly equal
     * no matter what. Some indexes are marked as <i>"good"</i>. This means that the distance between
     * zeros are equal.
     * <p>
     * Calculated using python: <blockquote>
     * 
     * <pre>{@code
     *     from collections import deque
     *
     *     for shotsPerSecond in range(1, 21):
     *         collection = deque([0] * 20)
     *         accumulate = 0
     *         for i in range(0, 20):
     *
     *             accumulate += shotsPerSecond / 20 + 0.00000000001
     *             if accumulate >= 1.0:
     *                 accumulate -= 1.0
     *                 collection[i] = 1
     *
     *         # shift over so the first tick is always a shot
     *         while collection[0] == 0:
     *             collection.rotate(-1)
     *
     *         print("\t{" + ", ".join(map(str, collection)) + "},")
     * }</pre>
     * 
     * </blockquote>
     * <p>
     */
    private static final int[][] AUTO = new int[][]{
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // 0 perfect
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // 1 good
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // 2 good
            {1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0}, // 3
            {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0}, // 4 good
            {1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0}, // 5 good
            {1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0}, // 6
            {1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0}, // 7
            {1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0}, // 8
            {1, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0}, // 9
            {1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0}, // 10 good
            {1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0}, // 11
            {1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0}, // 12
            {1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0}, // 13
            {1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0}, // 14
            {1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0}, // 15 good
            {1, 1, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, 1, 1, 0}, // 16
            {1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0}, // 17
            {1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1}, // 18
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0}, // 19
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1} // 20 good
    };

    private final WeaponHandler weaponHandler;
    private final EntityWrapper entityWrapper;
    private final boolean mainHand;
    private final TriggerType triggerType;
    private final boolean dualWield;
    private final HandData handData;
    private final String weaponTitle;
    private final ItemStack weaponStack;
    private int rate; // A number 1-20, the number of shots per second
    private int perShot; // the number of shots per tick to add, when rate > 20

    // Values from config
    private final Trigger trigger;
    private final boolean consumeItemOnShoot;
    private final int ammoPerShot;

    // Updated in the run() method
    private int currentTick;

    public FullAutoTask(WeaponHandler weaponHandler, EntityWrapper entityWrapper, String weaponTitle, ItemStack weaponStack, boolean mainHand, TriggerType triggerType, boolean dualWield,
        int shotsPerSecond) {
        this.weaponHandler = weaponHandler;
        this.entityWrapper = entityWrapper;
        this.mainHand = mainHand;
        this.triggerType = triggerType;
        this.dualWield = dualWield;
        this.handData = entityWrapper.getHandData(mainHand);
        this.weaponTitle = weaponTitle;
        this.weaponStack = weaponStack;

        this.rate = shotsPerSecond % 20;
        this.perShot = shotsPerSecond / 20;

        trigger = WeaponMechanics.getInstance().getWeaponConfigurations().getObject(weaponTitle + ".Shoot.Trigger", Trigger.class);
        consumeItemOnShoot = WeaponMechanics.getInstance().getWeaponConfigurations().getBoolean(weaponTitle + ".Shoot.Consume_Item_On_Shoot");
        ammoPerShot = WeaponMechanics.getInstance().getWeaponConfigurations().getInt(weaponTitle + ".Shoot.Ammo_Per_Shot", 1);
    }

    public EntityWrapper getEntityWrapper() {
        return entityWrapper;
    }

    public HandData getHandData() {
        return handData;
    }

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        if (rate < 0 || rate > 20)
            throw new IllegalArgumentException("rate must be [0, 20]");

        this.rate = rate;
    }

    public int getPerShot() {
        return perShot;
    }

    public void setPerShot(int perShot) {
        if (perShot < 0)
            throw new IllegalArgumentException("perShot must be positive");

        this.perShot = perShot;
    }

    public int getCurrentTick() {
        return currentTick;
    }

    public void setCurrentTick(int currentTick) {
        this.currentTick = currentTick;
    }

    @Override
    public void accept(@NotNull TaskImplementation task) {
        ItemStack taskReference = mainHand ? entityWrapper.getEntity().getEquipment().getItemInMainHand() : entityWrapper.getEntity().getEquipment().getItemInOffHand();
        if (!taskReference.hasItemMeta()) {
            task.cancel();
            handData.setFullAutoTask(null, null);
            return;
        }

        if (entityWrapper.getMainHandData().isReloading() || entityWrapper.getOffHandData().isReloading()) {
            task.cancel();
            handData.setFullAutoTask(null, null);
            return;
        }

        int ammoLeft = weaponHandler.getReloadHandler().getAmmoLeft(taskReference, weaponTitle);

        if (!weaponHandler.getShootHandler().keepFullAutoOn(entityWrapper, triggerType, trigger)) {
            task.cancel();
            handData.setFullAutoTask(null, null);

            if (ammoLeft == 0) {
                weaponHandler.getShootHandler().startReloadIfBothWeaponsEmpty(entityWrapper, weaponTitle, taskReference, mainHand ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND, dualWield, false);
            } else {
                weaponHandler.getShootHandler().doShootFirearmActions(entityWrapper, weaponTitle, taskReference, handData, mainHand ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND);
            }

            return;
        }

        // Determine if we should shoot on this tick. The AUTO array is a table of basically true/false
        // values.
        int shootAmount = perShot + AUTO[rate][currentTick];

        // START RELOAD STUFF
        if (ammoLeft != -1) {

            // Check whether shoot amount of this tick should be changed
            if (ammoLeft - shootAmount < 0) {
                shootAmount = ammoLeft;
            }

            if (!weaponHandler.getReloadHandler().consumeAmmo(taskReference, weaponTitle, shootAmount * ammoPerShot)) {
                task.cancel();
                handData.setFullAutoTask(null, null);

                weaponHandler.getShootHandler().startReloadIfBothWeaponsEmpty(entityWrapper, weaponTitle, taskReference, mainHand ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND, dualWield, false);
                return;
            }
        }
        // END RELOAD STUFF

        boolean destroyWhenEmpty = WeaponMechanics.getInstance().getWeaponConfigurations().getBoolean(weaponTitle + ".Shoot.Destroy_When_Empty");
        for (int i = 0; i < shootAmount; ++i) {
            Location shootLocation = weaponHandler.getShootHandler().getShootLocation(entityWrapper, weaponTitle, mainHand);
            weaponHandler.getShootHandler().shoot(entityWrapper, weaponTitle, taskReference, shootLocation, mainHand, true, false);
            boolean consumeEmpty = destroyWhenEmpty && CustomTag.AMMO_LEFT.getInteger(weaponStack) == 0;
            if ((consumeEmpty || consumeItemOnShoot) && weaponHandler.getShootHandler().handleConsumeItemOnShoot(weaponStack, handData)) {
                return;
            }
        }

        increment();
    }

    /**
     * Increments the current tick, and resets it to 0 if it is 20.
     */
    private void increment() {
        currentTick++;
        if (currentTick >= 20) {
            currentTick = 0;
        }
    }
}
