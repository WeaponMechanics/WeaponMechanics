package me.deecaad.weaponmechanics.weapon.placeholders;

import me.deecaad.core.file.Configuration;
import me.deecaad.core.placeholder.PlaceholderData;
import me.deecaad.core.placeholder.PlaceholderHandler;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.firearm.FirearmAction;
import me.deecaad.weaponmechanics.weapon.firearm.FirearmState;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PFirearmState extends PlaceholderHandler {

    public PFirearmState() {
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(WeaponMechanics.getInstance(), "firearm_state");
    }

    @Override
    public @Nullable String onRequest(@NotNull PlaceholderData data) {
        if (data.item() == null || data.itemTitle() == null)
            return null;

        FirearmAction firearmAction = WeaponMechanics.getInstance().getWeaponConfigurations().getObject(data.itemTitle() + ".Firearm_Action", FirearmAction.class);

        // Simply don't show anything
        if (firearmAction == null)
            return "";

        FirearmState state = firearmAction.getState(data.item());
        Configuration config = WeaponMechanics.getInstance().getConfiguration();
        return switch (state) {
            case OPEN -> config.getString("Placeholder_Symbols." + firearmAction.getFirearmType().name() + ".Open", " □");
            case CLOSE -> config.getString("Placeholder_Symbols." + firearmAction.getFirearmType().name() + ".Close", " ■");
            default -> "";
        };
    }
}