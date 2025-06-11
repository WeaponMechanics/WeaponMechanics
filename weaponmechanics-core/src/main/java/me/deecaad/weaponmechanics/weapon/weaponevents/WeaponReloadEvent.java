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
 * Called when a weapon begins to reload. Usually the entity involved will be a
 * {@link org.bukkit.entity.Player}, but this may change in the future.
 */
public class WeaponReloadEvent extends WeaponEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int ammoLeft; // internal use
    private int reloadTime;
    private int ammoPerReload;
    private int magazineSize;
    private int firearmOpenTime;
    private int firearmCloseTime;

    private MechanicManager mechanics;
    private boolean cancelled;

    public WeaponReloadEvent(String weaponTitle, ItemStack weaponItem, LivingEntity weaponUser, EquipmentSlot hand,
        int ammoLeft, int reloadTime, int ammoPerReload, int magazineSize, int firearmOpenTime, int firearmCloseTime,
        MechanicManager mechanics) {
        super(weaponTitle, weaponItem, weaponUser, hand);
        this.ammoLeft = ammoLeft;
        this.reloadTime = reloadTime;
        this.ammoPerReload = ammoPerReload;
        this.magazineSize = magazineSize;
        this.firearmOpenTime = firearmOpenTime;
        this.firearmCloseTime = firearmCloseTime;

        this.mechanics = mechanics;
    }

    public int getReloadTime() {
        return reloadTime;
    }

    public void setReloadTime(int reloadTime) {
        this.reloadTime = reloadTime;
    }

    public int getAmmoPerReload() {
        return ammoPerReload;
    }

    public void setAmmoPerReload(int ammoPerReload) {
        this.ammoPerReload = ammoPerReload;
    }

    public int getMagazineSize() {
        return magazineSize;
    }

    public void setMagazineSize(int magazineSize) {
        this.magazineSize = magazineSize;
    }

    public int getFirearmOpenTime() {
        return firearmOpenTime;
    }

    public void setFirearmOpenTime(int firearmOpenTime) {
        this.firearmOpenTime = firearmOpenTime;
    }

    public int getFirearmCloseTime() {
        return firearmCloseTime;
    }

    public void setFirearmCloseTime(int firearmCloseTime) {
        this.firearmCloseTime = firearmCloseTime;
    }

    public int getReloadCompleteTime() {
        return firearmOpenTime + reloadTime + firearmCloseTime;
    }

    public MechanicManager getMechanics() {
        return mechanics;
    }

    public void setMechanics(MechanicManager mechanics) {
        if (this.mechanics != null)
            this.mechanics.clearDirty(); // clear any modifications
        this.mechanics = mechanics;
    }

    @Override
    public boolean isCancelled() {
        return cancelled || ammoLeft >= magazineSize || reloadTime == 0;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
