package me.deecaad.weaponmechanics.weapon.damage;

import me.deecaad.core.file.Configuration;
import me.deecaad.core.utils.NumberUtil;
import me.deecaad.core.utils.RandomUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.compatibility.WeaponCompatibilityAPI;
import me.deecaad.weaponmechanics.utils.MetadataKey;
import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.attribute.Attribute;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.LazyMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import com.google.common.base.Function;

import java.util.EnumMap;
import java.util.Set;

public class DamageUtil {

    private static final EquipmentSlot[] EQUIPMENT_SLOTS = EquipmentSlot.values();

    /**
     * Do not let anyone instantiate this class
     */
    private DamageUtil() {
    }

    /**
     * @param source What caused the damage.
     * @param victim The victim being damaged.
     * @param damage The amount of damage to apply
     * @return true if damage was cancelled
     */
    public static boolean apply(@NotNull WeaponDamageSource source, @NotNull LivingEntity victim, double damage) {
        Configuration config = WeaponMechanics.getInstance().getConfiguration();

        // Skip armor stands for better plugin compatibility
        if (victim instanceof ArmorStand armorStand) {
            if (config.getBoolean("Damage.Ignore_Armor_Stand.Always"))
                return true;
            if (config.getBoolean("Damage.Ignore_Armor_Stand.Marker") && armorStand.isMarker())
                return true;
            if (config.getBoolean("Damage.Ignore_Armor_Stand.Invisible") && armorStand.isInvisible())
                return true;
        }

        if (victim.isInvulnerable() || victim.isDead())
            return true;

        // Make sure the player is not in creative or spectator, can only damage survival/adventure
        if (victim instanceof Player player) {
            GameMode gamemode = player.getGameMode();

            if (gamemode == GameMode.CREATIVE || gamemode == GameMode.SPECTATOR)
                return true;
        }

        // Use enderman teleport API added in 1.20.1
        else if (victim instanceof Enderman enderman) {

            // 64 is the value minecraft uses
            int teleportAttempts = config.getInt("Damage.Enderman_Teleport_Attempts", 64);

            boolean isTeleported = false;
            for (int i = 0; i < teleportAttempts && !isTeleported; i++) {
                if (source.getShooter() != null && enderman.getTarget() == source.getShooter()) {
                    isTeleported = enderman.teleportTowards(source.getShooter());
                } else {
                    isTeleported = enderman.teleport();
                }
            }

            // When the enderman does not teleport away, this is probably because
            // the user disabled the enderman teleportation in config.yml (set
            // attempts to 0), so we should damage the enderman instead.
            if (isTeleported)
                return true;
        }

        // Skip damaging if possible
        if (damage < 0)
            damage = 0;

        if (config.getBoolean("Damage.Use_Vanilla_Damaging", false)) {
            if (damage == 0)
                return true;

            // VANILLA_DAMAGE is used to make sure we don't do melee trigger checks
            // on EntityDamageByEntityEvent. null doesn't matter, the metadata key
            // is still applied to the entity.
            MetadataKey.VANILLA_DAMAGE.set(victim, null);

            victim.damage(damage, source.getShooter());

            // If the EntityDamageByEntityEvent was cancelled, then we should skip
            // everything else.
            if (MetadataKey.CANCELLED_DAMAGE.has(victim)) {
                MetadataKey.CANCELLED_DAMAGE.remove(victim);
                return true;
            }

            // Vanilla thing to allow constant hits from projectiles
            victim.setNoDamageTicks(0);
            return false;
        }

        // Used to check if a plugin changed the amount of damage
        double tempDamage = damage;

        var shooter = source.getShooter();
        if (shooter != null) {

            var cause = switch (source.getDamageType()) {
                case MELEE -> EntityDamageEvent.DamageCause.ENTITY_ATTACK;
                case PROJECTILE -> EntityDamageEvent.DamageCause.PROJECTILE;
                case EXPLOSION -> EntityDamageEvent.DamageCause.ENTITY_EXPLOSION;
            };

            var bukkitDamageType = switch (source.getDamageType()) {
                case MELEE -> shooter instanceof Player ? DamageType.PLAYER_ATTACK : DamageType.MOB_ATTACK;
                case PROJECTILE -> DamageType.MOB_PROJECTILE;
                case EXPLOSION -> shooter instanceof Player ? DamageType.PLAYER_EXPLOSION : DamageType.EXPLOSION;
            };

            var bukkitSource = DamageSource.builder(bukkitDamageType);

            if (source.getDamageLocation() != null) {
                bukkitSource.withDamageLocation(source.getDamageLocation());
            }

            // We don't have a real Bukkit projectile entity here, so shooter is used
            bukkitSource.withCausingEntity(shooter).withDirectEntity(shooter);

            // Mutable function because other plugins can call event.setDamage(...)
            var damageModifiers = new EnumMap<EntityDamageEvent.DamageModifier, Double>(EntityDamageEvent.DamageModifier.class);
            damageModifiers.put(EntityDamageEvent.DamageModifier.BASE, damage);

            Function<Double, Double> identity = d -> d;
            var damageModifierFunctions =
                    new EnumMap<EntityDamageEvent.DamageModifier, Function<? super Double, Double>>(EntityDamageEvent.DamageModifier.class);
            damageModifierFunctions.put(EntityDamageEvent.DamageModifier.BASE, identity);

            var event = new EntityDamageByEntityEvent(
                    shooter,
                    victim,
                    cause,
                    bukkitSource.build(),
                    damageModifiers,
                    damageModifierFunctions,
                    false
            );

            victim.setMetadata("doing-weapon-damage", new LazyMetadataValue(WeaponMechanics.getInstance(), () -> true));
            try {
                Bukkit.getPluginManager().callEvent(event);
            } finally {
                victim.removeMetadata("doing-weapon-damage", WeaponMechanics.getInstance());
            }

            if (event.isCancelled())
                return true;

            // Honor other plugins that might change this
            damage = event.getFinalDamage();
            if (!Double.isFinite(damage) || damage <= 0.0)
                return true;
        }

        // If a plugin modified the damage, and set it to 0.0, just cancel
        if (tempDamage != damage && damage == 0.0) {
            return true;
        }

        // Calculate the amount of damage to absorption hearts, and
        // determine how much damage is left over to deal to the victim
        double absorption = victim.getAbsorptionAmount();
        double absorbed = Math.max(0, absorption - damage);
        victim.setAbsorptionAmount(absorbed);
        damage = Math.max(damage - absorption, 0);

        double oldHealth = victim.getHealth();

        // Apply any remaining damage to the victim, and handle internals
        victim.setLastDamage(damage);
        if (source.getShooter() != null) {
            WeaponCompatibilityAPI.getWeaponCompatibility().logDamage(victim, source.getShooter(), oldHealth, damage, false);
            if (source.getShooter() instanceof Player player) {
                WeaponCompatibilityAPI.getWeaponCompatibility().setKiller(victim, player);
            }
        }

        // Determine the correct angle that the damage
        Location sourceLocation = source.getDamageLocation();
        float angle = 0f;
        if (sourceLocation != null) {
            Location victimLocation = victim.getLocation();
            double dx = sourceLocation.getX() - victimLocation.getX();
            double dz = sourceLocation.getZ() - victimLocation.getZ();
            angle = (float) Math.toDegrees(Math.atan2(dz, dx)) - victimLocation.getYaw();
            angle = NumberUtil.normalizeDegrees(angle) + 180f;
        }
        victim.playHurtAnimation(angle);

        double newHealth = NumberUtil.clamp(oldHealth - damage, 0, victim.getAttribute(Attribute.MAX_HEALTH).getValue());
        boolean killed = newHealth <= 0.0;
        boolean resurrected = false;

        // Try use totem of undying
        if (killed) {
            resurrected = tryUseTotemOfUndying(victim);
            killed = !resurrected;
        }

        // When the victim is resurrected via a totem, their health will already be set to 1.0
        if (!resurrected)
            victim.setHealth(newHealth);

        // Statistics
        if (victim instanceof Player player) {
            if (absorbed >= 0.1)
                player.incrementStatistic(Statistic.DAMAGE_ABSORBED, Math.max(1, Math.round((float) absorbed * 10)));
            if (damage >= 0.1)
                player.incrementStatistic(Statistic.DAMAGE_TAKEN, Math.max(1, Math.round((float) damage * 10)));
            if (killed && source.getShooter() != null)
                player.incrementStatistic(Statistic.ENTITY_KILLED_BY, source.getShooter().getType());
        }
        if (source.getShooter() instanceof Player player) {
            if (absorbed >= 0.1)
                player.incrementStatistic(Statistic.DAMAGE_DEALT_ABSORBED, Math.max(1, Math.round((float) absorbed * 10)));
            if (damage >= 0.1)
                player.incrementStatistic(Statistic.DAMAGE_DEALT, Math.max(1, Math.round((float) damage * 10)));
            if (killed)
                player.incrementStatistic(Statistic.KILL_ENTITY, victim.getType());
        }

        return false;
    }

