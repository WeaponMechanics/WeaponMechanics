package me.deecaad.weaponmechanics.weapon.skin;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface SkinSelector {

    /**
     * Returns the default skin (The skin used when given by command, and often when just sitting in the
     * inventory). This method ignores player preference, the {@link SkinAction}, and any attachments,
     * this method simply returns the default skin.
     * 
     * @return The default skin.
     */
    @NotNull BaseSkin getDefaultSkin();

    /**
     * Applies the default skin ({@link #getDefaultSkin()}) to the given weapon.
     *
     * @param weapon The item to apply the skin to.
     */
    default void applyDefaultSkin(@NotNull ItemStack weapon) {
        getDefaultSkin().apply(weapon);
    }

    /**
     * Returns a list of custom skins that were configured. May return an empty set if no skins are
     * configured. This method will still return the full list of skins, even if
     * WeaponMechanicsCosmetics is not installed.
     *
     * @return All configured cosmetic skins.
     */
    @NotNull Set<String> getCustomSkins();

    /**
     * Returns a list of skin actions present for the given skin. If the skin does not exist, this
     * method will return <code>null</code> (Except for relative skins, which will always return the
     * full set of actions regardless of input).
     *
     * @param skin The skin to check, or null for default.
     * @return The present actions.
     */
    @Nullable Set<SkinAction> getActions(@Nullable String skin);

    /**
     * Returns a list of attachments for the given skin. If the skin does not exist, this method will
     * return <code>null</code> (Except for relative skins, which will always return the full set of
     * attachments, else an empty set).
     *
     * @param skin The skin to check, or null for default.
     * @return The present attachments.
     */
    @Nullable Set<String> getAttachments(@Nullable String skin);

    /**
     * Returns <code>true</code> if the given <code>action</code> exists for the skin. Used by the
     * {@link SkinHandler}.
     *
     * @param skin Which skin to check, or null for default.
     * @param action Which action to check (Using null is allowed... but why would you use it???).
     * @return true if the action exists.
     */
    boolean hasAction(@Nullable String skin, @Nullable SkinAction action);

    /**
     * Applies the skin if one is present.
     *
     * @param weapon The weapon to apply the skin to.
     * @param skin The skin color to use, usually player's preference. Use null for default.
     * @param action The current action (scope/sprint/reload...). Use null for default.
     * @param attachments All attachments on the weapon, use null (or empty) for none.
     */
    void apply(@NotNull ItemStack weapon, @Nullable String skin, @Nullable SkinAction action, @Nullable String[] attachments);

    /**
     * Identifier for actions, like the default skin, scoping, sprinting, etc.
     */
    class SkinAction {

        public static final SkinAction DEFAULT = new SkinAction("Default");
        public static final SkinAction SCOPE = new SkinAction("Scope");
        public static final SkinAction SCOPE_STACK = new SkinAction("Scope_\\d\\d?", true);
        public static final SkinAction NO_AMMO = new SkinAction("No_Ammo");
        public static final SkinAction RELOAD = new SkinAction("Reload");
        public static final SkinAction SPRINT = new SkinAction("Sprint");

        private static final SkinAction[] VALUES = new SkinAction[]{DEFAULT, SCOPE, SCOPE_STACK, NO_AMMO, RELOAD, SPRINT};

        private final String key;
        private final boolean useRegex;

        public SkinAction(String key) {
            this(key, false);
        }

        public SkinAction(String key, boolean useRegex) {
            this.key = key;
            this.useRegex = useRegex;
        }

        public String getKey() {
            return key;
        }

        public boolean matches(String key) {
            if (useRegex) {
                Pattern pattern = Pattern.compile(this.key);
                Matcher matcher = pattern.matcher(key);
                return matcher.matches();
            }

            return this.key.equals(key);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            SkinAction that = (SkinAction) o;
            return key.equals(that.key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        public static SkinAction fromString(String str) {
            for (SkinAction identifier : VALUES) {
                if (identifier.matches(str))
                    return identifier;
            }

            return null;
        }

        public static SkinAction[] getValues() {
            return VALUES;
        }
    }
}
