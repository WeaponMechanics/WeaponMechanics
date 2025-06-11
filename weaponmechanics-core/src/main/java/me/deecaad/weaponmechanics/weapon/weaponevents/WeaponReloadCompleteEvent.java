package me.deecaad.weaponmechanics.weapon.weaponevents;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a reload is completed successfully.
 */
public class WeaponReloadCompleteEvent extends WeaponEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public WeaponReloadCompleteEvent(String weaponTitle, ItemStack weaponItem, LivingEntity weaponUser, EquipmentSlot slot) {
        super(weaponTitle, weaponItem, weaponUser, slot);
    }

    @Override
    @NotNull public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}