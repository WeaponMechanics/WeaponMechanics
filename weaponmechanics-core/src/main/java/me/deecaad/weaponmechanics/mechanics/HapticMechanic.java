package me.deecaad.weaponmechanics.mechanics;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.CastData;
import me.deecaad.core.mechanics.defaultmechanics.Mechanic;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.HapticSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.vivecraft.api.data.VRBodyPart;

public class HapticMechanic extends Mechanic {

    private HapticSerializer haptic;

    /**
     * Default serializer for constructor
     */
    public HapticMechanic() {
    }

    public HapticMechanic(HapticSerializer haptic) {
        this.haptic = haptic;
    }

    @Override
    protected void use0(CastData cast) {
        if (!(cast.getTarget() instanceof Player player))
            return;

        haptic.sendHapticPulse(cast.itemTitle(), cast.item(), player, null);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(WeaponMechanics.getInstance(), "haptic");
    }

    @Override
    public @NotNull Mechanic serialize(@NotNull SerializeData data) throws SerializerException {
        try {
            Class.forName("org.vivecraft.api.data.VRBodyPart");
        } catch (ClassNotFoundException e) {
            throw data.exception("Part", "Tried to use haptics when Vivecraft_Spigot_Extensions was not installed",
                    "Install here: https://www.spigotmc.org/resources/33166/");
        }

        VRBodyPart part = data.of("Part").assertExists().getEnum(VRBodyPart.class).orElse(null);
        float duration = (float) data.of("Duration").assertRange(0.0, null).assertExists().getDouble().orElseThrow();
        float frequency = (float) data.of("Frequency").assertRange(0.0, null).assertExists().getDouble().orElse(160.0F);
        float amplitude = (float) data.of("Amplitude").assertRange(0.0, null).assertExists().getDouble().orElse(1.0F);
        float delay = (float) data.of("Delay").assertRange(0.0, null).assertExists().getDouble().orElse(0.0F);

        HapticSerializer haptic = new HapticSerializer(part, duration, frequency, amplitude, delay);
        return applyParentArgs(data, new HapticMechanic(haptic));
    }
}
