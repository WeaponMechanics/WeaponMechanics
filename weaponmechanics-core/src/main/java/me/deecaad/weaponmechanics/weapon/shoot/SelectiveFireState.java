package me.deecaad.weaponmechanics.weapon.shoot;

import me.deecaad.weaponmechanics.utils.CustomTag;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponSelectiveFireChangeEvent;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import org.bukkit.Bukkit;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public enum SelectiveFireState {

    SINGLE,
    BURST,
    AUTO;

    private static final SelectiveFireState[] VALUES = values();

    SelectiveFireState() {
    }

    /**
     * Helper function to set item's selective fire state and call a
     * {@link me.deecaad.weaponmechanics.weapon.weaponevents.WeaponSelectiveFireChangeEvent}.
     */
    public static void setState(EntityWrapper entity, String weaponTitle, ItemStack item, EquipmentSlot slot, SelectiveFireState oldState, SelectiveFireState newState) {
        WeaponSelectiveFireChangeEvent event = new WeaponSelectiveFireChangeEvent(weaponTitle, item, entity.getEntity(), slot, oldState, newState);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled())
            return;

        CustomTag.SELECTIVE_FIRE.setInteger(item, event.getNewState().ordinal());
    }

    public static SelectiveFireState getState(int ordinal) {
        return VALUES[ordinal];
    }

    public static int count() {
        return VALUES.length;
    }

    public SelectiveFireState getNext() {
        int nextId = this.ordinal() + 1;
        return nextId >= count()
            ? getState(0)
            : getState(nextId);
    }
}