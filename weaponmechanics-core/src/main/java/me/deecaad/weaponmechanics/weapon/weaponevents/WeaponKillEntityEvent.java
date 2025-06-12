package me.deecaad.weaponmechanics.weapon.weaponevents;

import me.deecaad.weaponmechanics.utils.MetadataKey;
import me.deecaad.weaponmechanics.weapon.damage.AssistData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Called when a victim is killed from a {@link WeaponDamageEntityEvent}.
 */
public class WeaponKillEntityEvent extends WeaponEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity victim;
    private final WeaponDamageEntityEvent damageEvent;

    public WeaponKillEntityEvent(String weaponTitle, ItemStack weaponItem, LivingEntity weaponUser, EquipmentSlot hand,
        LivingEntity victim, WeaponDamageEntityEvent damageEvent) {
        super(weaponTitle, weaponItem, weaponUser, hand);
        this.victim = victim;
        this.damageEvent = damageEvent;
    }

    public LivingEntity getVictim() {
        return victim;
    }

    /**
     * Returns the {@link WeaponDamageEntityEvent} that was called before killing the victim
     * ({@link #getVictim()}).
     *
     * @return The damage event called right before this.
     */
    public WeaponDamageEntityEvent getDamageEvent() {
        return damageEvent;
    }

    /**
     * Return the map containing the player who assisted as key and actual assist data as value. Assist
     * data of player is map containing the weapon title as key and damage info as value. DamageInfo has
     * methods {@link AssistData.DamageInfo#getDamage()}, {@link AssistData.DamageInfo#getWeaponStack()}
     * and {@link AssistData.DamageInfo#getLastHitTime()}.
     *
     * @return the nullable assist data
     */
    @Nullable public Map<Player, Map<String, AssistData.DamageInfo>> getAssistData() {
        return !MetadataKey.ASSIST_DATA.has(victim)
            ? null
            : ((AssistData) MetadataKey.ASSIST_DATA.get(victim).get(0).value()).getAssists(victim.getKiller());
    }

    @Override
    @NotNull public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}