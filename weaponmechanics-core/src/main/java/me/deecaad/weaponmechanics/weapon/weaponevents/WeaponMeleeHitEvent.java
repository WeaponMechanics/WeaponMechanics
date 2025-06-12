package me.deecaad.weaponmechanics.weapon.weaponevents;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Called whenever a melee attack hits. Goes along with {@link WeaponMeleeMissEvent}.
 */
public class WeaponMeleeHitEvent extends WeaponEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity victim;
    private int meleeHitDelay;
    private boolean isBackstab;
    private boolean isCancelled;

    public WeaponMeleeHitEvent(String weaponTitle, ItemStack weaponStack, LivingEntity shooter, EquipmentSlot hand, LivingEntity victim, int meleeHitDelay, boolean isBackstab) {
        super(weaponTitle, weaponStack, shooter, hand);
        this.victim = victim;
        this.meleeHitDelay = meleeHitDelay;
        this.isBackstab = isBackstab;
    }

    /**
     * Returns who receives damage from the melee hit.
     *
     * @return The non-null victim.
     */
    public LivingEntity getVictim() {
        return victim;
    }

    /**
     * Returns the delay after the hit before the melee weapon can be swung again.
     *
     * @return The delay you can swing again.
     */
    public int getMeleeHitDelay() {
        return meleeHitDelay;
    }

    /**
     * Sets the delay after the hit before the melee weapon can be swung again.
     *
     * @param meleeHitDelay The delay before you can swing again.
     */
    public void setMeleeHitDelay(int meleeHitDelay) {
        this.meleeHitDelay = meleeHitDelay;
    }

    public boolean isBackstab() {
        return isBackstab;
    }

    public void setBackstab(boolean backstab) {
        isBackstab = backstab;
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
