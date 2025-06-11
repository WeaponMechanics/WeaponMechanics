package me.deecaad.weaponmechanics.weapon;

import me.deecaad.core.utils.LogLevel;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.listeners.trigger.TriggerPlayerListeners;
import me.deecaad.weaponmechanics.weapon.damage.DamageHandler;
import me.deecaad.weaponmechanics.weapon.info.InfoHandler;
import me.deecaad.weaponmechanics.weapon.melee.MeleeHandler;
import me.deecaad.weaponmechanics.weapon.reload.ReloadHandler;
import me.deecaad.weaponmechanics.weapon.scope.ScopeHandler;
import me.deecaad.weaponmechanics.weapon.shoot.ShootHandler;
import me.deecaad.weaponmechanics.weapon.skin.SkinHandler;
import me.deecaad.weaponmechanics.weapon.stats.StatsHandler;
import me.deecaad.weaponmechanics.weapon.trigger.AmmoTypeSwitchTriggerListener;
import me.deecaad.weaponmechanics.weapon.trigger.SelectiveFireTriggerListener;
import me.deecaad.weaponmechanics.weapon.trigger.TriggerListener;
import me.deecaad.weaponmechanics.weapon.trigger.TriggerType;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to generally handle weapon functions
 */
public class WeaponHandler {

    private final InfoHandler infoHandler;
    private final ShootHandler shootHandler;
    private final ReloadHandler reloadHandler;
    private final ScopeHandler scopeHandler;
    private final DamageHandler damageHandler;
    private final SkinHandler skinHandler;
    private final MeleeHandler meleeHandler;
    private final StatsHandler statsHandler;
    private final HitHandler hitHandler;
    private final List<TriggerListener> triggerListeners;

    public WeaponHandler() {
        infoHandler = new InfoHandler(this);
        shootHandler = new ShootHandler(this);
        reloadHandler = new ReloadHandler(this);
        scopeHandler = new ScopeHandler(this);
        damageHandler = new DamageHandler(this);
        skinHandler = new SkinHandler(this);
        meleeHandler = new MeleeHandler(this);
        statsHandler = new StatsHandler(this);
        hitHandler = new HitHandler(this);
        triggerListeners = new ArrayList<>(5);
        fillTriggerListeners();
    }

    /**
     * Checks main and off hand items if they're weapons and uses them based on trigger. This is used
     * with the exceptions off {@link TriggerPlayerListeners#dropItem(PlayerDropItemEvent)},
     * {@link TriggerPlayerListeners#interact(PlayerInteractEvent)} and
     * {@link TriggerPlayerListeners#swapHandItems(PlayerSwapHandItemsEvent)}.
     *
     * @param livingEntity the living entity which caused trigger
     * @param triggerType the trigger type
     * @param autoConvert whether the weapon items should be converted
     */
    public void useTrigger(LivingEntity livingEntity, TriggerType triggerType, boolean autoConvert) {

        // This because for all players this should be always nonnull
        // -> No auto add true to deny entity wrappers being added for unnecessary entities (they can also
        // trigger certain
        EntityWrapper entityWrapper = WeaponMechanics.getInstance().getEntityWrapper(livingEntity, true);
        if (entityWrapper == null)
            return;

        if (livingEntity.getType() == EntityType.PLAYER && ((Player) livingEntity).getGameMode() == GameMode.SPECTATOR)
            return;

        EntityEquipment entityEquipment = livingEntity.getEquipment();
        if (entityEquipment == null)
            return;

        ItemStack mainStack = entityEquipment.getItemInMainHand();
        String mainWeapon = infoHandler.getWeaponTitle(mainStack, autoConvert);

        ItemStack offStack = entityEquipment.getItemInOffHand();
        String offWeapon = infoHandler.getWeaponTitle(offStack, autoConvert);

        if (mainWeapon == null && offWeapon == null)
            return;

        if (infoHandler.denyDualWielding(triggerType, livingEntity.getType() == EntityType.PLAYER ? (Player) livingEntity : null, mainWeapon, offWeapon))
            return;

        boolean dualWield = mainWeapon != null && offWeapon != null;

        if (mainWeapon != null)
            tryUses(entityWrapper, mainWeapon, mainStack, EquipmentSlot.HAND, triggerType, dualWield, null);

        if (offWeapon != null)
            tryUses(entityWrapper, offWeapon, offStack, EquipmentSlot.OFF_HAND, triggerType, dualWield, null);
    }

