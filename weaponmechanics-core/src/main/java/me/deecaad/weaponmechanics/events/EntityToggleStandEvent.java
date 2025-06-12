package me.deecaad.weaponmechanics.events;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EntityToggleStandEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity livingEntity;
    private final boolean isStanding;

    /**
     * Called when player starts or stops standing.
     *
     * @param livingEntity the livingEntity used in event
     * @param isStanding is standing
     */
    public EntityToggleStandEvent(LivingEntity livingEntity, boolean isStanding) {
        this.livingEntity = livingEntity;
        this.isStanding = isStanding;
    }

    /**
     * @return the living entity
     */
    public LivingEntity getLivingEntity() {
        return this.livingEntity;
    }

    /**
     * @return true if player starts standing and false if stops standing
     */
    public boolean isStanding() {
        return this.isStanding;
    }

    @Override
    @NotNull public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}