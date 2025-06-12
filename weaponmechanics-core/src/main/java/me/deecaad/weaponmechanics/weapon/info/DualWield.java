package me.deecaad.weaponmechanics.weapon.info;

import me.deecaad.core.file.*;
import me.deecaad.core.file.simple.StringSerializer;
import me.deecaad.core.mechanics.CastData;
import me.deecaad.core.mechanics.MechanicManager;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.trigger.Trigger;
import me.deecaad.weaponmechanics.weapon.trigger.TriggerType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@SearcherFilter(SearchMode.ON_DEMAND)
public class DualWield implements Serializer<DualWield> {

    private boolean whitelist;
    private Set<String> weapons;
    private MechanicManager mechanics;

    /**
     * Default constructor for serializer
     */
    public DualWield() {
    }

    public DualWield(boolean whitelist, Set<String> weapons, MechanicManager mechanics) {
        this.whitelist = whitelist;
        this.weapons = weapons;
        this.mechanics = mechanics;
    }

    /**
     * Checks whether or not this can be used with other weapon title
     *
     * @param weaponTitle the other weapon title
     * @return true only if dual wielding is allowed
     */
    public boolean denyDualWieldingWith(String weaponTitle) {
        if (!whitelist) {
            // If blacklist and list contains weapon title
            // -> dual wield is not allowed
            return weapons.contains(weaponTitle.toLowerCase(Locale.ROOT));
        }
        // If whitelist and list DOES not contains weapon title
        // -> dual wield is not allowed
        return !weapons.contains(weaponTitle.toLowerCase(Locale.ROOT));
    }

    /**
     * Simply sends dual wield denied message for player. Message is only sent if dual wield check cause
     * is same as weapon's shoot, scope or reload trigger type.
     *
     * @param checkCause the cause of this dual wield check
     * @param player the player for who to send
     * @param weaponTitle the weapon used
     */
    public void sendDeniedMessage(TriggerType checkCause, @Nullable Player player, String weaponTitle) {
        if (player != null) {
            Configuration config = WeaponMechanics.getInstance().getWeaponConfigurations();
            for (String type : new String[]{".Shoot", ".Reload", ".Scope"}) {
                Trigger trigger = config.getObject(weaponTitle + type + ".Trigger", Trigger.class);
                if (trigger != null && (trigger.getMainhand() == checkCause || trigger.getOffhand() == checkCause)) {

                    if (mechanics != null)
                        mechanics.use(new CastData(player, weaponTitle, null));

                    break;
                }
            }
        }
    }

    @Override
    public String getKeyword() {
        return "Dual_Wield";
    }

    @Override
    public @NotNull DualWield serialize(@NotNull SerializeData data) throws SerializerException {
        Set<String> weapons = data.ofList("Weapons")
            .addArgument(new StringSerializer())
            .requireAllPreviousArgs()
            .assertExists()
            .assertList()
            .stream()
            .map(split -> split.get(0).get().toString().trim().toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());

        // Saves weapons in lower case
        boolean whitelist = data.of("Whitelist").getBool().orElse(false);
        MechanicManager mechanics = data.of("Mechanics_On_Deny").serialize(MechanicManager.class).orElse(null);
        return new DualWield(whitelist, weapons, mechanics);
    }
}