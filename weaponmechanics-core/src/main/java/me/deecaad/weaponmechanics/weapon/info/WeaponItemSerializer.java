package me.deecaad.weaponmechanics.weapon.info;

import me.deecaad.core.file.Configuration;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.serializers.ItemSerializer;
import me.deecaad.core.utils.StringUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.utils.CustomTag;
import me.deecaad.weaponmechanics.weapon.shoot.SelectiveFireState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Simple class to handle weapon item serialization a bit differently.
 */
public class WeaponItemSerializer extends ItemSerializer {

    /**
     * Default constructor for serializer
     */
    public WeaponItemSerializer() {
    }

    @Override
    public String getKeyword() {
        return "Weapon_Item";
    }

    @Override
    @NotNull public ItemStack serialize(@NotNull SerializeData data) throws SerializerException {
        ItemStack weaponStack = super.serializeWithoutRecipe(data);

        // Crossbows are not allowed to be used as the weapon's type. WMC
        // supports "faking" the crossbow for other players. I added this check
        // since it is a commonly asked question, and this should save me some
        // time in the long run.
        if (weaponStack.getType().name().equals("CROSSBOW")) {
            throw data.exception("Type", "You cannot use 'CROSSBOW' as a WeaponMechanics weapon!",
                "YES! We know that you want weapons to be 'held up' like a minecraft crossbow",
                "Purchase WMC to 'fake' the crossbow animation for other players: https://www.spigotmc.org/resources/104539/");
        }

        String weaponTitle = data.getKey().split("\\.")[0];

        // Saving display name and lore for use in WeaponMechanicsPlus to auto update items
        Configuration config = WeaponMechanics.getInstance().getWeaponConfigurations();
        String weaponDisplay = data.of("Name").getAdventure().orElse(null);
        if (weaponDisplay != null) {
            config.set(weaponTitle + ".Info.Weapon_Item.Display", weaponDisplay);
        }

        List<?> weaponLore = data.of("Lore").get(List.class).orElse(List.of());
        if (!weaponLore.isEmpty()) {
            config.set(weaponTitle + ".Info.Weapon_Item.Lore", weaponLore.stream()
                .map(line -> "<!italic>" + StringUtil.colorAdventure(line.toString()))
                .toList());
        }

        int magazineSize = (Integer) data.getConfig().get(weaponTitle + ".Reload.Magazine_Size", -1);
        if (magazineSize != -1) {
            CustomTag.AMMO_LEFT.setInteger(weaponStack, magazineSize);
        }

        String defaultSelectiveFire = data.getConfig().getString(weaponTitle + ".Shoot.Selective_Fire.Default");
        if (defaultSelectiveFire != null) {
            try {
                SelectiveFireState state = SelectiveFireState.valueOf(defaultSelectiveFire);
                CustomTag.SELECTIVE_FIRE.setInteger(weaponStack, state.ordinal());
            } catch (IllegalArgumentException e) {
                throw SerializerException.builder()
                    .location(data.getFile(), weaponTitle + ".Shoot.Selective_Fire.Default", null)
                    .buildInvalidEnumOption(defaultSelectiveFire, SelectiveFireState.class);
            }
        }

        // Weapons should, unless already customized, have a tool component to
        // not break any blocks. This lets durability bars work as well.
        ItemMeta meta = Objects.requireNonNull(weaponStack.getItemMeta());
        if (!meta.hasTool()) {
            ToolComponent tool = meta.getTool();
            tool.setDamagePerBlock(0);
            tool.setDefaultMiningSpeed(0f);
            meta.setTool(tool);
        }

        CustomTag.WEAPON_TITLE.setString(weaponStack, weaponTitle);
        weaponStack = super.serializeRecipe(data, weaponStack);
        return weaponStack;
    }
}