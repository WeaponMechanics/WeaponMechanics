package me.deecaad.weaponmechanics.weapon.trigger;

import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.Nullable;

public interface TriggerListener {

    /**
     * Mostly this is defaulted to false.
     *
     * @return true if other triggers can occur even after this trigger is valid
     */
    boolean allowOtherTriggers();

    /**
     * Tries to use given trigger type to trigger some action.
     *
     * @param entityWrapper the entity which caused trigger
     * @param weaponTitle the weapon title involved
     * @param weaponStack the weapon stack involved
     * @param slot the weapon slot used
     * @param triggerType the trigger which caused this
     * @param dualWield whether this was dual wield
     * @param victim if there is known victim
     * @return true if was able to use
     */
    boolean tryUse(EntityWrapper entityWrapper, String weaponTitle, ItemStack weaponStack, EquipmentSlot slot, TriggerType triggerType, boolean dualWield, @Nullable LivingEntity victim);
}