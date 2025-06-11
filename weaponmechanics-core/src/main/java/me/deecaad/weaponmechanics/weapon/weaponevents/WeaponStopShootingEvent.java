package me.deecaad.weaponmechanics.weapon.weaponevents;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class WeaponStopShootingEvent extends WeaponEvent {
    private static final HandlerList handlers = new HandlerList();

    private final long lastShootTime;

    public WeaponStopShootingEvent(String weaponTitle, ItemStack weaponStack, LivingEntity shooter, EquipmentSlot hand, long lastShootTime) {
        super(weaponTitle, weaponStack, shooter, hand);
        this.lastShootTime = lastShootTime;
    }

    public long getLastShootTime() {
        return lastShootTime;
    }

    @NotNull @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull public static HandlerList getHandlerList() {
        return handlers;
    }
}
