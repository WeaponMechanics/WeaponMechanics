package me.deecaad.weaponmechanics.compatibility;

import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public interface IWeaponCompatibility {

    /**
     * Rotates player's camera rotation with given values. Absolute true means that yaw and pitch will
     * be SET to the given values. While as absolute false means that yaw and pitch is ADDED to the
     * given values.
     *
     * <p>
     * Having absolute true may cause that player's movement glitches a bit.
     *
     * @param player the player whose camera rotation to rotate
     * @param yaw absolute or relative rotation on the X axis, in degrees
     * @param pitch absolute or relative rotation on the Y axis, in degrees
     * @param absolute whether to use absolute rotation
     */
    void modifyCameraRotation(Player player, float yaw, float pitch, boolean absolute);

    /**
     * Logs "fake" damage to the given <code>victim</code>'s <code>CombatTracker</code>. This is
     * important for death messages, and any plugins that may use minecraft's built in combat tracker.
     *
     * @param victim The entity receiving the damage
     * @param source The entity giving the damage
     * @param health The health of the entity
     * @param damage The damage being applied to the entity
     * @param isMelee Whether or not this is a melee attack (And not a projectile)
     */
    void logDamage(LivingEntity victim, LivingEntity source, double health, double damage, boolean isMelee);

    default EntityDamageByEntityEvent newEntityDamageByEntityEvent(LivingEntity victim, LivingEntity source, double damage, boolean isMelee) {
        return new EntityDamageByEntityEvent(
            source,
            victim,
            isMelee ? EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK : EntityDamageByEntityEvent.DamageCause.PROJECTILE,
            DamageSource.builder(
                isMelee
                    ? (source.getType() == EntityType.PLAYER ? DamageType.PLAYER_ATTACK : DamageType.MOB_ATTACK)
                    : DamageType.MOB_PROJECTILE).build(),
            damage);
    }

    /**
     * Sets which player killed the <code>victim</code>. Entities that are killed by players will drop
     * their experience.
     *
     * @param victim The entity that died
     * @param killer The killer
     */
    void setKiller(LivingEntity victim, Player killer);
}