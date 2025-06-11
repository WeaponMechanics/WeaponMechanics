package me.deecaad.weaponmechanics.weapon.weaponevents;

import me.deecaad.weaponmechanics.weapon.skin.SkinSelector;
import me.deecaad.weaponmechanics.weapon.trigger.TriggerType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Called whenever a {@link me.deecaad.weaponmechanics.weapon.skin.SkinHandler} attempts to change
 * the skin of a weapon. A skin may be changed when an entity zooms in/out, reloads, sprints, runs
 * out of ammo, etc.
 */
public class WeaponSkinEvent extends WeaponEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final SkinSelector skinList;
    private final TriggerType cause;
    private final boolean forceDefault;
    private String skin;

    private boolean cancel;

    public WeaponSkinEvent(String weaponTitle, ItemStack weaponStack, LivingEntity shooter, EquipmentSlot hand, SkinSelector skinList) {
        super(weaponTitle, weaponStack, shooter, hand);
        this.skinList = skinList;
        this.cause = null;
        this.forceDefault = false;
        this.skin = "default";
    }

    public WeaponSkinEvent(String weaponTitle, ItemStack weaponStack, LivingEntity shooter, EquipmentSlot hand, SkinSelector skinList, TriggerType cause, boolean forceDefault) {
        super(weaponTitle, weaponStack, shooter, hand);
        this.skinList = skinList;
        this.cause = cause;
        this.forceDefault = forceDefault;
        this.skin = "default";
    }

    /**
     * Returns the config options for this skin. You can get this by using
     * <code>WeaponMechanics.getConfigurations().getObject(weaponTitle + ".Skin", SkinSelector.class)</code>
     *
     * @return The non-null skin list.
     */
    public SkinSelector getSkinSelector() {
        return skinList;
    }

    /**
     * Returns the reason for the skin update. Returns <code>null</code> when the cause is unknown, or a
     * plugin manually updated the skin.
     *
     * @return The cause of the skin update, or null.
     */
    public TriggerType getCause() {
        return cause;
    }

    /**
     * Returns <code>true</code> if the {@link me.deecaad.weaponmechanics.weapon.skin.SkinHandler}
     * requests a DEFAULT weapon. This tends to happen when the weapon is dequipped.
     *
     * @return true if the weapon should be default skin.
     */
    public boolean isForceDefault() {
        return forceDefault;
    }

    /**
     * If only WeaponMechanics is installed, this method will always return <code>"Default"</code>. If
     * WeaponMechanicsCosmetics is installed, this method may return a different value (Which you can
     * find in the config for this weapon).
     *
     * @return The non-null name of the skin.
     */
    public String getSkin() {
        return skin;
    }

    /**
     * <ul>
     * <li>Only lowercase letters</li>
     * <li>Use null for default skin</li>
     * <li>Make sure skin exists</li>
     * </ul>
     *
     * @param skin The nullable skin name.
     */
    public void setSkin(String skin) {
        if (skin == null)
            skin = "default";

        this.skin = skin;
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    @Override
    @NotNull public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
