package me.deecaad.weaponmechanics.events;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EntityToggleWalkEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity livingEntity;
    private final boolean isWalking;

    /**
     * Called when player starts or stops walking.
     *
     * @param livingEntity the living entity used in event
     * @param isWalking is walking
     */
    public EntityToggleWalkEvent(LivingEntity livingEntity, boolean isWalking) {
        this.livingEntity = livingEntity;
        this.isWalking = isWalking;
    }

    /**
     * @return the living entity
     */
    public LivingEntity getLivingEntity() {
        return this.livingEntity;
    }

    /**
     * @return true if player starts walking and false if stops walking
     */
    public boolean isWalking() {
        return this.isWalking;
    }

    @Override
    @NotNull public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}