package me.deecaad.weaponmechanics.weapon.skin;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class BaseSkinSelector implements SkinSelector, Serializer<SkinSelector> {

    private Map<String, Map<SkinAction, BaseSkin>> map;

    /**
     * Default constructor for serializer.
     */
    public BaseSkinSelector() {
    }

    public BaseSkinSelector(Map<String, Map<SkinAction, BaseSkin>> map) {
        this.map = map;
    }

    public @Nullable Skin getSkin(@Nullable String skin, @Nullable SkinAction action, @Nullable String[] attachments) {
        if (skin == null)
            skin = "default";
        if (action == null)
            action = SkinAction.DEFAULT;

        Map<SkinAction, BaseSkin> temp = map.get(skin);
        return temp == null ? null : temp.get(action);
    }

    @Override
    public @NotNull BaseSkin getDefaultSkin() {
        return (BaseSkin) Objects.requireNonNull(getSkin(null, null, null), "Impossible Error Message");
    }

    @Override
    public @NotNull Set<String> getCustomSkins() {
        Set<String> copy = new HashSet<>(map.keySet());
        copy.remove("Default");
        return copy;
    }

    @Override
    public @Nullable Set<SkinAction> getActions(@Nullable String skin) {
        Map<SkinAction, BaseSkin> actions = map.get(skin);
        return actions == null ? null : new HashSet<>(actions.keySet());
    }

    @Override
    public @Nullable Set<String> getAttachments(@Nullable String skin) {
        // weird code, but it follows the contract. Return null if the skin
        // doesn't exist, then return the set of attachments (which is always none).
        Map<SkinAction, BaseSkin> actions = map.get(skin);
        return actions == null ? null : new HashSet<>();
    }

    @Override
    public boolean hasAction(@Nullable String skin, @Nullable SkinAction action) {
        return getSkin(skin, action, null) != null;
    }

    @Override
    public void apply(@NotNull ItemStack weapon, @Nullable String skin, @Nullable SkinAction action, @Nullable String[] attachments) {
        Skin s = getSkin(skin, action, attachments);
        if (s != null)
            s.apply(weapon);
    }

    @Override
    public String getKeyword() {
        return "Skin";
    }

    @NotNull @Override
    public SkinSelector serialize(@NotNull SerializeData data) throws SerializerException {
        Map<String, Map<SkinAction, BaseSkin>> map = new HashMap<>();
        Map<SkinAction, BaseSkin> defaultSkinData = new HashMap<>();

        // Extra check to make sure the user has a default skin
        data.of(SkinAction.DEFAULT.getKey()).assertExists();

        ConfigurationSection section = data.of().assertExists().get(ConfigurationSection.class).get();
        Set<String> keys = section.getKeys(false);
        for (String key : keys) {
            SkinAction action = SkinAction.fromString(key);

            // If we found a string, we should definitely be using the relative selector instead.
            // Of course, skip Item for inline HAND item serializer.
            if (data.of(key).is(String.class) || data.of(key).is(Integer.class))
                if (!"Item".equals(key))
                    return new RelativeSkinSelector().serialize(data);

            if (action == null) {

                if ("Attachments".equals(key)) {
                    throw data.exception(null, "If you want to use Attachments in skins, you cannot use the legacy format",
                        "Wiki: https://cjcrafter.gitbook.io/weaponmechanics/weapon-modules/skin");
                }

                // Hand skin requires 'Item'.
                if ("Item".equals(key))
                    continue;

                // This is either an error, or the admin is trying to use
                // relative skins. Assume relative skins, let it handle any error.
                if (!data.of(key).is(ConfigurationSection.class)) {
                    return new RelativeSkinSelector().serialize(data);
                }

                Map<SkinAction, BaseSkin> temp = serializeOne(data.move(key));
                map.put(key.toLowerCase(Locale.ROOT), temp);
                continue;
            }

            // Since SCOPE_STACK matches to different keys, we need to handle
            // Scope_1, Scope_2, Scope_3, etc.
            if (action == SkinAction.SCOPE_STACK)
                action = new SkinAction(key);

            defaultSkinData.put(action, data.of(key).assertExists().serialize(BaseSkin.class).get());
        }

        map.put("default", defaultSkinData);
        return new BaseSkinSelector(map);
    }

    private Map<SkinAction, BaseSkin> serializeOne(SerializeData data) throws SerializerException {
        Map<SkinAction, BaseSkin> map = new HashMap<>();

        // Extra check to make sure the user has a default skin
        data.of(SkinAction.DEFAULT.getKey()).assertExists();

        ConfigurationSection section = data.of().assertExists().get(ConfigurationSection.class).get();
        Set<String> keys = section.getKeys(false);
        for (String key : keys) {
            SkinAction action = SkinAction.fromString(key);

            if (action == null) {
                List<String> possibleActions = Arrays.stream(SkinAction.getValues()).map(SkinAction::getKey).toList();
                throw SerializerException.builder()
                    .addMessage("Wiki: https://cjcrafter.gitbook.io/weaponmechanics/weapon-modules/skin")
                    .buildInvalidOption(key, possibleActions);
            }

            // Since SCOPE_STACK matches to different keys, we need to handle
            // Scope_1, Scope_2, Scope_3, etc.
            if (action == SkinAction.SCOPE_STACK)
                action = new SkinAction(key);

            map.put(action, data.of(key).assertExists().serialize(BaseSkin.class).get());
        }

        return map;
    }
}
