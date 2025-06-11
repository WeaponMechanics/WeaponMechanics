package me.deecaad.weaponmechanics.weapon.weaponevents;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import me.deecaad.weaponmechanics.weapon.shoot.FullAutoTask;

/**
 * Called before an attempt to use Full Auto. This event is called for all types of weapons,
 * including semi, burst, AND auto. However, for semi and burst, shotsPerSecond will be 0 (and thus
 * the event will be cancelled).
 *
 * <p>
 * Note that all changes are kept until the task is cancelled. This means that changes are
 * <i>semi-permanent</i>. If you want to change the full-auto rate of a current gun, you should get
 * the {@link FullAutoTask} from an entity's hand data.
 *
 * <p>
 * If you want to "reset" the rate later, you should call this event so other plugins, like
 * WeaponMechanicsPlus, can modify the fire rate again.
 */
public class WeaponFullAutoEvent extends WeaponEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private int shotsPerSecond;

    public WeaponFullAutoEvent(String weaponTitle, ItemStack weaponStack, LivingEntity shooter, EquipmentSlot hand, int shotsPerSecond) {
        super(weaponTitle, weaponStack, shooter, hand);

        this.shotsPerSecond = shotsPerSecond;
    }

    /**
     * Gets the rate of fire in shots per second. This is the number of shots that will be fired every
     * second. For example, if this value is 2, then 2 shots will be fired every second.
     *
     * <p>
     * For semi and burst weapons, this value will be 0. A typical full auto weapon will use values
     * 1-20, but can be any integer.
     *
     * @return the rate of fire in shots per second
     */
    public int getShotsPerSecond() {
        return shotsPerSecond;
    }

    /**
     * Sets the rate of fire in shots per second. This is the number of shots that will be fired every
     * second. For example, if this value is 2, then 2 shots will be fired every second.
     *
     * <p>
     * If you set this value to 0, the full auto attempt will be cancelled, and the handler will try to
     * use burst/semi.
     *
     * @param shotsPerSecond the rate of fire in shots per second
     */
    public void setShotsPerSecond(int shotsPerSecond) {
        this.shotsPerSecond = shotsPerSecond;
    }

    @Override
    @NotNull public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        // Full auto rate is 0, so full auto is not used
        return getShotsPerSecond() == 0;
    }

    @Override
    @Deprecated
    public void setCancelled(boolean cancel) {
        // Since the cancellation state depends on an integer, using setCancelled=true
        // does not make sense... That is why this method is deprecated. Use
        // #setShotsPerSecond instead.
        if (cancel) {
            setShotsPerSecond(0);
        } else {
            setShotsPerSecond(1); // BAD TERRIBLE HACK, DO NOT USE
        }
    }
}
