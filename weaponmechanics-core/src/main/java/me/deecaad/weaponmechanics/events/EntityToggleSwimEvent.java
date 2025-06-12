package me.deecaad.weaponmechanics.events;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EntityToggleSwimEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity livingEntity;
    private final boolean isSwimming;

    /**
     * Called when player starts or stops swimming
     *
     * @param livingEntity the livingEntity used in event
     * @param isSwimming is swimming
     */
    public EntityToggleSwimEvent(LivingEntity livingEntity, boolean isSwimming) {
        this.livingEntity = livingEntity;
        this.isSwimming = isSwimming;
    }

    /**
     * @return the living entity
     */
    public LivingEntity getLivingEntity() {
        return this.livingEntity;
    }

    /**
     * @return true if player goes swimming and false if stops
     */
    public boolean isSwimming() {
        return this.isSwimming;
    }

    @Override
    @NotNull public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}