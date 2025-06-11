package me.deecaad.weaponmechanics.weapon.weaponevents;

import me.deecaad.weaponmechanics.weapon.damage.AssistData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class WeaponAssistEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity killed;
    private final Map<String, AssistData.DamageInfo> assists;

    /**
     * Consider using {@link WeaponKillEntityEvent} instead. Only down-side of that event is that it
     * isn't called when entity dies for other causes than weapon. This event is still called everytime
     * entity dies for any reason.
     */
    public WeaponAssistEvent(Player who, LivingEntity killed, Map<String, AssistData.DamageInfo> assists) {
        super(who);
        this.killed = killed;
        this.assists = assists;
    }

    /**
     * @return the non-null entity which was killed
     */
    public LivingEntity getKilled() {
        return killed;
    }

    /**
     * Return the map containing the weapon title as key and damage info as value. DamageInfo has
     * methods {@link AssistData.DamageInfo#getDamage()}, {@link AssistData.DamageInfo#getWeaponStack()}
     * and {@link AssistData.DamageInfo#getLastHitTime()}.
     *
     * @return the non-null map of assists
     */
    public Map<String, AssistData.DamageInfo> getAssistInfo() {
        return assists;
    }

    @Override
    @NotNull public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}