package me.deecaad.weaponmechanics.listeners.trigger;

import com.cjcrafter.foliascheduler.util.MinecraftVersions;
import me.deecaad.core.file.Configuration;
import me.deecaad.core.utils.NumberUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.events.PlayerJumpEvent;
import me.deecaad.weaponmechanics.weapon.WeaponHandler;
import me.deecaad.weaponmechanics.weapon.trigger.TriggerType;
import me.deecaad.weaponmechanics.wrappers.PlayerWrapper;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class TriggerPlayerListeners implements Listener {

    private final WeaponHandler weaponHandler;

    public TriggerPlayerListeners(WeaponHandler weaponHandler) {
        this.weaponHandler = weaponHandler;
    }

    @EventHandler
    public void join(PlayerJoinEvent e) {
        // Add PlayerWrapper
        PlayerWrapper playerWrapper = WeaponMechanics.getInstance().getPlayerWrapper(e.getPlayer());
        weaponHandler.getStatsHandler().load(playerWrapper);
    }

   @EventHandler
public void quit(PlayerQuitEvent e) {
    WeaponMechanics plugin = WeaponMechanics.getInstance();

    // Remove wrapper first if plugin is disabled
    if (!plugin.isEnabled()) {
        plugin.removeEntityWrapper(e.getPlayer());
        return;
    }

    // Only save if database is initialized
    if (plugin.getDatabase() != null) {
        try {
            PlayerWrapper wrapper = plugin.getPlayerWrapper(e.getPlayer());
            if (wrapper != null) {
                weaponHandler.getStatsHandler().save(wrapper, false);
            }
        } catch (Exception ignored) {
            // Safe fail during reload
        }
    }

    plugin.removeEntityWrapper(e.getPlayer());
}

    @EventHandler(ignoreCancelled = true)
    public void toggleSneak(PlayerToggleSneakEvent e) {
        if (WeaponMechanics.getInstance().getConfiguration().getBoolean("Disabled_Trigger_Checks.Sneak"))
            return;

        Player player = e.getPlayer();
        boolean isSneaking = e.isSneaking();

        if (isSneaking && WeaponMechanics.getInstance().getPlayerWrapper(player).didDoubleSneak()) {
            weaponHandler.useTrigger(player, TriggerType.DOUBLE_SNEAK, false);
            // DOUBLE_SNEAK and START_SNEAK can be called nearly at same time
        }

        weaponHandler.useTrigger(player, isSneaking ? TriggerType.START_SNEAK : TriggerType.END_SNEAK, false);
    }

    @EventHandler(ignoreCancelled = true)
    public void toggleSprint(PlayerToggleSprintEvent e) {
        if (WeaponMechanics.getInstance().getConfiguration().getBoolean("Disabled_Trigger_Checks.Sprint"))
            return;

        weaponHandler.useTrigger(e.getPlayer(), e.isSprinting() ? TriggerType.START_SPRINT : TriggerType.END_SPRINT, false);
    }

    @EventHandler
    public void jump(PlayerJumpEvent e) {
        // Whether this is used its checked already in MoveTask class
        weaponHandler.useTrigger(e.getPlayer(), e.isDoubleJump() ? TriggerType.DOUBLE_JUMP : TriggerType.JUMP, false);
    }

    @EventHandler(ignoreCancelled = true)
    public void toggleFlight(PlayerToggleFlightEvent e) {
        if (WeaponMechanics.getInstance().getConfiguration().getBoolean("Disabled_Trigger_Checks.Double_Jump"))
            return;

        Player player = e.getPlayer();
        GameMode gameMode = player.getGameMode();
        if (gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE) {
            e.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
            Bukkit.getPluginManager().callEvent(new PlayerJumpEvent(player, true));
        }
    }

    @EventHandler
    public void interactAt(PlayerInteractAtEntityEvent e) {

        // Used only when interacting with armor stand

        Entity entity = e.getRightClicked();
        if (entity.getType() != EntityType.ARMOR_STAND)
            return;

        // And when the armor stand is invisible
        // -> Likely e.g. model engine mob

        ArmorStand armorStand = (ArmorStand) entity;
        if (armorStand.isVisible())
            return;

        // Still don't consider as shoot interaction WHEN in creative or spectator

        Player player = e.getPlayer();
        EntityEquipment playerEquipment = player.getEquipment();
        GameMode gameMode = player.getGameMode();
        if ((gameMode == GameMode.SPECTATOR || gameMode == GameMode.CREATIVE) || playerEquipment == null)
            return;

        ItemStack mainStack = playerEquipment.getItemInMainHand();
        String mainWeapon = weaponHandler.getInfoHandler().getWeaponTitle(mainStack, false);

        ItemStack offStack = playerEquipment.getItemInOffHand();
        String offWeapon = weaponHandler.getInfoHandler().getWeaponTitle(offStack, false);

        if (mainWeapon == null && offWeapon == null)
            return;

        e.setCancelled(true);

        interact(new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, mainStack, null, null, e.getHand()));
    }

    @EventHandler
    public void interact(PlayerInteractEvent e) {
        Action action = e.getAction();
        Player player = e.getPlayer();
        EntityEquipment playerEquipment = player.getEquipment();
        Configuration config = WeaponMechanics.getInstance().getConfiguration();
        Configuration weaponConfig = WeaponMechanics.getInstance().getWeaponConfigurations();

        // I don't think ignoreCancelled = true works in this event properly
        if (player.getGameMode() == GameMode.SPECTATOR || playerEquipment == null)
            return;
        if (action == Action.PHYSICAL || e.useItemInHand() == Event.Result.DENY)
            return;
        if (config.getBoolean("Disabled_Trigger_Checks.Right_And_Left_Click"))
            return;

        // Basically this just cancel double call to player interact event
        EquipmentSlot hand = e.getHand();

        // Only if main hand is air (off hand can be whatever)
        if (playerEquipment.getItemInMainHand().getType() == Material.AIR) {

            // Check if the action was right click block AND hand used was main hand
            // -> Cancel
            if (action == Action.RIGHT_CLICK_BLOCK && hand == EquipmentSlot.HAND) {
                return;
            }
            // This basically means that hand is now OFF_HAND, but it doesn't let HAND calls pass

        } else if (hand == EquipmentSlot.OFF_HAND) {
            // If main hand had item, then we can always just cancel OFF_HAND call since HAND is guaranteed to
            // be used
            return;
        }

        PlayerWrapper playerWrapper = WeaponMechanics.getInstance().getPlayerWrapper(player);

        boolean rightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        if (rightClick) {
            playerWrapper.rightClicked();
        } else if (MinecraftVersions.BUZZY_BEES.isAtLeast() && !NumberUtil.hasMillisPassed(playerWrapper.getLastDropWeaponTime(), 25)) {
            // Fixes bug in 1.15+ where item dropping causes player to left click
            // Basically checks if less than 25 millis has passed since weapon item drop
            return;
        }

        ItemStack mainStack = playerEquipment.getItemInMainHand();
        String mainWeapon = weaponHandler.getInfoHandler().getWeaponTitle(mainStack, true);

        ItemStack offStack = playerEquipment.getItemInOffHand();
        String offWeapon = weaponHandler.getInfoHandler().getWeaponTitle(offStack, true);

        if (mainWeapon == null && offWeapon == null)
            return;

        if ((mainWeapon != null && weaponConfig.getBoolean(mainWeapon + ".Info.Cancel.Block_Interactions")
            || offWeapon != null && weaponConfig.getBoolean(offWeapon + ".Info.Cancel.Block_Interactions"))) {
            e.setUseInteractedBlock(Event.Result.DENY);
        }

        if ((mainWeapon != null && weaponConfig.getBoolean(mainWeapon + ".Info.Cancel.Item_Interactions")
            || offWeapon != null && weaponConfig.getBoolean(offWeapon + ".Info.Cancel.Item_Interactions"))) {
            e.setUseItemInHand(Event.Result.DENY);
        }

        boolean dualWield = mainWeapon != null && offWeapon != null;

        if (rightClick) {
            if (weaponHandler.getInfoHandler().denyDualWielding(TriggerType.RIGHT_CLICK, player, mainWeapon, offWeapon))
                return;

            if (mainWeapon != null)
                weaponHandler.tryUses(playerWrapper, mainWeapon, mainStack, EquipmentSlot.HAND, TriggerType.RIGHT_CLICK, dualWield, null);

            if (offWeapon != null)
                weaponHandler.tryUses(playerWrapper, offWeapon, offStack, EquipmentSlot.OFF_HAND, TriggerType.RIGHT_CLICK, dualWield, null);
            return;
        }

        if (weaponHandler.getInfoHandler().denyDualWielding(TriggerType.LEFT_CLICK, player, mainWeapon, offWeapon))
            return;

        if (mainWeapon != null) {
            weaponHandler.tryUses(playerWrapper, mainWeapon, mainStack, EquipmentSlot.HAND, TriggerType.LEFT_CLICK, dualWield, null);

            if (mainStack.getAmount() != 0) {
                // Try melee if main weapon isn't null
                weaponHandler.tryUses(playerWrapper, mainWeapon, mainStack, EquipmentSlot.HAND, TriggerType.MELEE, dualWield, null);
            }
        }

        if (offWeapon != null)
            weaponHandler.tryUses(playerWrapper, offWeapon, offStack, EquipmentSlot.OFF_HAND, TriggerType.LEFT_CLICK, dualWield, null);
    }

    /**
     * This is simply used to cancel player arm swing animation from OTHER players. It can't be
     * cancelled from the player doing the arm swing.
     */
    @EventHandler(ignoreCancelled = true)
    public void animation(PlayerAnimationEvent e) {
        if (e.getAnimationType() != PlayerAnimationType.ARM_SWING)
            return;
        if (WeaponMechanics.getInstance().getConfiguration().getBoolean("Disabled_Trigger_Checks.Right_And_Left_Click"))
            return;

        Player player = e.getPlayer();
        EntityEquipment playerEquipment = player.getEquipment();
        if (player.getGameMode() == GameMode.SPECTATOR || playerEquipment == null)
            return;

        // In 1.15+, there is a "feature" where item dropping causes player to left-click
        PlayerWrapper playerWrapper = WeaponMechanics.getInstance().getPlayerWrapper(player);
        if (MinecraftVersions.BUZZY_BEES.isAtLeast() && !NumberUtil.hasMillisPassed(playerWrapper.getLastDropWeaponTime(), 25)) {
            e.setCancelled(true);
            return;
        }

        ItemStack mainStack = playerEquipment.getItemInMainHand();
        String mainWeapon = weaponHandler.getInfoHandler().getWeaponTitle(mainStack, false);

        if (mainWeapon != null && WeaponMechanics.getInstance().getWeaponConfigurations().getBoolean(mainWeapon + ".Info.Cancel.Arm_Swing_Animation")) {
            e.setCancelled(true);
            return;
        }

        ItemStack offStack = playerEquipment.getItemInOffHand();
        String offWeapon = weaponHandler.getInfoHandler().getWeaponTitle(offStack, false);

        if (offWeapon != null && WeaponMechanics.getInstance().getWeaponConfigurations().getBoolean(offWeapon + ".Info.Cancel.Arm_Swing_Animation")) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void dropItem(PlayerDropItemEvent e) {
        Player player = e.getPlayer();
        if (WeaponMechanics.getInstance().getConfiguration().getBoolean("Disabled_Trigger_Checks.Drop_Item"))
            return;

        EntityEquipment playerEquipment = player.getEquipment();
        if (player.getGameMode() == GameMode.SPECTATOR || playerEquipment == null)
            return;

        // If this item drop was when inventory was open
        PlayerWrapper playerWrapper = WeaponMechanics.getInstance().getPlayerWrapper(player);
        if (!NumberUtil.hasMillisPassed(playerWrapper.getLastInventoryDropTime(), 50))
            return;

        ItemStack mainStack = e.getItemDrop().getItemStack();
        String mainWeapon = weaponHandler.getInfoHandler().getWeaponTitle(mainStack, false);

        ItemStack offStack = playerEquipment.getItemInOffHand();
        String offWeapon = weaponHandler.getInfoHandler().getWeaponTitle(offStack, false);

        if (mainWeapon == null && offWeapon == null)
            return;

        // If players are dead, don't do anything... This cancel can sometimes let players keep their
        // weapons
        if (!player.isDead()) {
            boolean cancelMainHand = mainWeapon != null && WeaponMechanics.getInstance().getWeaponConfigurations().getBoolean(mainWeapon + ".Info.Cancel.Drop_Item");
            boolean cancelOffHand = offWeapon != null && WeaponMechanics.getInstance().getWeaponConfigurations().getBoolean(offWeapon + ".Info.Cancel.Drop_Item");
            if (cancelOffHand || cancelMainHand) {
                e.setCancelled(true);
            }
        }

        if (weaponHandler.getInfoHandler().denyDualWielding(TriggerType.DROP_ITEM, player, mainWeapon, offWeapon))
            return;

        boolean dualWield = mainWeapon != null && offWeapon != null;

        if (mainWeapon != null) {
            playerWrapper.droppedWeapon();

            // This due to sometimes the instance changes in item drop...
            // - 1 item in slot when dropping -> reference changes
            // - 2 items or more in slot when dropping -> reference stays same
            WeaponMechanics.getInstance().getFoliaScheduler().entity(player).run(() -> {
                weaponHandler.tryUses(playerWrapper, mainWeapon, playerEquipment.getItemInMainHand(), EquipmentSlot.HAND, TriggerType.DROP_ITEM, dualWield, null);
            });
        }

        if (offWeapon != null) {
            playerWrapper.droppedWeapon();
            WeaponMechanics.getInstance().getFoliaScheduler().entity(player).run(() -> {
                weaponHandler.tryUses(playerWrapper, offWeapon, playerEquipment.getItemInOffHand(), EquipmentSlot.OFF_HAND, TriggerType.DROP_ITEM, dualWield, null);
            });
        }
    }

    // Event priority LOW to ensure that this is ran before.
    // Weapon listeners PlayerSwapHandItemsEvent is ran.
    // Basically lower priority means that it will be one of the first EventHandlers to run.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void swapHandItems(PlayerSwapHandItemsEvent e) {
        if (WeaponMechanics.getInstance().getConfiguration().getBoolean("Disabled_Trigger_Checks.Swap_Main_And_Hand_Items"))
            return;

        Player player = e.getPlayer();
        EntityEquipment playerEquipment = player.getEquipment();

        if (player.getGameMode() == GameMode.SPECTATOR || playerEquipment == null)
            return;

        PlayerWrapper playerWrapper = WeaponMechanics.getInstance().getPlayerWrapper(player);

        ItemStack toMain = e.getMainHandItem();
        String toMainWeapon = weaponHandler.getInfoHandler().getWeaponTitle(toMain, false);

        ItemStack toOff = e.getOffHandItem();
        String toOffWeapon = weaponHandler.getInfoHandler().getWeaponTitle(toOff, false);
        if (toMainWeapon == null && toOffWeapon == null)
            return;

        if ((toMainWeapon != null && WeaponMechanics.getInstance().getWeaponConfigurations().getBoolean(toMainWeapon + ".Info.Cancel.Swap_Hands"))
            || (toOffWeapon != null && WeaponMechanics.getInstance().getWeaponConfigurations().getBoolean(toOffWeapon + ".Info.Cancel.Swap_Hands"))) {

            e.setCancelled(true);

            toOff = playerEquipment.getItemInMainHand();
            toMain = playerEquipment.getItemInOffHand();
        } else {
            playerWrapper.getMainHandData().cancelTasks();
            playerWrapper.getOffHandData().cancelTasks();
        }

        boolean dualWield = toMainWeapon != null && toOffWeapon != null;

        if (isValid(toMain)) {
            // SWAP_TO_MAIN_HAND
            if (weaponHandler.getInfoHandler().denyDualWielding(TriggerType.SWAP_HANDS, player, toMainWeapon, toOffWeapon))
                return;

            // Only check off hand going to main hand
            if (toMainWeapon != null) {
                final ItemStack finalToMain = toMain;
                WeaponMechanics.getInstance().getFoliaScheduler().entity(player).run(() -> {
                    weaponHandler.tryUses(playerWrapper, toMainWeapon, finalToMain, EquipmentSlot.OFF_HAND, TriggerType.SWAP_HANDS, dualWield, null);
                });
            }
        }
        if (isValid(toOff)) {
            // SWAP_TO_OFF_HAND
            if (weaponHandler.getInfoHandler().denyDualWielding(TriggerType.SWAP_HANDS, player, toMainWeapon, toOffWeapon))
                return;

            // Only check main hand going to off hand
            if (toOffWeapon != null) {
                final ItemStack finalToOff = toOff;
                WeaponMechanics.getInstance().getFoliaScheduler().entity(player).run(() -> {
                    weaponHandler.tryUses(playerWrapper, toOffWeapon, finalToOff, EquipmentSlot.HAND, TriggerType.SWAP_HANDS, dualWield, null);
                });
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Only need to check main-hand for breaking blocks
        ItemStack weapon = player.getInventory().getItemInMainHand();
        String weaponTitle = !isValid(weapon) ? null : weaponHandler.getInfoHandler().getWeaponTitle(weapon, false);

        if (weaponTitle != null && WeaponMechanics.getInstance().getWeaponConfigurations().getBoolean(weaponTitle + ".Info.Cancel.Break_Blocks")) {

            // WeaponMechanicsCosmetics calls the BlockBreakEvent for block
            // damage, so we need to make sure that this doesn't interfere.
            if ("WeaponMechanicsBlockDamage".equals(event.getEventName()))
                return;

            event.setCancelled(true);
        }
    }

    private boolean isValid(ItemStack itemStack) {
        return itemStack != null && itemStack.getType() != Material.AIR;
    }
}