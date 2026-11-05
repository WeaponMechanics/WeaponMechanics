package me.deecaad.weaponmechanics.weapon.shoot;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.deecaad.core.file.Configuration;
import me.deecaad.core.mechanics.CastData;
import me.deecaad.core.mechanics.MechanicManager;
import me.deecaad.core.utils.NumberUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.WeaponHandler;
import me.deecaad.weaponmechanics.weapon.explode.Explosion;
import me.deecaad.weaponmechanics.weapon.explode.ExplosionTrigger;
import me.deecaad.weaponmechanics.weapon.trigger.TriggerType;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import me.deecaad.weaponmechanics.wrappers.HandData;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class CookHandler implements Listener {

    // The use animation starts on the tick after the interact event
    private static final int USE_START_GRACE_TICKS = 3;

    private final WeaponHandler weaponHandler;

    public CookHandler(WeaponHandler weaponHandler) {
        this.weaponHandler = weaponHandler;
    }

    public boolean isCookingWeapon(String weaponTitle) {
        if (weaponTitle == null) {
            return false;
        }

        Configuration config = WeaponMechanics.getInstance().getWeaponConfigurations();
        if (!config.getBoolean(weaponTitle + ".Shoot.Cooking.Enable")) {
            return false;
        }

        return getFuseTicks(weaponTitle) > 0;
    }

    public boolean tryStart(EntityWrapper entityWrapper, String weaponTitle, ItemStack weaponStack, EquipmentSlot slot, boolean dualWield) {
        if (!(entityWrapper.getEntity() instanceof Player player)) {
            return false;
        }

        if (!isCookingWeapon(weaponTitle)) {
            return false;
        }

        boolean mainHand = slot == EquipmentSlot.HAND;
        HandData handData = entityWrapper.getHandData(mainHand);
        if (handData.isCooking()) {
            return true;
        }

        if (handData.isUsingFullAuto() || handData.isUsingBurst()) {
            return false;
        }

        if (!weaponHandler.getInfoHandler().hasPermission(player, weaponTitle)) {
            return false;
        }

        Configuration config = WeaponMechanics.getInstance().getWeaponConfigurations();
        int delayBetweenShots = config.getInt(weaponTitle + ".Shoot.Delay_Between_Shots");
        if (delayBetweenShots != 0 && !NumberUtil.hasMillisPassed(handData.getLastShotTime(), delayBetweenShots)) {
            return true;
        }

        MechanicManager startMechanics = config.getObject(weaponTitle + ".Shoot.Cooking.Start_Mechanics", MechanicManager.class);
        if (startMechanics != null) {
            startMechanics.use(new CastData(player, weaponTitle, weaponStack));
        }

        int fuseTicks = getFuseTicks(weaponTitle);
        handData.setCooked(0, false);
        TaskImplementation<Void> task = WeaponMechanics.getInstance().getFoliaScheduler().entity(player).runAtFixedRate(new Consumer<>() {
            int ticks = 0;

            @Override
            public void accept(TaskImplementation<Void> scheduledTask) {
                ticks++;

                if (!isHoldingWeapon(player, slot, weaponTitle)) {
                    handData.stopCooking();
                    handData.setCooked(0, false);
                    return;
                }

                if (ticks >= fuseTicks) {
                    finish(entityWrapper, weaponTitle, slot, dualWield, fuseTicks, true);
                    return;
                }

                if (ticks > USE_START_GRACE_TICKS && !isUsingItem(player, slot)) {
                    finish(entityWrapper, weaponTitle, slot, dualWield, ticks, false);
                }
            }
        }, 1, 1);

        if (task == null) {
            return false;
        }

        handData.setCookTask(task);
        return true;
    }

    private void finish(EntityWrapper entityWrapper, String weaponTitle, EquipmentSlot slot, boolean dualWield, int cookedTicks, boolean cookedInHand) {
        Player player = (Player) entityWrapper.getEntity();
        HandData handData = entityWrapper.getHandData(slot == EquipmentSlot.HAND);
        handData.stopCooking();
        handData.setCooked(cookedTicks, cookedInHand);

        if (cookedInHand) {
            player.clearActiveItem();
        }

        ItemStack weaponStack = slot == EquipmentSlot.HAND ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();
        boolean shot = weaponHandler.getShootHandler().shootWithoutTrigger(entityWrapper, weaponTitle, weaponStack, slot, TriggerType.RIGHT_CLICK, dualWield);
        if (!shot) {
            handData.setCooked(0, false);
        }
    }

    private boolean isHoldingWeapon(Player player, EquipmentSlot slot, String weaponTitle) {
        if (!player.isOnline() || player.isDead()) {
            return false;
        }

        ItemStack held = slot == EquipmentSlot.HAND ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();
        return weaponTitle.equals(weaponHandler.getInfoHandler().getWeaponTitle(held, false));
    }

    private boolean isUsingItem(Player player, EquipmentSlot slot) {
        return player.isHandRaised() && player.getHandRaised() == slot;
    }

    private int getFuseTicks(String weaponTitle) {
        Explosion explosion = WeaponMechanics.getInstance().getWeaponConfigurations().getObject(weaponTitle + ".Explosion", Explosion.class);
        if (explosion == null || explosion.getDetonation() == null) {
            return 0;
        }

        if (!explosion.getDetonation().getTriggers().contains(ExplosionTrigger.SPAWN)) {
            return 0;
        }

        return explosion.getDetonation().getDelay();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void cancelVanillaTrident(ProjectileLaunchEvent e) {
        if (!(e.getEntity() instanceof Trident trident) || !(trident.getShooter() instanceof Player)) {
            return;
        }

        String weaponTitle = weaponHandler.getInfoHandler().getWeaponTitle(trident.getItemStack(), false);
        if (isCookingWeapon(weaponTitle)) {
            e.setCancelled(true);
        }
    }
}
