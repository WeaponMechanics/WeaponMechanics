package me.deecaad.weaponmechanics.lib;

import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent;
import io.lumine.mythic.bukkit.events.MythicDropLoadEvent;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import me.deecaad.weaponmechanics.WeaponMechanics;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MythicMobsLoader implements Listener {

    public MythicMobsLoader() {
        WeaponMechanics.getInstance().debugger.info("Hooking into MythicMobs");
    }

    @EventHandler
    public void onLoad(MythicMechanicLoadEvent event) {
        if (event.getMechanicName().equalsIgnoreCase("weaponMechanicsShoot")) {
            event.register(new MythicMobsWeaponShootSkill(event.getConfig()));
        }
    }

    @EventHandler
    public void onMythicConditionLoad(MythicConditionLoadEvent event) {
        if (event.getConditionName().equalsIgnoreCase("weaponMechanicsArmed")) {
            event.register(new MythicMobsArmedCondition(event.getConfig()));
        } else if (event.getConditionName().equalsIgnoreCase("weaponMechanicsReloading")) {
            event.register(new MythicMobsReloadingCondition(event.getConfig()));
        }
    }

    @EventHandler
    public void onMythicDropLoad(MythicDropLoadEvent event) {
        if (event.getDropName().equalsIgnoreCase("weaponMechanicsWeapon")) {
            event.register(new MythicMobsWeaponDrop(event.getConfig(), event.getArgument()));
        }
    }
}
