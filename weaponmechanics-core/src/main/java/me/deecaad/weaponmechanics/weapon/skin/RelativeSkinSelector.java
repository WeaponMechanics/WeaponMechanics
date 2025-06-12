package me.deecaad.weaponmechanics.weapon.skin;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.weaponmechanics.WeaponMechanics;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class RelativeSkinSelector implements SkinSelector, Serializer<RelativeSkinSelector> {

    private BaseSkin base;
    private Map<String, RelativeSkin> skins;
    private Map<SkinAction, RelativeSkin> actions;
    private Map<String, RelativeSkin> attachments;

    /**
     * Default constructor for serializer
     */
    public RelativeSkinSelector() {
    }

    public RelativeSkinSelector(BaseSkin base, Map<String, RelativeSkin> skins, Map<SkinAction, RelativeSkin> actions, Map<String, RelativeSkin> attachments) {
        this.base = base;
        this.skins = skins;
        this.actions = actions;
        this.attachments = attachments;
    }

    public Map<String, RelativeSkin> getSkins() {
        return skins;
    }

    public Map<SkinAction, RelativeSkin> getActions() {
        return actions;
    }

    public Map<String, RelativeSkin> getAttachments() {
        return attachments;
    }

    @Override
    public @NotNull BaseSkin getDefaultSkin() {
        return base;
    }

    @Override
    public @NotNull Set<String> getCustomSkins() {
        return new HashSet<>(skins.keySet());
    }

    @Override
    public @NotNull Set<SkinAction> getActions(@Nullable String skin) {
        return new HashSet<>(actions.keySet());
    }

    @Override
    public @Nullable Set<String> getAttachments(@Nullable String skin) {
        return new HashSet<>(attachments.keySet());
    }

    @Override
    public boolean hasAction(@Nullable String skin, @Nullable SkinAction action) {
        return actions.containsKey(action);
    }

    @Override
    public void apply(@NotNull ItemStack weapon, @Nullable String skin, @Nullable SkinAction action, @Nullable String[] attachments) {

        // Ideally this will never be true, but sometimes we try to apply skins
        // to AIR which causes errors.
        ItemMeta meta = weapon.getItemMeta();
        if (meta == null)
            return;

        // We set these to null because we must skip them. base is always
        // added, we shouldn't add it twice.
        if ("default".equals(skin))
            skin = null;
        if (action == SkinAction.DEFAULT)
            action = null;

        // Determine which custom model data to use.
        int customModelData = base.getCustomModelData();
        RelativeSkin relativeSkin;
        if (skin != null && (relativeSkin = skins.get(skin)) != null)
            customModelData += relativeSkin.getCustomModelData();
        RelativeSkin relativeAction;
        if (action != null && (relativeAction = actions.get(action)) != null)
            customModelData += relativeAction.getCustomModelData();
        if (attachments != null) {
            for (String attachment : attachments) {
                RelativeSkin relativeAttachment = this.attachments.get(attachment);
                if (relativeAttachment != null)
                    customModelData += relativeAttachment.getCustomModelData();
            }
        }

        if (base.hasType() && weapon.getType().asItemType() != base.getType())
            weapon.setType(base.getType().asMaterial());

        meta = weapon.getItemMeta();
        if (meta == null) {
            WeaponMechanics.getInstance().debugger.warning("Item w/ skin suddenly lost metadata... Did you redefine item type to AIR?");
            return;
        }

        meta.setCustomModelData(customModelData);
        weapon.setItemMeta(meta);
    }

    @NotNull @Override
    public RelativeSkinSelector serialize(@NotNull SerializeData data) throws SerializerException {

        Map<String, RelativeSkin> skins = new LinkedHashMap<>();
        Map<SkinAction, RelativeSkin> actions = new LinkedHashMap<>();
        Map<String, RelativeSkin> attachments = new LinkedHashMap<>();

        // The base skin is the starting integer for the custom_model_data
        BaseSkin base = new BaseSkin(data.of("Default").assertExists().getInt().getAsInt());

        // Strict check
        if (WeaponMechanics.getInstance().getConfiguration().getBoolean("Strict_Relative_Skins", true)) {

            // base skin should be [1, 999]
            if (base.getCustomModelData() < 1 || base.getCustomModelData() > 999) {
                throw data.exception("Default", "Your 'Default: " + base.getCustomModelData() + "' is incorrect",
                    "The default skin should be a number between 1 and 999");
            }
        }

        // Now everything else is expected to be a relative skin. Anything else
        // is an error.
        ConfigurationSection section = data.of().assertExists().get(ConfigurationSection.class).get();
        Set<String> keys = section.getKeys(false);
        for (String key : keys) {

            // Attachments -> it is handled later
            // Item -> it is specifically for hand items, and handled by WMC
            // Default -> it is already handled in the BaseSkin
            if ("Attachments".equals(key) || "Item".equals(key) || "Default".equals(key))
                continue;

            // When this is null, that means that it is a skin
            SkinAction action = SkinAction.fromString(key);
            if (action == null) {
                skins.put(key, data.of(key).assertExists().serialize(RelativeSkin.class).get());
                continue;
            }

            // SCOPE_STACK matches SCOPE_1, SCOPE_2, etc.. So we need to make a proper one
            if (action == SkinAction.SCOPE_STACK)
                action = new SkinAction(key);

            actions.put(action, data.of(key).assertExists().serialize(RelativeSkin.class).get());
        }

        // Check for attachments
        Set<String> attachmentKeys = data.of("Attachments").get(ConfigurationSection.class)
            .map(attachmentSection -> attachmentSection.getKeys(false))
            .orElse(null);

        if (attachmentKeys != null) {
            for (String attachment : attachmentKeys) {

                // We can't really check if an attachment exists since we
                // cannot access the WMP API, so users may be prone to error
                RelativeSkin relativeSkin = data.of("Attachments." + attachment).serialize(RelativeSkin.class).orElse(null);
                if (relativeSkin == null) {
                    throw data.exception("Attachments." + attachment, "Some error occurred when making the relative skin",
                        "Please make sure you formatted things correctly... An example value is something like '+10' or '+100'");
                }

                attachments.put(attachment, relativeSkin);
            }
        }

        return new RelativeSkinSelector(base, skins, actions, attachments);
    }
}
