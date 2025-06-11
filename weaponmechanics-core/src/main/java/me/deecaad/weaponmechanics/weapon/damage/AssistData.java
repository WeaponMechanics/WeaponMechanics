package me.deecaad.weaponmechanics.weapon.damage;

import me.deecaad.core.file.Configuration;
import me.deecaad.core.file.IValidator;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.utils.NumberUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AssistData implements IValidator {

    private final Map<UUID, Map<String, DamageInfo>> players;

    public AssistData() {
        this.players = new HashMap<>();
    }

    public void logDamage(Player shooter, String weaponTitle, ItemStack weaponStack, double amount) {
        UUID uuid = shooter.getUniqueId();
        players.putIfAbsent(uuid, new HashMap<>());
        players.get(uuid).compute(weaponTitle, (key, value) -> value == null ? new DamageInfo(amount, weaponStack) : value.add(amount, weaponStack));
    }

    @Nullable public Map<Player, Map<String, DamageInfo>> getAssists(Player killer) {
        if (players.isEmpty())
            return null;

        Map<Player, Map<String, DamageInfo>> assists = new HashMap<>();

        Configuration config = WeaponMechanics.getInstance().getConfiguration();
        double requiredDamageAmount = config.getDouble("Assists_Event.Required_Damage_Amount", 0);
        int timer = config.getInt("Assists_Event.Timer", 0);

        players.forEach((uuid, value) -> {
            if (killer != null && uuid.equals(killer.getUniqueId()))
                return;

            Player playerByUuid = Bukkit.getPlayer(uuid);
            // Might be null if damager has quit
            if (playerByUuid == null)
                return;

            assists.putIfAbsent(playerByUuid, new HashMap<>());

            double totalDamage = 0;
            long lastHitTime = 0;
            for (Map.Entry<String, DamageInfo> entry : value.entrySet()) {

                DamageInfo entryValue = entry.getValue();
                totalDamage += entryValue.damage;
                if (lastHitTime == 0)
                    lastHitTime = entryValue.lastHitTime;
                lastHitTime = Math.max(lastHitTime, entryValue.lastHitTime);

                assists.get(playerByUuid).put(entry.getKey(), entryValue);
            }

            if ((requiredDamageAmount != 0 && totalDamage < requiredDamageAmount)
                || (timer != 0 && NumberUtil.hasMillisPassed(lastHitTime, timer))) {

                // Remove since it wasn't valid
                assists.remove(playerByUuid);
            }
        });

        return assists.isEmpty() ? null : assists;
    }

    public static class DamageInfo {

        private long lastHitTime;
        private double damage;
        private ItemStack weaponStack;

        public DamageInfo(double damage, ItemStack weaponStack) {
            this.lastHitTime = System.currentTimeMillis();
            this.damage = damage;
            this.weaponStack = weaponStack;
        }

        private DamageInfo add(double damage, ItemStack weaponStack) {
            this.lastHitTime = System.currentTimeMillis();
            this.damage += damage;
            this.weaponStack = weaponStack;
            return this;
        }

        /**
         * @return the total damage done using this weapon
         */
        public double getDamage() {
            return damage;
        }

        /**
         * Using this to e.g. set meta is unreliable since the reference might be old at this point.
         *
         * @return the weapon stack used to damage.
         */
        public ItemStack getWeaponStack() {
            return weaponStack;
        }

        /**
         * @return the last hit time in ticks
         */
        public int getLastHitTime() {
            return (int) ((System.currentTimeMillis() - lastHitTime) / 50);
        }

        @Override
        public String toString() {
            return "DamageInfo{" +
                "lastHitTime=" + lastHitTime +
                ", damage=" + damage +
                ", weaponStack=" + weaponStack.getType() +
                '}';
        }
    }

    @Override
    public String toString() {
        return "AssistData{" +
            "players=" + players +
            '}';
    }

    @Override
    public String getKeyword() {
        return "Assists_Event";
    }

    @Override
    public void validate(Configuration configuration, SerializeData data) throws SerializerException {
        int timer = data.of("Timer").assertRange(1, null).getInt().orElse(100);
        if (timer != 0) {
            // Convert to millis
            configuration.set(data.getKey() + ".Timer", timer * 50);
        }

        double damageAmount = data.of("Required_Damage_Amount").assertRange(0.0, null).getDouble().orElse(0);

        if (data.of("Enable").getBool().orElse(true) && timer == 0 && damageAmount == 0) {
            throw data.exception("", "When using assists, make sure to either use Timer or Required_Damage_Amount");
        }
    }
}