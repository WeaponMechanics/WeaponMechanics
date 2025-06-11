package me.deecaad.weaponmechanics.weapon.weaponevents;

import me.deecaad.weaponmechanics.weapon.shoot.recoil.RecoilProfile;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WeaponRecoilEvent extends WeaponEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private float recoilMeanX;
    private float recoilMeanY;
    private float recoilVarianceX;
    private float recoilVarianceY;
    private float recoilSpeed;
    private float damping;
    private float dampingRecovery;
    private float smoothingFactor;
    private float maxRecoilAccum;

    private boolean isCancelled;

    public WeaponRecoilEvent(
        @NotNull String weaponTitle,
        @NotNull ItemStack weaponItem,
        @Nullable LivingEntity weaponUser,
        @NotNull EquipmentSlot hand,
        @NotNull RecoilProfile recoilProfile) {
        super(weaponTitle, weaponItem, weaponUser, hand);

        this.recoilMeanX = recoilProfile.getRecoilMeanX();
        this.recoilMeanY = recoilProfile.getRecoilMeanY();
        this.recoilVarianceX = recoilProfile.getRecoilVarianceX();
        this.recoilVarianceY = recoilProfile.getRecoilVarianceY();
        this.recoilSpeed = recoilProfile.getRecoilSpeed();
        this.damping = recoilProfile.getDamping();
        this.dampingRecovery = recoilProfile.getDampingRecovery();
        this.smoothingFactor = recoilProfile.getSmoothingFactor();
        this.maxRecoilAccum = recoilProfile.getMaxRecoilAccum();
    }

    public float getRecoilMeanX() {
        return recoilMeanX;
    }

    public void setRecoilMeanX(float recoilMeanX) {
        this.recoilMeanX = recoilMeanX;
    }

    public float getRecoilMeanY() {
        return recoilMeanY;
    }

    public void setRecoilMeanY(float recoilMeanY) {
        this.recoilMeanY = recoilMeanY;
    }

    public float getRecoilVarianceX() {
        return recoilVarianceX;
    }

    public void setRecoilVarianceX(float recoilVarianceX) {
        this.recoilVarianceX = recoilVarianceX;
    }

    public float getRecoilVarianceY() {
        return recoilVarianceY;
    }

    public void setRecoilVarianceY(float recoilVarianceY) {
        this.recoilVarianceY = recoilVarianceY;
    }

    public float getRecoilSpeed() {
        return recoilSpeed;
    }

    public void setRecoilSpeed(float recoilSpeed) {
        this.recoilSpeed = recoilSpeed;
    }

    public float getDamping() {
        return damping;
    }

    public void setDamping(float damping) {
        this.damping = damping;
    }

    public float getDampingRecovery() {
        return dampingRecovery;
    }

    public void setDampingRecovery(float dampingRecovery) {
        this.dampingRecovery = dampingRecovery;
    }

    public float getSmoothingFactor() {
        return smoothingFactor;
    }

    public void setSmoothingFactor(float smoothingFactor) {
        this.smoothingFactor = smoothingFactor;
    }

    public float getMaxRecoilAccum() {
        return maxRecoilAccum;
    }

    public void setMaxRecoilAccum(float maxRecoilAccum) {
        this.maxRecoilAccum = maxRecoilAccum;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
