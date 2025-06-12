package me.deecaad.weaponmechanics.lib;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.utils.CustomTag;
import me.deecaad.weaponmechanics.weapon.info.InfoHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class MythicMobsArmedCondition implements IEntityCondition {

    private final boolean wildcard;
    private final Set<String> weapons;

    public MythicMobsArmedCondition(MythicLineConfig config) {
        this.weapons = new HashSet<>();
        String weapons = config.getString(new String[]{"weapons", "weapon", "title", "weaponTitle", "w"}, "*");

        this.wildcard = "*".equals(weapons.trim());
        if (wildcard)
            return;

        InfoHandler info = WeaponMechanics.getInstance().getWeaponHandler().getInfoHandler();
        for (String title : weapons.split(", ?")) {
            this.weapons.add(info.getWeaponTitle(title));
        }
    }

    @Override
    public boolean check(AbstractEntity abstractEntity) {
        LivingEntity entity = (LivingEntity) abstractEntity.getBukkitEntity();

        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null)
            return false;

        ItemStack main = equipment.getItemInMainHand();
        ItemStack off = equipment.getItemInOffHand();

        String mainTitle = main.hasItemMeta() ? CustomTag.WEAPON_TITLE.getString(main) : null;
        String offTitle = off.hasItemMeta() ? CustomTag.WEAPON_TITLE.getString(off) : null;

        if (wildcard)
            return mainTitle != null || offTitle != null;
        else
            return (mainTitle != null && weapons.contains(mainTitle)) || (offTitle != null && weapons.contains(offTitle));
    }
}
