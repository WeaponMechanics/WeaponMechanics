package me.deecaad.weaponmechanics.weapon.trigger;

import me.deecaad.core.file.Configuration;
import me.deecaad.core.mechanics.CastData;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.utils.CustomTag;
import me.deecaad.weaponmechanics.weapon.info.WeaponInfoDisplay;
import me.deecaad.weaponmechanics.weapon.reload.ammo.AmmoConfig;
import me.deecaad.weaponmechanics.weapon.skin.SkinHandler;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import me.deecaad.weaponmechanics.wrappers.PlayerWrapper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class AmmoTypeSwitchTriggerListener implements TriggerListener {

    @Override
    public boolean allowOtherTriggers() {
        return false;
    }

    @Override
    public boolean tryUse(EntityWrapper entityWrapper, String weaponTitle, ItemStack weaponStack, EquipmentSlot slot, TriggerType triggerType, boolean dualWield, @Nullable LivingEntity victim) {
        Configuration config = WeaponMechanics.getInstance().getWeaponConfigurations();
        AmmoConfig ammo = config.getObject(weaponTitle + ".Reload.Ammo", AmmoConfig.class);
        if (ammo == null)
            return false;

        Trigger ammoTypeSwitchTrigger = ammo.getSwitchTrigger();
        if (ammoTypeSwitchTrigger == null || entityWrapper.getEntity().getType() != EntityType.PLAYER
            || !ammoTypeSwitchTrigger.check(triggerType, slot, entityWrapper)) {
            return false;
        }

        // First empty the current ammo
        int ammoLeft = CustomTag.AMMO_LEFT.getInteger(weaponStack);
        if (ammoLeft > 0) {
            ammo.giveAmmo(weaponStack, (PlayerWrapper) entityWrapper, ammoLeft, config.getInt(weaponTitle + ".Reload.Magazine_Size"));
            CustomTag.AMMO_LEFT.setInteger(weaponStack, 0);
        }

        // Then do the switch
        ammo.updateToNextAmmo(weaponStack);

        entityWrapper.getMainHandData().cancelTasks();
        entityWrapper.getOffHandData().cancelTasks();

        if (ammo.getSwitchMechanics() != null)
            ammo.getSwitchMechanics().use(new CastData(entityWrapper.getEntity(), weaponTitle, weaponStack));

        WeaponInfoDisplay weaponInfoDisplay = WeaponMechanics.getInstance().getWeaponConfigurations().getObject(weaponTitle + ".Info.Weapon_Info_Display", WeaponInfoDisplay.class);
        if (weaponInfoDisplay != null)
            weaponInfoDisplay.send((PlayerWrapper) entityWrapper, slot);

        SkinHandler skinHandler = WeaponMechanics.getInstance().getWeaponHandler().getSkinHandler();
        skinHandler.tryUse(triggerType, entityWrapper, weaponTitle, weaponStack, slot);

        return true;
    }
}
