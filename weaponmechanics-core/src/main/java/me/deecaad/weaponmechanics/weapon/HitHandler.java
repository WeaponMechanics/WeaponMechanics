package me.deecaad.weaponmechanics.weapon;

import me.deecaad.core.compatibility.CompatibilityAPI;
import me.deecaad.core.compatibility.HitBox;
import me.deecaad.core.compatibility.worldguard.WorldGuardCompatibility;
import me.deecaad.core.file.Configuration;
import me.deecaad.core.utils.LogLevel;
import me.deecaad.core.utils.StringUtil;
import me.deecaad.core.utils.ray.BlockTraceResult;
import me.deecaad.core.utils.ray.EntityTraceResult;
import me.deecaad.core.utils.ray.RayTraceResult;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.damage.DamagePoint;
import me.deecaad.weaponmechanics.weapon.damage.MeleeDamageSource;
import me.deecaad.weaponmechanics.weapon.damage.ProjectileDamageSource;
import me.deecaad.weaponmechanics.weapon.damage.WeaponDamageSource;
import me.deecaad.weaponmechanics.weapon.explode.Explosion;
import me.deecaad.weaponmechanics.weapon.explode.ExplosionTrigger;
import me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile.WeaponProjectile;
import me.deecaad.weaponmechanics.weapon.weaponevents.ProjectileHitBlockEvent;
import me.deecaad.weaponmechanics.weapon.weaponevents.ProjectileHitEntityEvent;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponMeleeHitEvent;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import me.deecaad.weaponmechanics.wrappers.HandData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class HitHandler {

    /**
     * Simple final modifier for front hit adjusting. Basically -0.2 means that 20% of hit box's front
     */
    private static final double FRONT_HIT = -0.2;

    private WeaponHandler weaponHandler;

    public HitHandler(WeaponHandler weaponHandler) {
        this.weaponHandler = weaponHandler;
    }

    /**
     * @param result the ray trace result used
     * @param projectile the projectile which caused hit
     * @return true if hit was cancelled
     */
    public boolean handleHit(RayTraceResult result, WeaponProjectile projectile) {
        if (result instanceof BlockTraceResult blockHit)
            return handleBlockHit(blockHit, projectile);
        else if (result instanceof EntityTraceResult entityHit)
            return handleEntityHit(entityHit, projectile);

        // DeeCaaD wrote that there is the possibility for non-block, non-entity
        // collisions. Was he on crack? Maybe... Or maybe I (CJCrafter) don't understand...
        // Return true to cancel the hit
        return true;
    }

    /**
     * @return true if hit was cancelled
     */
    public boolean handleMeleeHit(EntityTraceResult result, LivingEntity shooter, Vector shooterDirection, String weaponTitle, ItemStack weaponStack, EquipmentSlot slot) {
        // Handle worldguard flags
        WorldGuardCompatibility worldGuard = CompatibilityAPI.getWorldGuardCompatibility();
        Location loc = result.getHitLocation().clone().toLocation(shooter.getWorld());

        if (!worldGuard.testFlag(loc, shooter instanceof Player ? (Player) shooter : null, "weapon-damage")) { // is cancelled check
            Object obj = worldGuard.getValue(loc, "weapon-damage-message");
            if (obj != null && !obj.toString().isEmpty()) {
                shooter.sendMessage(StringUtil.colorBukkit(obj.toString()));
            }
            return true;
        }

        Configuration config = WeaponMechanics.getInstance().getWeaponConfigurations();
        LivingEntity livingEntity = result.getEntity();
        int meleeHitDelay = config.getInt(weaponTitle + ".Melee.Melee_Hit_Delay") / 50;
        boolean backstab = livingEntity.getLocation().getDirection().dot(shooterDirection) > 0.0;
        WeaponMeleeHitEvent event = new WeaponMeleeHitEvent(weaponTitle, weaponStack, shooter, slot, livingEntity, meleeHitDelay, backstab);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled())
            return true;

        if (event.getMeleeHitDelay() != 0) {
            EntityWrapper wrapper = WeaponMechanics.getInstance().getEntityWrapper(shooter);
            HandData hand = wrapper.getMainHandData(); // always mainhand for melee

            hand.setLastMeleeTime(System.currentTimeMillis());
        }

        DamagePoint point = getDamagePoint(result, shooterDirection);
        WeaponDamageSource source = new MeleeDamageSource(shooter, weaponTitle, weaponStack, point, backstab);
        double baseDamage = config.getDouble(weaponTitle + ".Damage.Base_Damage");
        return !weaponHandler.getDamageHandler().tryUse(source, livingEntity, baseDamage, slot);
    }

    private boolean handleBlockHit(BlockTraceResult result, WeaponProjectile projectile) {
        ProjectileHitBlockEvent hitBlockEvent = new ProjectileHitBlockEvent(projectile, result.getBlock(), result.getHitFace(), result.getHitLocation().clone());
        Bukkit.getPluginManager().callEvent(hitBlockEvent);
        if (hitBlockEvent.isCancelled())
            return true;

        Explosion explosion = WeaponMechanics.getInstance().getWeaponConfigurations().getObject(projectile.getWeaponTitle() + ".Explosion", Explosion.class);
        if (explosion != null) {
            // When the explosion is instant, the origin is the hit location
            Location origin = null;
            if (explosion.getDetonation().getDelay() <= 1)
                origin = result.getHitLocation().clone().toLocation(projectile.getWorld());
            explosion.handleExplosion(projectile.getShooter(), origin, projectile, ExplosionTrigger.BLOCK);
        }

        return false;
    }

    private boolean handleEntityHit(EntityTraceResult result, WeaponProjectile projectile) {
        // Handle worldguard flags
        WorldGuardCompatibility worldGuard = CompatibilityAPI.getWorldGuardCompatibility();
        Location loc = result.getHitLocation().toLocation(projectile.getWorld());
        LivingEntity shooter = projectile.getShooter();

        if (!worldGuard.testFlag(loc, shooter instanceof Player ? (Player) shooter : null, "weapon-damage")) { // is cancelled check
            Object obj = worldGuard.getValue(loc, "weapon-damage-message");
            if (obj != null && !obj.toString().isEmpty() && shooter != null) {
                shooter.sendMessage(StringUtil.colorBukkit(obj.toString()));
            }
            return true;
        }

        LivingEntity livingEntity = result.getEntity();
        boolean backstab = livingEntity.getLocation().getDirection().dot(projectile.getMotion()) > 0.0;

        DamagePoint hitPoint = getDamagePoint(result, shooter.getLocation().getDirection());

        ProjectileHitEntityEvent hitEntityEvent = new ProjectileHitEntityEvent(projectile, livingEntity, result.getHitLocation().clone(), hitPoint, backstab);
        Bukkit.getPluginManager().callEvent(hitEntityEvent);
        if (hitEntityEvent.isCancelled())
            return true;

        hitPoint = hitEntityEvent.getPoint();
        backstab = hitEntityEvent.isBackStab();

        double baseDamage = WeaponMechanics.getInstance().getWeaponConfigurations().getDouble(projectile.getWeaponTitle() + ".Damage.Base_Damage");
        WeaponDamageSource source = new ProjectileDamageSource(projectile, hitPoint);
        if (!weaponHandler.getDamageHandler().tryUse(source, livingEntity, baseDamage, projectile.getHand())) {
            // Damage was cancelled
            return true;
        }

        Explosion explosion = WeaponMechanics.getInstance().getWeaponConfigurations().getObject(projectile.getWeaponTitle() + ".Explosion", Explosion.class);
        if (explosion != null) {
            // When the explosion is instant, the origin is the hit location
            Location origin = null;
            if (explosion.getDetonation().getDelay() <= 1)
                origin = result.getHitLocation().clone().toLocation(projectile.getWorld());

            explosion.handleExplosion(projectile.getShooter(), origin, projectile, ExplosionTrigger.ENTITY);
        }

        return false;
    }

    /**
     * @param result the hit result
     * @param normalizedMotion the normalized direction
     * @return the damage point or null if tried to cast when living entity was not defined
     */
    private DamagePoint getDamagePoint(EntityTraceResult result, Vector normalizedMotion) {
        LivingEntity livingEntity = result.getEntity();
        Configuration basicConfiguration = WeaponMechanics.getInstance().getConfiguration();

        EntityType type = livingEntity.getType();
        double entityHeight = livingEntity.getHeight();

        double hitY = result.getHitLocation().getY();

        HitBox hitBox = result.getHitBox();
        double maxY = hitBox.getMaxY();

        // Check HEAD
        double head = basicConfiguration.getDouble("Entity_Hitboxes." + type.name() + "." + DamagePoint.HEAD.name());
        if (head > 0.0 && maxY - (entityHeight * head) < hitY) {
            return DamagePoint.HEAD;
        }

        // Check BODY
        double body = basicConfiguration.getDouble("Entity_Hitboxes." + type.name() + "." + DamagePoint.BODY.name());
        if (body >= 1.0 || body > 0.0 && maxY - (entityHeight * (head + body)) < hitY) {

            boolean horizontalEntity = basicConfiguration.getBoolean("Entity_Hitboxes." + type.name() + ".Horizontal_Entity", false);
            boolean arms = basicConfiguration.getBoolean("Entity_Hitboxes." + type.name() + "." + DamagePoint.ARMS.name(), false);
            if (horizontalEntity || arms) {
                Vector normalizedEntityDirection = livingEntity.getLocation().getDirection();

                if (horizontalEntity && !hitBox.cloneDimensions().expand(normalizedEntityDirection, FRONT_HIT).collides(result.getHitLocation())) {
                    // Basically removes directionally 0.2 from this entity hitbox and check if the hit location is
                    // still in the hitbox
                    return DamagePoint.HEAD;
                }

                if (arms && Math.abs(normalizedMotion.clone().setY(0).dot(normalizedEntityDirection.setY(0))) < 0.5) {
                    return DamagePoint.ARMS;
                }
            }

            return DamagePoint.BODY;
        }

        // Check LEGS
        double legs = basicConfiguration.getDouble("Entity_Hitboxes." + type.name() + "." + DamagePoint.LEGS.name());
        if (legs > 0.0 && maxY - (entityHeight * (head + body + legs)) < hitY) {
            return DamagePoint.LEGS;
        }

        // Check FEET
        double feet = basicConfiguration.getDouble("Entity_Hitboxes." + type.name() + "." + DamagePoint.FEET.name());
        if (feet > 0.0) { // No need for actual check since it can't be HEAD, BODY or LEGS anymore so only option left is FEET
            return DamagePoint.FEET;
        }

        WeaponMechanics.getInstance().debugger.warning("Something unexpected happened and HEAD, BODY, LEGS or FEET wasn't valid",
            "This should never happen. Using BODY as default value...",
            "This happened with entity type " + type + ".");
        return DamagePoint.BODY;
    }
}