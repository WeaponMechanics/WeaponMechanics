package me.deecaad.weaponmechanics.lib;

import io.lumine.mythic.api.adapters.AbstractItemStack;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.drops.DropMetadata;
import io.lumine.mythic.api.drops.IItemDrop;
import io.lumine.mythic.bukkit.adapters.item.ItemComponentBukkitItemStack;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.info.InfoHandler;
import org.bukkit.inventory.ItemStack;

public class MythicMobsWeaponDrop implements IItemDrop {

    private final String weaponTitle;
    private final int amount;

    public MythicMobsWeaponDrop(MythicLineConfig config, String argument) {
        String weaponTitle = config.getString(new String[]{"weapon", "title", "weaponTitle", "w"}, "", "");
        this.amount = config.getInteger(new String[]{"amount", "a"}, 1);

        InfoHandler info = WeaponMechanics.getInstance().getWeaponHandler().getInfoHandler();
        try {
            weaponTitle = info.getWeaponTitle(weaponTitle);
        } catch (IllegalArgumentException ignore) {
        }
        this.weaponTitle = weaponTitle;

        if (this.amount < 1) {
            WeaponMechanics.getInstance().debugger.severe("MythicMobs expected positive integer, found: " + amount,
                "Located in file '" + config.getFileName() + "' at '" + config.getKey() + "'",
                "Found argument: " + argument);
        }
    }

    @Override
    public AbstractItemStack getDrop(DropMetadata dropMetadata, double v) {
        InfoHandler info = WeaponMechanics.getInstance().getWeaponHandler().getInfoHandler();
        ItemStack item = info.generateWeapon(weaponTitle, amount);

        if (item == null) {
            WeaponMechanics.getInstance().debugger.warning("MythicMobs tried to generate weapon which doesn't exist: '" + weaponTitle + "'");
            return null;
        }

        return new ItemComponentBukkitItemStack(item);
    }
}
