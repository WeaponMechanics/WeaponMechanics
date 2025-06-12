package me.deecaad.weaponmechanics.weapon.weaponevents;

import me.deecaad.core.mechanics.MechanicManager;
import me.deecaad.core.mechanics.Mechanics;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Called whenever a melee swing misses. Goes along with {@link WeaponMeleeHitEvent}.
 */
public class WeaponMeleeMissEvent extends WeaponEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private int meleeMissDelay;
    private MechanicManager mechanics;
    private boolean consume;
    private boolean isCancelled;

    public WeaponMeleeMissEvent(String weaponTitle, ItemStack weaponStack, LivingEntity shooter, EquipmentSlot hand,
        int meleeMissDelay, MechanicManager mechanics, boolean consume) {

        super(weaponTitle, weaponStack, shooter, hand);

        this.meleeMissDelay = meleeMissDelay;
        this.mechanics = mechanics;
        this.consume = consume;
    }

    public int getMeleeMissDelay() {
        return meleeMissDelay;
    }

    public void setMeleeMissDelay(int meleeMissDelay) {
        this.meleeMissDelay = meleeMissDelay;
    }

    public MechanicManager getMechanics() {
        return mechanics;
    }

    public void setMechanics(MechanicManager mechanics) {
        this.mechanics = mechanics;
    }

    public boolean isConsume() {
        return consume;
    }

    public void setConsume(boolean consume) {
        this.consume = consume;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    @Override
    @NotNull public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}