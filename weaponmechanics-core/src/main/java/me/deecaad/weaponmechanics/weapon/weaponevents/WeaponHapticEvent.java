package me.deecaad.weaponmechanics.weapon.weaponevents;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vivecraft.api.data.VRBodyPart;

public class WeaponHapticEvent extends WeaponEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private @Nullable VRBodyPart part;
    private float duration;
    private float frequency;
    private float amplitude;
    private float delay;

    private boolean cancelled;

    public WeaponHapticEvent(String weaponTitle, ItemStack weaponStack, LivingEntity shooter, EquipmentSlot hand, @Nullable VRBodyPart part, float duration, float frequency, float amplitude, float delay) {
        super(weaponTitle, weaponStack, shooter, hand);
        this.part = part;
        this.duration = duration;
        this.frequency = frequency;
        this.amplitude = amplitude;
        this.delay = delay;
    }

    public @Nullable VRBodyPart getPart() {
        return part;
    }

    public void setPart(@Nullable VRBodyPart part) {
        this.part = part;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public float getFrequency() {
        return frequency;
    }

    public void setFrequency(float frequency) {
        this.frequency = frequency;
    }

    public float getAmplitude() {
        return amplitude;
    }

    public void setAmplitude(float amplitude) {
        this.amplitude = amplitude;
    }

    public float getDelay() {
        return delay;
    }

    public void setDelay(float delay) {
        this.delay = delay;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
