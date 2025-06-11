package me.deecaad.weaponmechanics.weapon.placeholders;

import me.deecaad.core.file.Configuration;
import me.deecaad.core.file.IValidator;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.utils.StringUtil;

public class PlaceholderValidator implements IValidator {

    /**
     * Default constructor for validator
     */
    public PlaceholderValidator() {
    }

    @Override
    public String getKeyword() {
        return "Placeholder_Symbols";
    }

    @Override
    public void validate(Configuration configuration, SerializeData data) throws SerializerException {

        for (String firearm : new String[]{"REVOLVER", "PUMP", "LEVER", "SLIDE"}) {
            convert(configuration, "Placeholder_Symbols." + firearm + ".Open");
            convert(configuration, "Placeholder_Symbols." + firearm + ".Close");
        }

        convert(configuration, "Placeholder_Symbols.Reload");
        convert(configuration, "Placeholder_Symbols.Selective_Fire.SINGLE");
        convert(configuration, "Placeholder_Symbols.Selective_Fire.BURST");
        convert(configuration, "Placeholder_Symbols.Selective_Fire.AUTO");
        convert(configuration, "Placeholder_Symbols.Dual_Wield.Split");
    }

    private void convert(Configuration configuration, String key) {
        configuration.set(key, StringUtil.colorAdventure(configuration.getString(key)));
    }
}
