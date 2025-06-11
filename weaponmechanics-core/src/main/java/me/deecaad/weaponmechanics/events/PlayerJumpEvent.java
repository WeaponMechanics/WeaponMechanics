package me.deecaad.weaponmechanics.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerJumpEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final boolean doubleJump;

    /**
     * Called when player jumps.
     *
     * @param player the player used in event
     * @param doubleJump whether or not this was double jump
     */
    public PlayerJumpEvent(Player player, boolean doubleJump) {
        this.player = player;
        this.doubleJump = doubleJump;
    }

    /**
     * @return the player
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * @return true if jump is double jump
     */
    public boolean isDoubleJump() {
        return this.doubleJump;
    }

    @Override
    @NotNull public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}