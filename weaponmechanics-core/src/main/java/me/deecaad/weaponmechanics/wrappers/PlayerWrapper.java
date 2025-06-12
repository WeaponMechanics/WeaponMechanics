package me.deecaad.weaponmechanics.wrappers;

import me.deecaad.core.file.Configuration;
import me.deecaad.core.utils.NumberUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.shoot.recoil.RecoilController;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * Wraps a {@link Player} object to simplify per-player data/methods that are used by
 * WeaponMechanics. Also contains useful API functionality for plugins who want to check if an
 * entity is scoped, reloading, etc.
 */
public class PlayerWrapper extends EntityWrapper {

    private final Player player;
    private long lastRightClick;
    private long lastStartSneak;
    private long lastWeaponDrop;
    private long lastInventoryDrop;
    private MessageHelper messageHelper;
    private RecoilController recoilController;
    private long lastAmmoConvert;
    private StatsData statsData;

    public PlayerWrapper(Player player) {
        super(player);
        this.player = player;
        Configuration config = WeaponMechanics.getInstance().getConfiguration();
        if (config.getBoolean("Database.Enable", true)) {
            statsData = new StatsData(player.getUniqueId());
        }

        long millisecondsBetweenRecoil = config.getInt("Recoil_Millis_Between_Rotations", 20);
        this.recoilController = new RecoilController(player);
        WeaponMechanics.getInstance().getFoliaScheduler().async().runAtFixedRate(recoilController, 0, millisecondsBetweenRecoil, TimeUnit.MILLISECONDS);
    }

    public Player getPlayer() {
        return this.player;
    }

    public void rightClicked() {
        lastRightClick = System.currentTimeMillis();
    }

    public boolean didDoubleSneak() {
        if (lastStartSneak == 0) {
            lastStartSneak = System.currentTimeMillis();

            // There hasn't yet been last sneak -> false
            return false;
        }

        if (!NumberUtil.hasMillisPassed(lastStartSneak, 500)) {
            // Double sneaked
            lastStartSneak = 0; // Reset the timer
            return true;
        }

        lastStartSneak = System.currentTimeMillis();
        return false;
    }

    public void droppedWeapon() {
        lastWeaponDrop = System.currentTimeMillis();
    }

    public long getLastDropWeaponTime() {
        return lastWeaponDrop;
    }

    public void inventoryDrop() {
        lastInventoryDrop = System.currentTimeMillis();
    }

    public long getLastInventoryDropTime() {
        return lastInventoryDrop;
    }

    public MessageHelper getMessageHelper() {
        return messageHelper == null ? messageHelper = new MessageHelper() : messageHelper;
    }

    public RecoilController getRecoilController() {
        return recoilController;
    }

    public void convertedAmmo() {
        lastAmmoConvert = System.currentTimeMillis();
    }

    public long getLastAmmoConvert() {
        return lastAmmoConvert;
    }

    @Override
    public boolean isRightClicking() {

        // When a player is blocking with a shield or a sword, then they are
        // definitely right-clicking.
        if (player.isBlocking())
            return true;

        if (player.getPing() > 215) {
            // Ping was more than 215 so lets take player's ping in account
            // when checking if it is still right clicking
            return !NumberUtil.hasMillisPassed(lastRightClick, player.getPing() + 15);
        }
        return !NumberUtil.hasMillisPassed(lastRightClick, 215);
    }

    @Override
    public boolean isSneaking() {
        return player.isSneaking();
    }

    /**
     * Returns <code>true</code> if the player is 'crawling'.
     *
     * @return <code>true</code> when the player is 'crawling'.
     */
    public boolean isCrawling() {
        if (this.player.isSneaking()) {
            return false;
        }
        if (this.player.isSwimming()) {
            return false;
        }
        if (this.player.isGliding()) {
            return false;
        }
        final double crawlingMaxHeight = 1.5;
        double hitboxHeight = this.player.getBoundingBox().getHeight();

        return hitboxHeight <= crawlingMaxHeight;
    }

    @Override
    public boolean isSprinting() {
        return player.isSprinting();
    }

    @Override
    public boolean isPlayer() {
        return true;
    }

    /**
     * @return the stats data or null if disabled or not yet synced
     */
    @Nullable public StatsData getStatsData() {
        return (statsData == null || !statsData.isSync()) ? null : statsData;
    }

    /**
     * This method might not be safe to use nor should be used. Use {@link #getStatsData()} instead. By
     * unsafe I mean that this object might not have been synced yet.
     *
     * @return the unsafe stats data object
     */
    public StatsData getStatsDataUnsafe() {
        return statsData;
    }
}
