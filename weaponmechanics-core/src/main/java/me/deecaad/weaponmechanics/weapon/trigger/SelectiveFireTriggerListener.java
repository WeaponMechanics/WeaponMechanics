package me.deecaad.weaponmechanics.weapon.trigger;

import me.deecaad.core.file.Configuration;
import me.deecaad.core.mechanics.CastData;
import me.deecaad.core.mechanics.MechanicManager;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.utils.CustomTag;
import me.deecaad.weaponmechanics.weapon.info.WeaponInfoDisplay;
import me.deecaad.weaponmechanics.weapon.shoot.SelectiveFireState;
import me.deecaad.weaponmechanics.weapon.skin.SkinHandler;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import me.deecaad.weaponmechanics.wrappers.PlayerWrapper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import static me.deecaad.weaponmechanics.weapon.shoot.SelectiveFireState.AUTO;
import static me.deecaad.weaponmechanics.weapon.shoot.SelectiveFireState.BURST;

public class SelectiveFireTriggerListener implements TriggerListener {

    @Override
    public boolean allowOtherTriggers() {
        return false;
    }

    @Override
    public boolean tryUse(EntityWrapper entityWrapper, String weaponTitle, ItemStack weaponStack, EquipmentSlot slot, TriggerType triggerType, boolean dualWield, @Nullable LivingEntity victim) {
        Configuration config = WeaponMechanics.getInstance().getWeaponConfigurations();
        Trigger selectiveFireTrigger = config.getObject(weaponTitle + ".Shoot.Selective_Fire.Trigger", Trigger.class);
        if (selectiveFireTrigger == null || !selectiveFireTrigger.check(triggerType, slot, entityWrapper)) {
            return false;
        }

        boolean hasBurst = config.getInt(weaponTitle + ".Shoot.Burst.Shots_Per_Burst") != 0 && config.getInt(weaponTitle + ".Shoot.Burst.Ticks_Between_Each_Shot") != 0;
        boolean hasAuto = config.getInt(weaponTitle + ".Shoot.Fully_Automatic_Shots_Per_Second") != 0;

        int selectiveFireStateId = CustomTag.SELECTIVE_FIRE.getInteger(weaponStack);
        SelectiveFireState selectiveFireState = SelectiveFireState.getState(selectiveFireStateId);

        // Order is
        // 1) Single
        // 2) Burst
        // 3) Auto
        SelectiveFireState nextState = selectiveFireState.getNext();

        if ((!hasBurst && nextState == BURST) || (!hasAuto && nextState == AUTO)) {
            nextState = nextState.getNext();
        }

        SelectiveFireState.setState(entityWrapper, weaponTitle, weaponStack, slot, selectiveFireState, nextState);

        entityWrapper.getMainHandData().cancelTasks();
        entityWrapper.getOffHandData().cancelTasks();

        MechanicManager selectiveFireMechanics = config.getObject(weaponTitle + ".Shoot.Selective_Fire.Mechanics", MechanicManager.class);
        if (selectiveFireMechanics != null)
            selectiveFireMechanics.use(new CastData(entityWrapper.getEntity(), weaponTitle, weaponStack));

        WeaponInfoDisplay weaponInfoDisplay = config.getObject(weaponTitle + ".Info.Weapon_Info_Display", WeaponInfoDisplay.class);
        if (weaponInfoDisplay != null)
            weaponInfoDisplay.send((PlayerWrapper) entityWrapper, slot);

        SkinHandler skinHandler = WeaponMechanics.getInstance().getWeaponHandler().getSkinHandler();
        skinHandler.tryUse(triggerType, entityWrapper, weaponTitle, weaponStack, slot);

        return true;
    }
}
