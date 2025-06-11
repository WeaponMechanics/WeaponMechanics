package me.deecaad.weaponmechanics.weapon.reload.ammo;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.CastData;
import me.deecaad.core.mechanics.MechanicManager;
import me.deecaad.core.mechanics.Mechanics;
import me.deecaad.core.utils.NumberUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.utils.CustomTag;
import me.deecaad.weaponmechanics.weapon.trigger.Trigger;
import me.deecaad.weaponmechanics.wrappers.PlayerWrapper;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AmmoConfig implements Serializer<AmmoConfig> {

    private MechanicManager outOfAmmoMechanics;
    private Trigger switchTrigger;
    private MechanicManager switchMechanics;
    private List<Ammo> ammunitions;

    /**
     * Default constructor for serializer.
     */
    public AmmoConfig() {
    }

    public AmmoConfig(MechanicManager outOfAmmoMechanics, Trigger switchTrigger, MechanicManager switchMechanics, List<Ammo> ammunitions) {
        this.outOfAmmoMechanics = outOfAmmoMechanics;
        this.switchTrigger = switchTrigger;
        this.switchMechanics = switchMechanics;
        this.ammunitions = ammunitions;
    }

    public MechanicManager getOutOfAmmoMechanics() {
        return outOfAmmoMechanics;
    }

    public Trigger getSwitchTrigger() {
        return switchTrigger;
    }

    public MechanicManager getSwitchMechanics() {
        return switchMechanics;
    }

    public List<Ammo> getAmmunitions() {
        return ammunitions;
    }

    public int getCurrentAmmoIndex(ItemStack weapon) {
        int ammoIndex = CustomTag.AMMO_TYPE_INDEX.getInteger(weapon);
        // Clamping is really not needed, but it can happen when an existing
        // weapon uses an ammo that was removed from config.
        return NumberUtil.clamp(ammoIndex, 0, ammunitions.size() - 1);
    }

    public void setCurrentAmmoIndex(ItemStack weapon, int index) {
        CustomTag.AMMO_TYPE_INDEX.setInteger(weapon, index);
    }

    public Ammo getCurrentAmmo(ItemStack weapon) {
        return ammunitions.get(getCurrentAmmoIndex(weapon));
    }

    public void updateToNextAmmo(ItemStack weapon) {
        int next = getCurrentAmmoIndex(weapon) + 1;
        if (next >= ammunitions.size())
            next = 0;

        setCurrentAmmoIndex(weapon, next);
    }

    public boolean hasAmmo(String weaponTitle, ItemStack weapon, PlayerWrapper player) {
        int index = getCurrentAmmoIndex(weapon);
        if (ammunitions.get(index).getType().hasAmmo(player)) {
            return true;
        }

        // At this point, we know that we don't have ammo for the current
        // ammo type, BUT the player MIGHT have ammo for different types that
        // can be loaded into the gun. Of course, for that, we need the gun to
        // be empty. Then we search.
        int ammoLeft = CustomTag.AMMO_LEFT.getInteger(weapon);
        if (ammoLeft > 0)
            return false;

        for (int i = 0; i < ammunitions.size(); i++) {
            if (i == index)
                continue; // already checked for this ^^^

            if (!ammunitions.get(i).getType().hasAmmo(player))
                continue;

            setCurrentAmmoIndex(weapon, i);
            if (switchMechanics != null)
                switchMechanics.use(new CastData(player.getPlayer(), weaponTitle, weapon));
            return true;
        }

        return false;
    }

    public int removeAmmo(ItemStack weapon, PlayerWrapper player, int amount, int maximumMagazineSize) {
        if (amount == 0)
            return 0;
        return getCurrentAmmo(weapon).getType().removeAmmo(weapon, player, amount, maximumMagazineSize);
    }

    public void giveAmmo(ItemStack weapon, PlayerWrapper playerWrapper, int amount, int maximumMagazineSize) {
        if (amount == 0)
            return;
        getCurrentAmmo(weapon).getType().giveAmmo(weapon, playerWrapper, amount, maximumMagazineSize);
    }

    public int getMaximumAmmo(ItemStack weapon, PlayerWrapper player, int maximumMagazineSize) {
        return getCurrentAmmo(weapon).getType().getMaximumAmmo(player, maximumMagazineSize);
    }

    @Override
    public String getKeyword() {
        return "Ammo";
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/weaponmechanics/weapon-modules/reload/ammo";
    }

    @NotNull @Override
    public AmmoConfig serialize(@NotNull SerializeData data) throws SerializerException {
        MechanicManager mechanics = data.of("Out_Of_Ammo_Mechanics").serialize(MechanicManager.class).orElse(null);
        Trigger switchTrigger = data.of("Ammo_Switch_Trigger").serialize(Trigger.class).orElse(null);
        MechanicManager switchMechanics = data.of("Ammo_Switch_Mechanics").serialize(MechanicManager.class).orElse(null);
        List<String> ammunitionStrings = data.of("Ammos").assertExists().get(List.class).get();

        List<Ammo> ammunitions = new ArrayList<>(ammunitionStrings.size());
        for (int i = 0; i < ammunitionStrings.size(); i++) {
            String ammoTitle = ammunitionStrings.get(i);
            Ammo ammo = WeaponMechanics.getInstance().getAmmoConfigurations().getObject(ammoTitle, Ammo.class);

            // Make sure the ammo exists
            if (ammo == null) {
                List<String> ammos = WeaponMechanics.getInstance().getAmmoConfigurations().entries()
                        .stream()
                        .filter(entry -> entry.getValue() instanceof Ammo)
                        .map(Map.Entry::getKey)
                        .toList();
                throw SerializerException.builder()
                    .locationRaw(data.ofList("Ammos").getLocation(i))
                    .addMessage("Ammo '" + ammoTitle + "' does not exist in the ammo registry.")
                    .buildInvalidOption(ammoTitle, ammos);
            }

            ammunitions.add(ammo);
        }

        return new AmmoConfig(mechanics, switchTrigger, switchMechanics, ammunitions);
    }
}
