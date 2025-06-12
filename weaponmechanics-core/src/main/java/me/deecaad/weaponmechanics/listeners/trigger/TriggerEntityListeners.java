package me.deecaad.weaponmechanics.listeners.trigger;

import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.events.EntityToggleInMidairEvent;
import me.deecaad.weaponmechanics.events.EntityToggleStandEvent;
import me.deecaad.weaponmechanics.events.EntityToggleSwimEvent;
import me.deecaad.weaponmechanics.events.EntityToggleWalkEvent;
import me.deecaad.weaponmechanics.utils.MetadataKey;
import me.deecaad.weaponmechanics.weapon.WeaponHandler;
import me.deecaad.weaponmechanics.weapon.trigger.TriggerType;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class TriggerEntityListeners implements Listener {

    private WeaponHandler weaponHandler;

    public TriggerEntityListeners(WeaponHandler weaponHandler) {
        this.weaponHandler = weaponHandler;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void damageMonitor(EntityDamageByEntityEvent e) {
        Entity victim = e.getEntity();

        // Only when victim has been damaged by WM projectile
        // when Use_Vanilla_Damaging is true
        if (!MetadataKey.VANILLA_DAMAGE.has(victim))
            return;
        MetadataKey.VANILLA_DAMAGE.remove(victim);

        if (e.isCancelled()) {
            // If cancelled set this new meta to let WM know not to use mechanics anymore
            MetadataKey.CANCELLED_DAMAGE.set(victim, null);
        }
    }

    @EventHandler
    public void damage(EntityDamageByEntityEvent e) {
        Entity victim = e.getEntity();

        if (MetadataKey.VANILLA_DAMAGE.has(victim)) {
            // Don't try melee nor cancel the damage if this entity was just hit by
            // WM projectile while using ´Use_Vanilla_Damaging´.
            return;
        }

        // WeaponMechanics fires an EntityDamageByEntityEvent
        if (victim.hasMetadata("doing-weapon-damage"))
            return;

        // MythicMobs damage skill support. Holding a gun while dealing damage normally triggers this
        if (victim.hasMetadata("skill-damage"))
            return;

        EntityDamageEvent.DamageCause cause = e.getCause();
        boolean isSweep = cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK;
        if (cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK && !isSweep)
            return;
        if (WeaponMechanics.getInstance().getConfiguration().getBoolean("Disabled_Trigger_Checks.Right_And_Left_Click"))
            return;

        Entity damager = e.getDamager();
        if (!damager.getType().isAlive() || !victim.getType().isAlive())
            return;

        EntityWrapper entityWrapper = WeaponMechanics.getInstance().getEntityWrapper((LivingEntity) damager, true);
        if (entityWrapper == null)
            return;

        LivingEntity livingEntity = entityWrapper.getEntity();

        EntityEquipment entityEquipment = livingEntity.getEquipment();
        if (entityEquipment == null)
            return;

        ItemStack mainStack = entityEquipment.getItemInMainHand();
        String mainWeapon = weaponHandler.getInfoHandler().getWeaponTitle(mainStack, false);

        ItemStack offStack = entityEquipment.getItemInOffHand();
        String offWeapon = weaponHandler.getInfoHandler().getWeaponTitle(offStack, false);

        if (mainWeapon == null && offWeapon == null)
            return;

        if (mainWeapon != null) {
            // Cancel melee with weapons by default
            e.setCancelled(true);
        }

        // When sweep hit we don't want to do actual melee casts
        if (isSweep)
            return;

        if (weaponHandler.getInfoHandler().denyDualWielding(TriggerType.MELEE,
            livingEntity.getType() == EntityType.PLAYER ? (Player) livingEntity : null, mainWeapon, offWeapon))
            return;

        if (mainStack.getAmount() != 0) {
            weaponHandler.tryUses(entityWrapper, mainWeapon, mainStack, EquipmentSlot.HAND, TriggerType.LEFT_CLICK, mainWeapon != null && offWeapon != null, (LivingEntity) victim);

            weaponHandler.tryUses(entityWrapper, mainWeapon, mainStack, EquipmentSlot.HAND, TriggerType.MELEE, mainWeapon != null && offWeapon != null, (LivingEntity) victim);
        }
    }

    @EventHandler
    public void death(EntityDeathEvent e) {
        Entity entity = e.getEntity();

        // Don't remove when its player
        if (entity.getType() == EntityType.PLAYER)
            return;

        // If entity had EntityWrapper data, remove it
        WeaponMechanics.getInstance().removeEntityWrapper(e.getEntity());
    }

    @EventHandler
    public void toggleWalk(EntityToggleWalkEvent e) {
        // Whether this is used its checked already in MoveTask class
        weaponHandler.useTrigger(e.getLivingEntity(), e.isWalking() ? TriggerType.START_WALK : TriggerType.END_WALK, false);
    }

    @EventHandler
    public void toggleInMidair(EntityToggleInMidairEvent e) {
        // Whether this is used its checked already in MoveTask class
        weaponHandler.useTrigger(e.getLivingEntity(), e.isInMidair() ? TriggerType.START_IN_MIDAIR : TriggerType.END_IN_MIDAIR, false);
    }

    @EventHandler
    public void toggleStand(EntityToggleStandEvent e) {
        // Whether this is used its checked already in MoveTask class
        weaponHandler.useTrigger(e.getLivingEntity(), e.isStanding() ? TriggerType.START_STAND : TriggerType.END_STAND, false);
    }

    @EventHandler
    public void toggleSwim(EntityToggleSwimEvent e) {
        // Whether this is used its checked already in MoveTask class
        weaponHandler.useTrigger(e.getLivingEntity(), e.isSwimming() ? TriggerType.START_SWIM : TriggerType.END_SWIM, false);
    }

    @EventHandler(ignoreCancelled = true)
    public void toggleGlide(EntityToggleGlideEvent e) {
        if (WeaponMechanics.getInstance().getConfiguration().getBoolean("Disabled_Trigger_Checks.Glide"))
            return;

        if (!e.getEntityType().isAlive())
            return;
        weaponHandler.useTrigger((LivingEntity) e.getEntity(), e.isGliding() ? TriggerType.START_GLIDE : TriggerType.END_GLIDE, false);
    }
}