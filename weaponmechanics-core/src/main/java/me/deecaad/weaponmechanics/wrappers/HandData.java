package me.deecaad.weaponmechanics.wrappers;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.shoot.FullAutoTask;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponReloadCancelEvent;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponReloadCompleteEvent;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponStopShootingEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

public class HandData {

    private final EntityWrapper entityWrapper;
    private final boolean mainhand;

    private FullAutoTask fullAutoTask;
    private TaskImplementation<Void> fullAutoWrappedTask;
    private TaskImplementation<Void> burstTask;
    private long lastShotTime;
    private long lastScopeTime;
    private long lastEquipTime;
    private double spreadChange;
    private long lastMeleeTime;
    private long lastMeleeMissTime;

    // Save weaponstack/weaponTitle for reload complete and cancel events
    private long reloadStart;
    private final List<TaskImplementation<Void>> reloadTasks = new LinkedList<>();
    private ItemStack reloadWeaponStack;
    private String reloadWeaponTitle;

    // Save weaponstack/weapontitle for weapon stop shooting event
    private boolean firedWeaponStopShootEvent = false;
    private ItemStack lastWeaponShot;
    private String lastWeaponShotTitle;

    private ZoomData zoomData;
    private final List<TaskImplementation<Void>> firearmActionTasks = new LinkedList<>();

    private String currentWeaponTitle;

    public HandData(EntityWrapper entityWrapper, boolean mainhand) {
        this.entityWrapper = entityWrapper;
        this.mainhand = mainhand;
    }

    public EntityWrapper getEntityWrapper() {
        return entityWrapper;
    }

    public boolean isMainhand() {
        return mainhand;
    }

    /**
     * Cancels following things - Full auto - Burst - Reload tasks - Shoot firearm action task - Zooming
     * <p>
     * Does not cancel recoil task.
     */
    public void cancelTasks() {
        cancelTasks(false);
    }

    /**
     * @param trySkinUpdate whether to also try to update skin
     */
    public void cancelTasks(boolean trySkinUpdate) {
        if (fullAutoWrappedTask != null) {
            fullAutoWrappedTask.cancel();
            fullAutoWrappedTask = null;
            fullAutoTask = null;
        }
        if (burstTask != null) {
            burstTask.cancel();
            burstTask = null;
        }
        stopReloadingTasks();
        stopFirearmActionTasks();
        getZoomData().ifZoomingForceZoomOut();

        // Tasks cancelled means a weapon switch or similar, so make sure we have fired a stop shooting
        // event
        if (!firedWeaponStopShootEvent && lastWeaponShot != null && lastWeaponShotTitle != null) {
            firedWeaponStopShootEvent = true;

            Bukkit.getPluginManager().callEvent(new WeaponStopShootingEvent(lastWeaponShotTitle, lastWeaponShot, entityWrapper.getEntity(), mainhand ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND,
                lastShotTime));
        }

        if (!trySkinUpdate)
            return;

        // Try to update skin in given hand
        LivingEntity livingEntity = entityWrapper.getEntity();
        if (livingEntity.getType() == EntityType.PLAYER && ((Player) livingEntity).getGameMode() == GameMode.SPECTATOR)
            return;

        EntityEquipment entityEquipment = livingEntity.getEquipment();
        if (entityEquipment == null)
            return;

        ItemStack weaponStack;
        String weaponTitle;

        if (mainhand) {
            weaponStack = entityEquipment.getItemInMainHand();
        } else {
            weaponStack = entityEquipment.getItemInOffHand();
        }

        weaponTitle = WeaponMechanics.getInstance().getWeaponHandler().getInfoHandler().getWeaponTitle(weaponStack, false);

        if (weaponTitle == null)
            return;

        WeaponMechanics.getInstance().getWeaponHandler().getSkinHandler().tryUse(entityWrapper, weaponTitle, weaponStack, mainhand ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND);
    }

    public boolean isUsingFullAuto() {
        return fullAutoWrappedTask != null;
    }

    /**
     * If you cancel this task, be sure to call
     * {@link #setFullAutoTask(FullAutoTask, TaskImplementation)} with null and 0. Otherwise,
     * WeaponMechanics will break.
     *
     * @return The full auto task, or null.
     */
    public @Nullable FullAutoTask getFullAutoTask() {
        return fullAutoTask;
    }

    public @Nullable TaskImplementation<Void> getFullAutoWrappedTask() {
        return fullAutoWrappedTask;
    }

    public void setFullAutoTask(@Nullable FullAutoTask fullAutoTask, TaskImplementation<Void> fullAutoWrappedTask) {
        this.fullAutoTask = fullAutoTask;
        this.fullAutoWrappedTask = fullAutoWrappedTask;
    }

    public boolean isUsingBurst() {
        return burstTask != null;
    }