    public static void damageArmor(@NotNull LivingEntity victim, @NotNull WeaponDamageSource source, int amount) {

        // If the damage amount is 0, we can skip the calculations
        if (amount <= 0)
            return;

        // Stores which armors should be damaged
        EntityEquipment equipment = victim.getEquipment();
        if (equipment == null)
            return;

        if (source.getEffectedEquipment() != null) {
            for (EquipmentSlot slot : EQUIPMENT_SLOTS) {
                if (!source.getEffectedEquipment().test(slot))
                    continue;

                damage(equipment, slot, amount);
            }
        }
    }

    private static void damage(EntityEquipment equipment, EquipmentSlot slot, int amount) {
        ItemStack armor = switch (slot) {
            case HEAD -> equipment.getHelmet();
            case CHEST -> equipment.getChestplate();
            case LEGS -> equipment.getLeggings();
            case FEET -> equipment.getBoots();
            default -> null;
        };

        // All items implement Damageable (since Spigot is stupid). We use this check
        // to see if an item is *actually* damageable.
        if (armor == null || "AIR".equals(armor.getType().name()) || armor.getType().getMaxDurability() == 0)
            return;

        ItemMeta meta = armor.getItemMeta();
        if (meta == null)
            return;

        // Do not attempt to damage armor that is unbreakable
        if (meta.isUnbreakable())
            return;

        // Formula taken from Unbreaking enchant code
        int level = meta.getEnchantLevel(Enchantment.UNBREAKING);
        boolean skipDamage = !RandomUtil.chance(0.6 + 0.4 / (level + 1));
        if (skipDamage)
            return;

        if (meta instanceof Damageable damageable) {
            damageable.setDamage(damageable.getDamage() + amount);
            armor.setItemMeta(meta);

            if (damageable.getDamage() >= armor.getType().getMaxDurability())
                armor.setAmount(0);
        }

        // Getting an ItemStack from an EntityEquipment copies the item... we
        // need to set the item.
        switch (slot) {
            case HEAD -> equipment.setHelmet(armor);
            case CHEST -> equipment.setChestplate(armor);
            case LEGS -> equipment.setLeggings(armor);
            case FEET -> equipment.setBoots(armor);
        }
    }

