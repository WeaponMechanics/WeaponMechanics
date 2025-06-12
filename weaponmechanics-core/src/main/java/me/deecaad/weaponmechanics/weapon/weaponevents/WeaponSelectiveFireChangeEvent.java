package me.deecaad.weaponmechanics.weapon.weaponevents;

import me.deecaad.weaponmechanics.weapon.shoot.SelectiveFireState;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Event that occurs when a {@link org.bukkit.entity.Player} switches between semi-automatic, burst,
 * and automatic using WeaponMechanic's selective fire feature.
 */
public class WeaponSelectiveFireChangeEvent extends WeaponEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final SelectiveFireState oldState;
    private SelectiveFireState newState;
    private boolean cancelled;

    public WeaponSelectiveFireChangeEvent(String weaponTitle, ItemStack weaponStack, LivingEntity shooter, EquipmentSlot hand,
        SelectiveFireState oldState, SelectiveFireState newState) {
        super(weaponTitle, weaponStack, shooter, hand);

        this.oldState = oldState;
        this.newState = newState;
    }

    public SelectiveFireState getOldState() {
        return oldState;
    }

    public SelectiveFireState getNewState() {
        return newState;
    }

    public void setNewState(SelectiveFireState newState) {
        this.newState = newState;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    @NotNull public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
