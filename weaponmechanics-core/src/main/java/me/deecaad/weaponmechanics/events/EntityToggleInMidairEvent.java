package me.deecaad.weaponmechanics.events;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EntityToggleInMidairEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity livingEntity;
    private final boolean isInMidair;

    /**
     * Called when player goes midair or lands.
     *
     * @param livingEntity the livingEntity used in event
     * @param isInMidair is in midair
     */
    public EntityToggleInMidairEvent(LivingEntity livingEntity, boolean isInMidair) {
        this.livingEntity = livingEntity;
        this.isInMidair = isInMidair;
    }

    /**
     * @return the living entity
     */
    public LivingEntity getLivingEntity() {
        return this.livingEntity;
    }

    /**
     * @return true if player goes midair and false if lands
     */
    public boolean isInMidair() {
        return this.isInMidair;
    }

    @Override
    @NotNull public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}