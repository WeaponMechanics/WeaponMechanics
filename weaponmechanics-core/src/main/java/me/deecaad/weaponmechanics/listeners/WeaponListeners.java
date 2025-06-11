package me.deecaad.weaponmechanics.listeners;

import me.deecaad.core.events.EntityEquipmentEvent;
import me.deecaad.core.mechanics.CastData;
import me.deecaad.core.mechanics.MechanicManager;
import me.deecaad.core.mechanics.Mechanics;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.utils.MetadataKey;
import me.deecaad.weaponmechanics.weapon.WeaponHandler;
import me.deecaad.weaponmechanics.weapon.damage.AssistData;
import me.deecaad.weaponmechanics.weapon.info.WeaponInfoDisplay;
import me.deecaad.weaponmechanics.weapon.stats.WeaponStat;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponAssistEvent;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponEquipEvent;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import me.deecaad.weaponmechanics.wrappers.HandData;
import me.deecaad.weaponmechanics.wrappers.PlayerWrapper;
import me.deecaad.weaponmechanics.wrappers.StatsData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class WeaponListeners implements Listener {

    private WeaponHandler weaponHandler;

    public WeaponListeners(WeaponHandler weaponHandler) {
        this.weaponHandler = weaponHandler;
    }

    @EventHandler
    public void equip(EntityEquipmentEvent e) {
        if (e.isArmor())
            return;

        LivingEntity entity = (LivingEntity) e.getEntity();
        EntityWrapper entityWrapper = WeaponMechanics.getInstance().getEntityWrapper(entity);
        ItemStack weaponStack = e.getEquipped();

        // Also try auto converting to weapon
        String weaponTitle = weaponHandler.getInfoHandler().getWeaponTitle(weaponStack, true);
        boolean alreadyUsedEquipMechanics = false;

        boolean mainhand = e.getSlot() == EquipmentSlot.HAND;

        HandData handData = mainhand ? entityWrapper.getMainHandData() : entityWrapper.getOffHandData();
        handData.setCurrentWeaponTitle(weaponTitle);

        if (weaponTitle != null) {
            if (e.getEntityType() == EntityType.PLAYER) {
                PlayerWrapper playerWrapper = (PlayerWrapper) entityWrapper;
                if (playerWrapper.getStatsData() != null)
                    playerWrapper.getStatsData().add(weaponTitle, WeaponStat.EQUIP_TIMES, 1);

                WeaponInfoDisplay weaponInfoDisplay = WeaponMechanics.getInstance().getWeaponConfigurations().getObject(weaponTitle + ".Info.Weapon_Info_Display", WeaponInfoDisplay.class);
                if (weaponInfoDisplay != null)
                    weaponInfoDisplay.send(playerWrapper, e.getSlot(), mainhand ? weaponStack : null, !mainhand ? weaponStack : null);
            }

            weaponHandler.getSkinHandler().tryUse(entityWrapper, weaponTitle, weaponStack, e.getSlot());

            MechanicManager equipMechanics = WeaponMechanics.getInstance().getWeaponConfigurations().getObject(weaponTitle + ".Info.Weapon_Equip_Mechanics", MechanicManager.class);
            if (equipMechanics != null) {
                equipMechanics.use(new CastData(entity, weaponTitle, weaponStack));
                alreadyUsedEquipMechanics = true;
            }

            handData.setLastEquipTime(System.currentTimeMillis());

            Bukkit.getPluginManager().callEvent(new WeaponEquipEvent(weaponTitle, weaponStack, entity, e.getSlot() == EquipmentSlot.HAND));
        } else {
            // If not-weapon was equipped, cancel all tasks
            handData.cancelTasks(false);
        }

        ItemStack dequipped = e.getDequipped();
        String dequippedWeapon = weaponHandler.getInfoHandler().getWeaponTitle(dequipped, false);
        if (dequippedWeapon != null) {

            // Don't use holster mechanics is equip mechanics were already used
            if (!alreadyUsedEquipMechanics) {
                MechanicManager holsterMechanics = WeaponMechanics.getInstance().getWeaponConfigurations().getObject(dequippedWeapon + ".Info.Weapon_Holster_Mechanics", MechanicManager.class);
                if (holsterMechanics != null)
                    holsterMechanics.use(new CastData(entity, dequippedWeapon, dequipped));
            }

            // Make sure to cancel tasks for the dequipped weapon
            handData.cancelTasks(false);

            weaponHandler.getSkinHandler().tryUse(entityWrapper, dequippedWeapon, dequipped, e.getSlot(), true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void itemHeld(PlayerItemHeldEvent e) {
        Player player = e.getPlayer();
        EntityWrapper entityWrapper = WeaponMechanics.getInstance().getEntityWrapper(player);
        entityWrapper.getMainHandData().cancelTasks();
        // No need to cancel off hand tasks since this is only called when changing held slot
        // Unless player is now dual wielding
        EntityEquipment entityEquipment = player.getEquipment();
        ItemStack nextSlot = player.getInventory().getItem(e.getNewSlot());
        if (entityEquipment.getItemInOffHand().getType() != Material.AIR && nextSlot != null && nextSlot.getType() != Material.AIR) {
            entityWrapper.getOffHandData().cancelTasks(true);
        }
    }

    @EventHandler
    public void death(EntityDeathEvent e) {
        LivingEntity entity = e.getEntity();
        if (MetadataKey.ASSIST_DATA.has(entity)) {
            AssistData allData = (AssistData) MetadataKey.ASSIST_DATA.get(entity).get(0).value();
            Map<Player, Map<String, AssistData.DamageInfo>> assistData = allData.getAssists(entity.getKiller());
            if (assistData != null) {
                assistData.forEach((player, data) -> {
                    StatsData statsData = WeaponMechanics.getInstance().getPlayerWrapper(player).getStatsData();
                    if (statsData != null) {
                        if (entity.getType() == EntityType.PLAYER) {
                            data.keySet().forEach((weaponTitle) -> statsData.add(weaponTitle, WeaponStat.PLAYER_ASSISTS, 1));
                        } else {
                            data.keySet().forEach((weaponTitle) -> statsData.add(weaponTitle, WeaponStat.OTHER_ASSISTS, 1));
                        }
                    }

                    Bukkit.getPluginManager().callEvent(new WeaponAssistEvent(player, entity, data));
                });
            }

            MetadataKey.ASSIST_DATA.remove(entity);
        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent e) {
        // Cleanup metadata on player quit
        Player player = e.getPlayer();
        if (!MetadataKey.ASSIST_DATA.has(player))
            return;
        MetadataKey.ASSIST_DATA.remove(player);
    }

    @EventHandler
    public void unload(ChunkUnloadEvent e) {
        // Small performance boost when using assists only for players
        if (WeaponMechanics.getInstance().getConfiguration().getBoolean("Assists_Event.Only_Players", true))
            return;

        // Cleanup metadata on chunk unload...
        for (Entity entity : e.getChunk().getEntities()) {
            if (!MetadataKey.ASSIST_DATA.has(entity))
                continue;
            MetadataKey.ASSIST_DATA.remove(entity);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player))
            return;
        PlayerWrapper playerWrapper = WeaponMechanics.getInstance().getPlayerWrapper(player);

        // Keep track of when last inventory click drop happens
        ClickType clickType = e.getClick();
        if (clickType == ClickType.DROP || clickType == ClickType.CONTROL_DROP
            || e.getSlot() == -999) {
            playerWrapper.inventoryDrop();
        }

        // Off hand is also considered as quickbar slot
        if (e.getSlotType() != InventoryType.SlotType.QUICKBAR)
            return;

        playerWrapper.getMainHandData().cancelTasks(true);
        playerWrapper.getOffHandData().cancelTasks(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void swapHandItems(PlayerSwapHandItemsEvent e) {
        EntityWrapper entityWrapper = WeaponMechanics.getInstance().getEntityWrapper(e.getPlayer());
        entityWrapper.getMainHandData().cancelTasks();
        entityWrapper.getOffHandData().cancelTasks();
    }
}