    /**
     * @param cause the cause entity (shooter of projectile)
     * @param victim the victim
     * @return true only if cause can harm victim
     */
    public static boolean canHarmScoreboardTeams(@NotNull LivingEntity cause, @NotNull LivingEntity victim) {

        // Owner invulnerability is handled separately.
        if (cause.equals(victim))
            return true;

        // Only check scoreboard teams for players
        if (cause.getType() != EntityType.PLAYER || victim.getType() != EntityType.PLAYER)
            return true;

        Scoreboard shooterScoreboard = ((Player) cause).getScoreboard();

        Set<Team> teams = shooterScoreboard.getTeams();
        if (teams.isEmpty())
            return true;

        for (Team team : teams) {
            Set<String> entries = team.getEntries();

            // Seems like this has to be also checked...
            if (!entries.contains(cause.getName()))
                continue;

            // If not in same team -> continue
            if (!entries.contains(victim.getName()))
                continue;

            // Now we know they're in same team.
            // -> If friendly is not enabled
            // --> they can't harm each other
            if (!team.allowFriendlyFire()) {
                // This approach only checks first same team WHERE friendly fire is enabled
                return false;
            }
        }

        return true;
    }

    /**
     * Attempts to use a totem of undying on the entity. This will return false if the entity does not
     * have a totem of undying, or if the {@link EntityResurrectEvent} is cancelled. This method will
     * return true if the totem affect is used.
     *
     * @param entity The non-null entity to use the totem on.
     * @return true if the totem was used.
     */
    public static boolean tryUseTotemOfUndying(@NotNull LivingEntity entity) {

        // Check if the entity has a totem of undying.
        EntityEquipment equipment = entity.getEquipment();
        ItemStack mainHand = equipment == null ? null : equipment.getItemInMainHand();
        ItemStack offHand = equipment == null ? null : equipment.getItemInOffHand();
        EquipmentSlot hand = null;
        if (mainHand != null && mainHand.getType() == Material.TOTEM_OF_UNDYING) {
            hand = EquipmentSlot.HAND;
        } else if (offHand != null && offHand.getType() == Material.TOTEM_OF_UNDYING) {
            hand = EquipmentSlot.OFF_HAND;
        }

        // This is how Spigot handles resurrection. They always call the event,
        // and cancel the event if there is no totem.
        ItemStack totem = hand == null ? null : (hand == EquipmentSlot.HAND ? mainHand : offHand);
        EntityResurrectEvent event = new EntityResurrectEvent(entity, hand);
        event.setCancelled(hand == null);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled())
            return false;

        // 'totem' can still be null here, since plugins may modify the cancellation state/inventory
        if (totem != null && totem.getType() == Material.TOTEM_OF_UNDYING) {
            totem.setAmount(totem.getAmount() - 1);
        }

        // Attempt to award stats
        if (entity instanceof Player player) {
            player.incrementStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING);
            // TODO convert this to Bukkit Code
            // CriteriaTriggers.USED_TOTEM.trigger(entityplayer, itemstack);
        }

        // Not quite ideal, as this fires 1 event PER potion effect, but otherwise 1to1 copy from vanilla
        // code
        entity.setHealth(1.0D);
        for (PotionEffect potion : entity.getActivePotionEffects())
            entity.removePotionEffect(potion.getType());
        entity.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0));
        entity.playEffect(EntityEffect.PROTECTED_FROM_DEATH);
        return true;
    }
}