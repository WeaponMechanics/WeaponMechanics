package me.deecaad.weaponmechanics.compatibility;

import com.cjcrafter.foliascheduler.util.MinecraftVersions;
import me.deecaad.core.compatibility.CompatibilitySetup;
import me.deecaad.weaponmechanics.WeaponMechanics;
import org.bukkit.Bukkit;

public class WeaponCompatibilityAPI {

    private static IWeaponCompatibility weaponCompatibility;

    public static IWeaponCompatibility getWeaponCompatibility() {
        if (weaponCompatibility == null) {
            weaponCompatibility = new CompatibilitySetup().getCompatibleVersion(IWeaponCompatibility.class, "me.deecaad.weaponmechanics.compatibility");
            if (weaponCompatibility == null) {
                WeaponMechanics.getInstance().getDebugger().severe("Unsupported server version: " + MinecraftVersions.getCurrent() + " (" + Bukkit.getBukkitVersion() + ")",
                        "The protocol version " + MinecraftVersions.getCurrent().toProtocolString() + " has no compatibility class...",
                        "If you are using a new version of Minecraft, you might need to update your plugins!",
                        "!!! CRITICAL ERROR !!!");
            }
        }
        return weaponCompatibility;
    }
}