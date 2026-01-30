package me.deecaad.weaponmechanics.weapon;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponHapticEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vivecraft.api.VRAPI;
import org.vivecraft.api.data.VRBodyPart;

import java.util.EnumMap;
import java.util.Map;

/**
 * Integrates with Vivecraft_Spigot_Extensions to send haptic pulses to a player's
 * controller.
 *
 * @see <a href="https://vivecraft.github.io/spigot-javadoc/org/vivecraft/api/VRAPI.html#sendHapticPulse(org.bukkit.entity.Player,org.vivecraft.api.data.VRBodyPart,float,float,float,float)">...</a>
 */
public class HapticSerializer implements Serializer<HapticSerializer> {

    private static final Map<EquipmentSlot, VRBodyPart> SLOT_TO_VR_MAP = new EnumMap<>(EquipmentSlot.class);

    static {
        // TODO: group these smarter?
        SLOT_TO_VR_MAP.put(EquipmentSlot.HAND, VRBodyPart.MAIN_HAND);
        SLOT_TO_VR_MAP.put(EquipmentSlot.OFF_HAND, VRBodyPart.OFF_HAND);
    }

    private VRBodyPart part;
    private float duration;
    private float frequency;
    private float amplitude;
    private float delay;

    /**
     * Default constructor for serializer
     */
    public HapticSerializer() {
    }

    public HapticSerializer(VRBodyPart part, float duration, float frequency, float amplitude, float delay) {
        this.part = part;
        this.duration = duration;
        this.frequency = frequency;
        this.amplitude = amplitude;
        this.delay = delay;
    }

    public VRBodyPart getPart() {
        return part;
    }

    public float getDuration() {
        return duration;
    }

    public float getFrequency() {
        return frequency;
    }

    public float getAmplitude() {
        return amplitude;
    }

    public float getDelay() {
        return delay;
    }

    public void sendHapticPulse(@NotNull String weaponTitle, @NotNull ItemStack weaponStack, @NotNull LivingEntity shooter, @Nullable EquipmentSlot hand) {
        if (!(shooter instanceof Player player))
            return;

        WeaponHapticEvent e = new WeaponHapticEvent(weaponTitle, weaponStack, shooter, hand, part, duration, frequency, amplitude, delay);
        Bukkit.getPluginManager().callEvent(e);
        if (e.isCancelled())
            return;

        VRBodyPart part = e.getPart();
        if (part == null) {
            if (hand == null)
                throw new IllegalArgumentException("Found a haptic with no specific body part defined... This is likely a config error.");

            part = SLOT_TO_VR_MAP.get(hand);
        }

        // If no body part is available, haptic everything
        if (part == null) {
            for (VRBodyPart vrPart : VRBodyPart.values()) {
                VRAPI.instance().sendHapticPulse(player, vrPart, e.getDuration(), e.getFrequency(), e.getAmplitude(), e.getDelay());
            }
        } else {
            VRAPI.instance().sendHapticPulse(player, part, e.getDuration(), e.getFrequency(), e.getAmplitude(), e.getDelay());
        }
    }

    @Override
    public @NotNull HapticSerializer serialize(@NotNull SerializeData data) throws SerializerException {
        try {
            Class.forName("org.vivecraft.api.data.VRBodyPart");
        } catch (ClassNotFoundException e) {
            throw data.exception("Part", "Tried to use haptics when Vivecraft_Spigot_Extensions was not installed",
                    "Install here: https://www.spigotmc.org/resources/33166/");
        }

        VRBodyPart part = data.of("Part").getEnum(VRBodyPart.class).orElse(null);
        float duration = (float) data.of("Duration").assertRange(0.0, null).assertExists().getDouble().orElseThrow();
        float frequency = (float) data.of("Frequency").assertRange(0.0, null).assertExists().getDouble().orElse(160.0F);
        float amplitude = (float) data.of("Amplitude").assertRange(0.0, null).assertExists().getDouble().orElse(1.0F);
        float delay = (float) data.of("Delay").assertRange(0.0, null).assertExists().getDouble().orElse(0.0F);
        return new HapticSerializer(part, duration, frequency, amplitude, delay);
    }
}
