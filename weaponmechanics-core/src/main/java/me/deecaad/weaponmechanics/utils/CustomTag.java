package me.deecaad.weaponmechanics.utils;

import me.deecaad.core.compatibility.CompatibilityAPI;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/**
 * This enum is used to keep a list of all NBT tags used by WeaponMechanics. This enum also wraps
 * the {@link me.deecaad.core.compatibility.nbt.NBTCompatibility} api, making it easier to access
 * data from items.
 */
public enum CustomTag {

    /**
     * The weapon title is stored as a String, and is used by WeaponMechanics to determine if an item is
     * a weapon, and which weapon it is. The title can be used to retrieve values from config.
     */
    WEAPON_TITLE,

    /**
     * Selective fire is stored as an int, and is used by WeaponMechanics to determine the current
     * selective fire rate of the weapon. 0 = single, 1 = burst, and 2 = auto.
     */
    SELECTIVE_FIRE,

    /**
     * Ammo left is stored as an int, and is used by WeaponMechanics to store the amount of ammo left in
     * the gun.
     */
    AMMO_LEFT,

    /**
     * Ammo type index is stored as an int, and is used by WeaponMechanics to determine which ammo type
     * that is currently loaded in the weapon. This is done since config allows for multiple ammo types
     * to be loaded into the weapons. This is used by WeaponMechanicsPlus to add modifiers to different
     * ammo types.
     */
    AMMO_TYPE_INDEX,

    /**
     * The ammo name is stored as a String, and is used by WeaponMechanics to determine if an item can
     * be used as ammunition.
     */
    AMMO_TITLE("ammo-name"),

    /**
     * Ammo magazine is stored as an int, and is used by WeaponMechanics to determine if the ammo was
     * loaded as a magazine, or as individual bullets. 0 = bullets, 1 = magazine.
     */
    AMMO_MAGAZINE,

    /**
     * Firearm action state is stored as an int, and is used by WeaponMechanics to check if the weapon
     * is open/closed. 0 = Ready, 1 = Open, 2 = Closed.
     *
     * @see me.deecaad.weaponmechanics.weapon.firearm.FirearmAction#getState(ItemStack)
     */
    FIREARM_ACTION_STATE,

    /**
     * Attachment title is stored as a string, and is used by WeaponMechanicsPlus to determine if an
     * item is an attachment. This title can be used to get attachment information from config (or the
     * attachment registry, stored in WeaponMechanicsPlus).
     */
    ATTACHMENT_TITLE,

    /**
     * Attachments are stored as a list of strings, and is used by WeaponMechanicsPlus to determine
     * which attachments are attached to a weapon. Each string in the list is an
     * {@link #ATTACHMENT_TITLE}, and can be used to pull information from config.
     */
    ATTACHMENTS,

    /**
     * Weapon skin is stored as a string, and is used by WeaponMechanicsCosmetics to determine if a
     * weapon has a set skin. This overrides the player's preferred skin.
     */
    WEAPON_SKIN,

    /**
     * Armor title is stored as a string, and is used by ArmorMechanics to determine if an item is a
     * custom armor added by the plugin.
     */
    ARMOR_TITLE(null, "armormechanics"),

    /**
     * Prevent remove is stored as an int, and is used by ArmorMechanics as a marker to prevent armor
     * from being unequipped.
     */
    PREVENT_REMOVE(null, "armormechanics");

    private final String owningPlugin;
    private final String id;

    CustomTag() {
        this.id = name().toLowerCase(Locale.ROOT).replace('_', '-');
        this.owningPlugin = "weaponmechanics";
    }

    /**
     * This is only used for backwards support. For example, AMMO_NAME -> {@link #AMMO_TITLE}. Use the
     * default constructor.
     *
     * @param id The non-null id to be used as the NBT tag.
     */
    CustomTag(String id) {
        this.id = id;
        this.owningPlugin = "weaponmechanics";
    }

    CustomTag(String id, String owningPlugin) {
        this.id = id != null ? id : name().toLowerCase(Locale.ROOT).replace('_', '-');
        this.owningPlugin = owningPlugin;
    }

    /**
     * @return the id that should be put to item stack to be used that identifier
     */
    public String getId() {
        return this.id;
    }

    public String getKey() {
        return owningPlugin + ":" + getId();
    }

    public boolean hasString(ItemStack item) {
        return CompatibilityAPI.getNBTCompatibility().hasString(item, owningPlugin, id);
    }

    public String getString(ItemStack item) {
        return CompatibilityAPI.getNBTCompatibility().getString(item, owningPlugin, id);
    }

    public void setString(ItemStack item, String value) {
        CompatibilityAPI.getNBTCompatibility().setString(item, owningPlugin, id, value);
    }

    public boolean hasInteger(ItemStack item) {
        return CompatibilityAPI.getNBTCompatibility().hasInt(item, owningPlugin, id);
    }

    public int getInteger(ItemStack item) {
        return CompatibilityAPI.getNBTCompatibility().getInt(item, owningPlugin, id);
    }

    public void setInteger(ItemStack item, int value) {
        CompatibilityAPI.getNBTCompatibility().setInt(item, owningPlugin, id, value);
    }

    public boolean hasDouble(ItemStack item) {
        return CompatibilityAPI.getNBTCompatibility().hasDouble(item, owningPlugin, id);
    }

    public double getDouble(ItemStack item) {
        return CompatibilityAPI.getNBTCompatibility().getDouble(item, owningPlugin, id);
    }

    public void setDouble(ItemStack item, double value) {
        CompatibilityAPI.getNBTCompatibility().setDouble(item, owningPlugin, id, value);
    }

    public boolean hasArray(ItemStack item) {
        return CompatibilityAPI.getNBTCompatibility().hasArray(item, owningPlugin, id);
    }

    public int[] getArray(ItemStack item) {
        return CompatibilityAPI.getNBTCompatibility().getArray(item, owningPlugin, id);
    }

    public void setArray(ItemStack item, int[] value) {
        CompatibilityAPI.getNBTCompatibility().setArray(item, owningPlugin, id, value);
    }

    public boolean hasStringArray(ItemStack item) {
        return CompatibilityAPI.getNBTCompatibility().hasStringArray(item, owningPlugin, id);
    }

    public String[] getStringArray(ItemStack item) {
        return CompatibilityAPI.getNBTCompatibility().getStringArray(item, owningPlugin, id);
    }

    public void setStringArray(ItemStack item, String[] value) {
        CompatibilityAPI.getNBTCompatibility().setStringArray(item, owningPlugin, id, value);
    }

    public void remove(ItemStack item) {
        CompatibilityAPI.getNBTCompatibility().remove(item, owningPlugin, id);
    }
}