    public void setBurstTask(TaskImplementation<Void> burstTask) {
        this.burstTask = burstTask;
    }

    public void setLastShotTime(long lastShotTime) {
        this.lastShotTime = lastShotTime;
    }

    public long getLastShotTime() {
        return lastShotTime;
    }

    public long getLastScopeTime() {
        return lastScopeTime;
    }

    public void setLastScopeTime(long lastScopeTime) {
        this.lastScopeTime = lastScopeTime;
    }

    public long getLastReloadTime() {
        return reloadStart;
    }

    public long getLastEquipTime() {
        return lastEquipTime;
    }

    public void setLastEquipTime(long lastEquipTime) {
        this.lastEquipTime = lastEquipTime;
    }

    public double getSpreadChange() {
        return spreadChange;
    }

    public void setSpreadChange(double spreadChange) {
        this.spreadChange = spreadChange;
    }

    public long getLastMeleeTime() {
        return lastMeleeTime;
    }

    public void setLastMeleeTime(long lastMeleeTime) {
        this.lastMeleeTime = lastMeleeTime;
    }

    public long getLastMeleeMissTime() {
        return lastMeleeMissTime;
    }

    public void setLastMeleeMissTime(long lastMeleeMissTime) {
        this.lastMeleeMissTime = lastMeleeMissTime;
    }

    public void addReloadTask(TaskImplementation<Void> reloadTask) {
        if (this.reloadTasks.isEmpty()) {
            // Reload is starting
            reloadStart = System.currentTimeMillis();
        }
        this.reloadTasks.add(reloadTask);
    }

    public boolean isReloading() {
        return !reloadTasks.isEmpty();
    }

    public int getReloadElapsedTime() {
        return (int) ((System.currentTimeMillis() - reloadStart) / 50);
    }

    public void finishReload() {
        if (!reloadTasks.isEmpty()) {
            for (TaskImplementation<Void> task : reloadTasks) {
                task.cancel();
            }
            reloadTasks.clear();

            Bukkit.getPluginManager().callEvent(new WeaponReloadCompleteEvent(reloadWeaponTitle, reloadWeaponStack, entityWrapper.getEntity(), mainhand ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND));

            reloadWeaponStack = null;
            reloadWeaponTitle = null;
        }
    }

    public void stopReloadingTasks() {
        if (!reloadTasks.isEmpty()) {
            for (TaskImplementation<Void> task : reloadTasks) {
                task.cancel();
            }
            reloadTasks.clear();

            Bukkit.getPluginManager().callEvent(new WeaponReloadCancelEvent(reloadWeaponTitle, reloadWeaponStack, entityWrapper.getEntity(), mainhand ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND,
                getReloadElapsedTime()));

            reloadWeaponStack = null;
            reloadWeaponTitle = null;
        }
    }

    public void setReloadData(String weaponTitle, ItemStack weaponStack) {
        this.reloadWeaponTitle = weaponTitle;
        this.reloadWeaponStack = weaponStack;
    }

    public boolean isFiredWeaponStopShootEvent() {
        return firedWeaponStopShootEvent;
    }

    public void setFiredWeaponStopShootEvent(boolean firedWeaponStopShootEvent) {
        this.firedWeaponStopShootEvent = firedWeaponStopShootEvent;
    }

    public ItemStack getLastWeaponShot() {
        return lastWeaponShot;
    }

    public String getLastWeaponShotTitle() {
        return lastWeaponShotTitle;
    }

    public void setLastWeaponShot(String weaponTitle, ItemStack weaponStack) {
        this.firedWeaponStopShootEvent = false;
        this.lastWeaponShotTitle = weaponTitle;
        this.lastWeaponShot = weaponStack;
    }

    public ZoomData getZoomData() {
        return zoomData == null ? zoomData = new ZoomData(this) : zoomData;
    }

    /**
     * Only used with shoot firearm actions. Reload firearm actions use addReloadTask()
     */
    public void addFirearmActionTask(TaskImplementation<Void> firearmTask) {
        firearmActionTasks.add(firearmTask);
    }

    /**
     * Only used with shoot firearm actions Reload firearm actions use addReloadTask()
     */
    public boolean hasRunningFirearmAction() {
        return !firearmActionTasks.isEmpty();
    }

    /**
     * Only used with shoot firearm actions Reload firearm actions use addReloadTask()
     */
    public void stopFirearmActionTasks() {
        if (!firearmActionTasks.isEmpty()) {
            for (TaskImplementation<Void> task : firearmActionTasks) {
                task.cancel();
            }
            firearmActionTasks.clear();
        }
    }

    public String getCurrentWeaponTitle() {
        return currentWeaponTitle;
    }

    public void setCurrentWeaponTitle(String currentWeaponTitle) {
        this.currentWeaponTitle = currentWeaponTitle;
    }
}