    /**
     * Tries all uses in this exact order
     * 
     * <pre>{@code
     * 1) Shoot
     * 2) Reload
     * 3) Scope
     * 4) Selective fire
     * 5) Ammo type switch
     * 6) Any other registered TriggerListener in order they were added
     * }</pre>
     *
     * @param entityWrapper the entity which caused trigger
     * @param weaponTitle the weapon title involved
     * @param weaponStack the weapon stack involved
     * @param slot the weapon slot used
     * @param triggerType the trigger which caused this
     * @param dualWield whether this was dual wield
     * @param victim if there is known victim
     */
    public void tryUses(EntityWrapper entityWrapper, String weaponTitle, ItemStack weaponStack, EquipmentSlot slot, TriggerType triggerType, boolean dualWield, @Nullable LivingEntity victim) {
        if (!weaponStack.hasItemMeta())
            return;

        try {
            for (TriggerListener listener : triggerListeners) {

                // If trigger isn't valid, continue
                if (!listener.tryUse(entityWrapper, weaponTitle, weaponStack, slot, triggerType, dualWield, victim))
                    continue;

                // Most of the time other triggers aren't allowed during same loop
                if (!listener.allowOtherTriggers()) {
                    return;
                }
            }
        } catch (Exception e) {
            WeaponMechanics.getInstance().debugger.warning("Unhandled exception while looping trigger listeners!", e);
            return;
        }

        // Only try this if any trigger wasn't valid.
        // They should update skin if required
        getSkinHandler().tryUse(triggerType, entityWrapper, weaponTitle, weaponStack, slot);
    }

    private void fillTriggerListeners() {
        triggerListeners.add(shootHandler);
        triggerListeners.add(reloadHandler);
        triggerListeners.add(scopeHandler);
        triggerListeners.add(new AmmoTypeSwitchTriggerListener());
        triggerListeners.add(new SelectiveFireTriggerListener());
    }

    /**
     * Add new trigger listener to list. Trigger listeners are used in order they're given
     *
     * @param triggerListener the new trigger listener
     */
    public void addTriggerListener(TriggerListener triggerListener) {
        if (triggerListener == null)
            throw new NullPointerException("Plugin gave null trigger listener...?");
        triggerListeners.add(triggerListener);
    }

    /**
     * @return the info handler
     */
    public InfoHandler getInfoHandler() {
        return infoHandler;
    }

    /**
     * @return the shoot handler
     */
    public ShootHandler getShootHandler() {
        return shootHandler;
    }

    /**
     * @return the reload handler
     */
    public ReloadHandler getReloadHandler() {
        return reloadHandler;
    }

    /**
     * @return the scope handler
     */
    public ScopeHandler getScopeHandler() {
        return scopeHandler;
    }

    /**
     * @return the damage handler
     */
    public DamageHandler getDamageHandler() {
        return damageHandler;
    }

    /**
     * @return the skin handler
     */
    public SkinHandler getSkinHandler() {
        return skinHandler;
    }

    /**
     * @return the melee handler
     */
    public MeleeHandler getMeleeHandler() {
        return meleeHandler;
    }

    /**
     * @return the stats handler
     */
    public StatsHandler getStatsHandler() {
        return statsHandler;
    }

    /**
     * @return the hit handler
     */
    public HitHandler getHitHandler() {
        return hitHandler;
    }
}