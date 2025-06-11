package me.deecaad.weaponmechanics.weapon.weaponevents;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Called if a reload was cancelled early (Usually because the player swapped hands).
 */
public class WeaponReloadCancelEvent extends WeaponEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int elapsedTime;

    public WeaponReloadCancelEvent(String weaponTitle, ItemStack weaponItem, LivingEntity weaponUser, EquipmentSlot hand, int elapsedTime) {
        super(weaponTitle, weaponItem, weaponUser, hand);
        this.elapsedTime = elapsedTime;
    }

    /**
     * Returns the amount of time, in ticks, that elapsed since the reload started.
     *
     * @return The elapsed time.
     */
    public int getElapsedTime() {
        return elapsedTime;
    }

    @Override
    @NotNull public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
