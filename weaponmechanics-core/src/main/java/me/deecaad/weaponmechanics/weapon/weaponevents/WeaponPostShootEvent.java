package me.deecaad.weaponmechanics.weapon.weaponevents;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Called after all the {@link WeaponShootEvent}s are called. This is useful for weapons like
 * shotguns, which fire multiple pellets in the same shot (and therefor call multiple
 * {@link WeaponShootEvent}s).
 */
public class WeaponPostShootEvent extends WeaponEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private boolean unscopeAfterShot;

    public WeaponPostShootEvent(String weaponTitle, ItemStack weaponStack, LivingEntity shooter, EquipmentSlot hand, boolean unscopeAfterShot) {
        super(weaponTitle, weaponStack, shooter, hand);
        this.unscopeAfterShot = unscopeAfterShot;
    }

    public boolean isUnscopeAfterShot() {
        return unscopeAfterShot;
    }

    public void setUnscopeAfterShot(boolean unscopeAfterShot) {
        this.unscopeAfterShot = unscopeAfterShot;
    }

    @Override
    @NotNull public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}