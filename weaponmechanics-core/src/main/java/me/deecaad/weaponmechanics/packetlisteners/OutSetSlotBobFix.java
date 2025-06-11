package me.deecaad.weaponmechanics.packetlisteners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.deecaad.weaponmechanics.utils.CustomTag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OutSetSlotBobFix implements Listener, PacketListener {

    private final Map<Player, SimpleItemData> mainHand;
    private final Map<Player, SimpleItemData> offHand;

    public OutSetSlotBobFix(Plugin plugin) {
        mainHand = new HashMap<>();
        offHand = new HashMap<>();

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void drop(PlayerDropItemEvent event) {
        mainHand.put(event.getPlayer(), null);
    }

    @EventHandler
    public void click(InventoryClickEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player player))
            return;
        mainHand.put(player, null);
        offHand.put(player, null);
    }

    @EventHandler
    public void click(InventoryDragEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player player))
            return;
        mainHand.put(player, null);
        offHand.put(player, null);
    }

    @EventHandler(ignoreCancelled = true)
    public void held(PlayerItemHeldEvent e) {
        mainHand.put(e.getPlayer(), null);
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        mainHand.remove(event.getPlayer());
        offHand.remove(event.getPlayer());
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.SET_SLOT)
            return;
        // 1.21.4 has resource pack feature to disable bobbing
        if (!event.getUser().getClientVersion().isOlderThan(ClientVersion.V_1_21_4))
            return;

        Player player = event.getPlayer();
        WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);

        // 0 is the player's inventory.
        if (wrapper.getWindowId() != 0)
            return;

        int slot = wrapper.getSlot();
        boolean mainHand = slot == 36 + player.getInventory().getHeldItemSlot();
        if (!mainHand && slot != 45)
            return;

        Map<Player, SimpleItemData> data = mainHand ? this.mainHand : this.offHand;

        ItemStack packetItem = SpigotConversionUtil.toBukkitItemStack(wrapper.getItem());
        if (!packetItem.hasItemMeta() || !CustomTag.WEAPON_TITLE.hasString(packetItem)) {
            data.put(player, null);
            return;
        }

        SimpleItemData lastData = data.get(player);
        if (lastData == null) {
            data.put(player, new SimpleItemData(slot, packetItem));
            return;
        }

        SimpleItemData newData = new SimpleItemData(slot, packetItem);
        if (newData.isDifferent(lastData)) {
            data.put(player, newData);
            return;
        }

        event.setCancelled(true);
    }

    public static class SimpleItemData {

        private final int sentToSlot;
        private final Material type;
        private final int amount;
        private String displayName;
        private List<String> lore;
        private int customModelData;
        private int damage;

        public SimpleItemData(int sentToSlot, ItemStack itemStack) {
            this.sentToSlot = sentToSlot;
            this.type = itemStack.getType();
            this.amount = itemStack.getAmount();
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta == null)
                return;

            if (itemMeta.hasDisplayName())
                this.displayName = itemMeta.getDisplayName();
            if (itemMeta.hasLore())
                this.lore = itemMeta.getLore();
            if (itemMeta.hasCustomModelData())
                this.customModelData = itemMeta.getCustomModelData();
            if (itemMeta instanceof Damageable damageable)
                this.damage = damageable.getDamage();
        }

        public boolean isDifferent(SimpleItemData other) {
            return sentToSlot != other.sentToSlot || type != other.type || amount != other.amount || customModelData != other.customModelData
                    || damage != other.damage || !Objects.equals(displayName, other.displayName)
                    || !Objects.equals(lore, other.lore);
        }
    }
}