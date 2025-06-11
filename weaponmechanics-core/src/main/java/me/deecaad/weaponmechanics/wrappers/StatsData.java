package me.deecaad.weaponmechanics.wrappers;

import me.deecaad.weaponmechanics.weapon.stats.PlayerStat;
import me.deecaad.weaponmechanics.weapon.stats.WeaponStat;

import java.util.*;
import java.util.function.BiFunction;

import static org.bukkit.ChatColor.*;

public class StatsData {

    private final UUID uuid;
    private Map<PlayerStat, Object> playerData;
    private Map<String, Map<WeaponStat, Object>> weaponData;
    private boolean isSync;

    public StatsData(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * @return whether these stats have been synced with database
     */
    public boolean isSync() {
        return isSync;
    }

    /**
     * Adds given data to player stat. Doesn't do anything if this hasn't yet been synced with database.
     *
     * @param stat the stat
     * @param data the amount to add
     */
    public void add(PlayerStat stat, int data) {
        if (!isSync)
            return;
        if (!stat.getClassType().isInstance(data))
            throw new IllegalArgumentException("Tried to give invalid data for stat " + stat + " " + data);
        playerData.compute(stat, (key, value) -> value == null ? data : (int) value + data);
    }

    /**
     * Adds given data to player stat. Doesn't do anything if this hasn't yet been synced with database.
     *
     * @param stat the stat
     * @param data the amount to add
     */
    public void add(PlayerStat stat, float data) {
        if (!isSync)
            return;
        if (!stat.getClassType().isInstance(data))
            throw new IllegalArgumentException("Tried to give invalid data for stat " + stat + " " + data);
        playerData.compute(stat, (key, value) -> value == null ? data : (float) value + data);
    }

    /**
     * Set given data to player stat. Doesn't do anything if this hasn't yet been synced with database.
     *
     * @param stat the stat
     * @param data the data to set
     */
    public void set(PlayerStat stat, String data) {
        if (!isSync)
            return;
        if (!stat.getClassType().isInstance(data))
            throw new IllegalArgumentException("Tried to give invalid data for stat " + stat + " " + data);
        playerData.put(stat, data);
    }

    /**
     * Used to get the synced data. When data isn't yet synced, default value is always returned.
     * Assigns default value to be 0 or null depending on stat type
     *
     * @param stat the player stat
     * @return the data object
     */
    public Object get(PlayerStat stat) {
        return get(stat, stat.getDefaultValue());
    }

    /**
     * Used to get the synced data. When data isn't yet synced, default value is always returned.
     *
     * @param stat the player stat
     * @param defaultValue the default value for player stat
     * @return the data object
     */
    public Object get(PlayerStat stat, Object defaultValue) {
        if (!isSync)
            return defaultValue;
        if (stat == PlayerStat.UUID)
            return uuid;
        return playerData.getOrDefault(stat, defaultValue);
    }

    /**
     * Adds given data to weapon's weapon stat. Doesn't do anything if this hasn't yet been synced with
     * database.
     *
     * @param weaponTitle the weapon title
     * @param stat the stat
     * @param data the amount to add
     */
    public void add(String weaponTitle, WeaponStat stat, int data) {
        if (!isSync)
            return;
        if (!stat.getClassType().isInstance(data))
            throw new IllegalArgumentException("Tried to give invalid data for stat " + stat + " " + data);
        weaponData.putIfAbsent(weaponTitle, new HashMap<>());
        weaponData.get(weaponTitle).compute(stat, (key, value) -> value == null ? data : (int) value + data);
    }

    /**
     * Adds given data to weapon's weapon stat. Doesn't do anything if this hasn't yet been synced with
     * database.
     *
     * @param weaponTitle the weapon title
     * @param stat the stat
     * @param data the amount to add
     */
    public void add(String weaponTitle, WeaponStat stat, float data) {
        if (!isSync)
            return;
        if (!stat.getClassType().isInstance(data))
            throw new IllegalArgumentException("Tried to give invalid data for stat " + stat + " " + data);
        weaponData.putIfAbsent(weaponTitle, new HashMap<>());
        weaponData.get(weaponTitle).compute(stat, (key, value) -> value == null ? data : (float) value + data);
    }

    /**
     * Set given data to weapon's weapon stat using BiFunction. Doesn't do anything if this hasn't yet
     * been synced with database. Useful when you want to update stat's value conditionally (e.g.
     * longest distance hit)
     *
     * @param weaponTitle the weapon title
     * @param stat the stat
     * @param compute the BiFunction
     */
    public void set(String weaponTitle, WeaponStat stat, BiFunction<WeaponStat, Object, Float> compute) {
        if (!isSync)
            return;
        weaponData.putIfAbsent(weaponTitle, new HashMap<>());
        weaponData.get(weaponTitle).compute(stat, compute);
    }

    /**
     * Set given data to weapon's weapon stat. Doesn't do anything if this hasn't yet been synced with
     * database.
     *
     * @param weaponTitle the weapon title
     * @param stat the stat
     * @param data the data to set
     */
    public void set(String weaponTitle, WeaponStat stat, String data) {
        if (!isSync)
            return;
        if (!stat.getClassType().isInstance(data))
            throw new IllegalArgumentException("Tried to give invalid data for stat " + stat + " " + data);
        weaponData.putIfAbsent(weaponTitle, new HashMap<>());
        weaponData.get(weaponTitle).put(stat, data);
    }

    /**
     * Adds given data to weapon's weapon stat set. Doesn't do anything if this hasn't yet been synced
     * with database.
     *
     * @param weaponTitle the weapon title
     * @param stat the stat
     * @param data the data to add
     */
    public void addToSet(String weaponTitle, WeaponStat stat, String data) {
        if (!isSync)
            return;
        if (stat.getClassType() != Set.class)
            throw new IllegalArgumentException("Tried to add to set when stat wasn't set " + stat + " " + data);
        weaponData.putIfAbsent(weaponTitle, new HashMap<>());
        Map<WeaponStat, Object> dataMap = weaponData.get(weaponTitle);
        Object mapData = dataMap.get(stat);
        if (mapData == null) {
            dataMap.put(stat, new HashSet<>(Collections.singleton(data)));
            return;
        }
        ((Set<String>) mapData).add(data);
    }

    /**
     * Removes given data from weapon's weapon stat set. Doesn't do anything if this hasn't yet been
     * synced with database.
     *
     * @param weaponTitle the weapon title
     * @param stat the stat
     * @param data the data to remove
     */
    public void removeFromSet(String weaponTitle, WeaponStat stat, String data) {
        if (!isSync)
            return;
        if (stat.getClassType() != Set.class)
            throw new IllegalArgumentException("Tried to remove from set when stat wasn't set " + stat + " " + data);
        Map<WeaponStat, Object> dataMap = weaponData.get(weaponTitle);
        if (dataMap == null || !dataMap.containsKey(stat))
            return;
        Set<String> dataSet = (Set<String>) dataMap.get(stat);
        dataSet.remove(data);
    }

    /**
     * @return the weapon titles which have some data saved, null if not synced yet
     */
    public Set<String> getWeapons() {
        if (!isSync)
            return null;
        return weaponData.keySet();
    }

    /**
     * Used to get the synced data. When data isn't yet synced, default value is always returned.
     * Assigns default value to be 0 or null depending on stat type
     *
     * @param weaponTitle the weapon title
     * @param stat the weapon stat
     * @return the data object
     */
    public Object get(String weaponTitle, WeaponStat stat) {
        return get(weaponTitle, stat, stat.getDefaultValue());
    }

    /**
     * Used to get the synced data. When data isn't yet synced, default value is always returned.
     *
     * @param weaponTitle the weapon title
     * @param stat the weapon stat
     * @param defaultValue the default value for weapon stat
     * @return the data object
     */
    public Object get(String weaponTitle, WeaponStat stat, Object defaultValue) {
        if (!isSync)
            return defaultValue;
        if (stat == WeaponStat.UUID)
            return uuid;
        if (stat == WeaponStat.WEAPON_TITLE)
            return weaponTitle;

        Map<WeaponStat, Object> data = weaponData.get(weaponTitle);
        if (data == null)
            return defaultValue;

        return data.getOrDefault(stat, defaultValue);
    }

    /**
     * This method can only be used once during StatsData object lifetime. Used to insert the database
     * data via hashmaps.
     *
     * @param playerData the player data
     * @param weaponData the weapon data
     */
    public void setData(Map<PlayerStat, Object> playerData, Map<String, Map<WeaponStat, Object>> weaponData) {
        if (isSync)
            throw new IllegalArgumentException("Tried to set data after sync");
        if (playerData == null)
            throw new IllegalArgumentException("Tried to set null value for player data");
        if (weaponData == null)
            throw new IllegalArgumentException("Tried to set null value for weapon data");

        this.playerData = playerData;
        this.weaponData = weaponData;
        isSync = true;
    }

    public List<String> getPlayerData() {
        if (!isSync)
            return null;
        if (playerData.isEmpty())
            return null;
        List<String> data = new ArrayList<>();
        for (Map.Entry<PlayerStat, Object> entry : playerData.entrySet()) {
            data.add("" + GOLD + entry.getKey() + ": " + GRAY + entry.getValue());
        }
        return data;
    }

    public List<String> getWeaponData(String weapon) {
        if (!isSync)
            return null;
        Map<WeaponStat, Object> data = weaponData.get(weapon);
        if (data == null || data.isEmpty())
            return null;
        List<String> dataList = new ArrayList<>();
        for (Map.Entry<WeaponStat, Object> entry : data.entrySet()) {
            dataList.add("" + GOLD + entry.getKey() + ": " + GRAY + entry.getValue());
        }
        return dataList;
    }

    @Override
    public String toString() {
        return "StatsData{" +
            "playerData=" + playerData +
            ", weaponData=" + weaponData +
            '}';
    }
}