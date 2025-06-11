package me.deecaad.weaponmechanics.weapon.weaponevents;

import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import me.deecaad.weaponmechanics.wrappers.HandData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;

public abstract class WeaponEvent extends EntityEvent {

    protected final String weaponTitle;
    private final ItemStack weaponStack;
    private final LivingEntity shooter;
    private final EquipmentSlot hand;

    public WeaponEvent(String weaponTitle, ItemStack weaponStack, LivingEntity shooter, EquipmentSlot hand) {
        super(shooter);

        this.weaponTitle = weaponTitle;
        this.weaponStack = weaponStack;
        this.shooter = shooter;
        this.hand = hand;
    }

    /**
     * Returns the weapon-title associated with the weapon involved. This is the same
     * as`CustomTag.WEAPON_TITLE.getString(event.getWeaponStack())`.
     *
     * @return The non-null weapon title.
     */
    @NotNull public String getWeaponTitle() {
        return weaponTitle;
    }

    /**
     * Returns the weapon item which caused the event. This should always be an item in the player's
     * main hand, or off hand.
     *
     * @return The non-null weapon item.
     */
    public ItemStack getWeaponStack() {
        return weaponStack;
    }

    /**
     * Returns the shooter involved in this event. This is the same as {@link #getEntity()} but without
     * the added overhead from casting it to a {@link LivingEntity}.
     *
     * @return The non-null entity that fired the weapon. This will not always be a player!
     */
    @NotNull public LivingEntity getShooter() {
        return this.shooter;
    }

    /**
     * Returns the hand involved in the event. WeaponMechanics will only ever set this to
     * {@link EquipmentSlot#HAND} or {@link EquipmentSlot#OFF_HAND} (Other plugins may change that).
     * This value will be <code>null</code> when no hand was involved (Some explosions and some API
     * methods).
     *
     * @return The nullable hand involved.
     * @see #isMainHand()
     * @see #isOffHand()
     */
    @Nullable public EquipmentSlot getHand() {
        return hand;
    }

    /**
     * Returns <code>true</code> if the weapon involved in this event was in the main hand.
     * <code>false</code> means the weapon was in the offhand.
     *
     * @return true if weapon is in main hand.
     */
    public boolean isMainHand() {
        return hand == EquipmentSlot.HAND;
    }

    /**
     * Returns <code>false</code> if the weapon involved in this event was in the main hand.
     * <code>true</code> means the weapon was in the offhand.
     *
     * @return true if weapon is in offhand.
     */
    public boolean isOffHand() {
        return hand == EquipmentSlot.OFF_HAND;
    }

    /**
     * Helper method to the {@link HandData} involved in this event. For player shooters, the returned
     * value will never be null. For entities, the value <i>might</i> be null.
     *
     * @return The nullable hand data involved.
     */
    public HandData getHandData(boolean noAutoAdd) {
        if (hand == null)
            return null;

        EntityWrapper wrapper = WeaponMechanics.getInstance().getEntityWrapper(getShooter(), noAutoAdd);
        if (wrapper == null)
            return null;

        return isMainHand() ? wrapper.getMainHandData() : wrapper.getOffHandData();
    }

    public EntityWrapper getShooterWrapper(boolean noAutoAdd) {
        return WeaponMechanics.getInstance().getEntityWrapper(getShooter(), noAutoAdd);
    }
}