package me.deecaad.weaponmechanics.weapon.skin;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.utils.StringUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RelativeSkin implements Skin, Serializer<RelativeSkin> {

    private int customModelData;

    /**
     * Default constructor for serializer.
     */
    public RelativeSkin() {
    }

    public RelativeSkin(int customModelData) {
        this.customModelData = customModelData;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    @Override
    public void apply(@NotNull ItemStack item) {
        // Instead of using Skin#apply use SkinSelector#apply
        throw new UnsupportedOperationException("apply");
    }

    @NotNull @Override
    public RelativeSkin serialize(@NotNull SerializeData data) throws SerializerException {

        if (data.of().is(ConfigurationSection.class)) {
            throw data.exception(null, "Tried to override a 'Relative Skin' with 'Normal Skin'",
                "When using the '+10' feature of skins, ALL of your skins on this weapon must use the + feature");
        }

        String str = data.of().assertExists().get(Object.class).get().toString().trim().toLowerCase();

        if (str.startsWith("+"))
            str = str.substring(1).trim();
        if (str.startsWith("add"))
            str = str.substring(3).trim();

        int cmd;
        try {
            cmd = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            throw SerializerException.builder()
                .locationRaw(data.of().getLocation())
                .addMessage("Invalid number format for relative skin")
                .buildInvalidType("Integer", str);
        }

        // Strict mode so users cannot make mistakes (they must follow a strict format though)
        if (WeaponMechanics.getInstance().getConfiguration().getBoolean("Strict_Relative_Skins", true)) {
            int zeros = StringUtil.countOccurrences(str, '0');
            int nonzeros = str.length() - zeros;
            boolean isAttachment = data.getKey().contains("Attachments");
            String action = data.getKey().substring(data.getKey().lastIndexOf('.') + 1);
            boolean isSkin = SkinSelector.SkinAction.fromString(action) == null;

            // Negative numbers are confusing... especially when mixing with positive.
            if (cmd < 0) {
                throw data.exception(null, "You cannot use negative numbers as a relative skin. You have to ADD numbers.",
                    "For value: " + str);
            }

            // 0 probably means the option doesn't exist since... why add 0?
            if (cmd == 0) {
                throw data.exception(null, "You cannot use 0 as a relative skin. If you want to delete the option, you should delete the entire line",
                    "If you use 0 as a custom model data, you should change it in the Resource Pack to 1 and in the config to 1");
            }

            // We want exactly 1 significant figure. 11000 is both a skin and scoping, which is bad.
            // We should only have 1 significant digit controlling the skin for each relative skin.
            else if (nonzeros != 1) {
                throw data.exception(null, "When using relative skins, you must use exactly 1 non-zero digit. For you Science nerds, that means 1 sig-fig",
                    "For example, '11000' is NOT allowed but '10000' is good",
                    "For value: " + str);
            }

            // Attachments can be multiples of 100,000; 1,000,000; 10,000,000; 100,000,000; or 1,000,000,000
            // So this check makes sure that attachments have exactly 1 sig-fig, and at least 5 zeros.
            else if (isAttachment) {
                if (zeros < 5) {
                    throw data.exception(null, "100,000 is the minimum number required for relative attachment skins",
                        "Attachments can be a multiple of 100,000 or 1,000,000 or 10,000,000 or 100,000,000 or 1,000,000,000",
                        "Which means you have to use at least 5 zeros in the skin. For example, '10,000' is a bad number but '100,000' is good",
                        "For value: " + str);
                }
            }

            // Skins can be a multiple of 10,000 (up to 90,000)
            else if (isSkin) {
                if (cmd < 10_000 || cmd > 90_000) {
                    throw data.exception(null, "Relative skins have to be a multiple of 10,000. This means that 90,000 is the max",
                        "For value: " + str);
                }
            }

            // Scope should be +1000. Plain and simple, keep it organized. Rest is up to player
            else if ("Scope".equals(action)) {
                if (cmd != 1000) {
                    throw data.exception(null, "When using relative skins, scoping should always be 'ADD 1000'",
                        "Using 1000 for scoping keeps your resource pack organized, which avoids errors in the future",
                        "For value: " + str);
                }
            }

            // At this point, we know that the config is an action like sprinting/reloading.
            // These actions should be multiples of 1000, and be [2000, 9000]
            else if (cmd < 2000 || cmd > 9000) {
                throw data.exception(null, "The '" + action + "' skin should be a multiple of 1000 and be between 2000 and 9000",
                    "For example, WeaponMechanics default weapons use 'Sprint: ADD 2000' and 'Reload: ADD 3000'",
                    "For value: " + str);
            }
        }

        return new RelativeSkin(cmd);
    }
}
