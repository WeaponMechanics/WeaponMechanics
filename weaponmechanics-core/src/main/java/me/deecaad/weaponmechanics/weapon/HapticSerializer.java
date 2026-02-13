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

/**
 * Integrates with Vivecraft_Spigot_Extensions to send haptic pulses to a player's
 * controller.
 *
 * <p>All ViveCraft API references are deferred to avoid {@link NoClassDefFoundError}
 * when ViveCraft is not installed on the server.
 *
 * @see <a href="https://vivecraft.github.io/spigot-javadoc/org/vivecraft/api/VRAPI.html#sendHapticPulse(org.bukkit.entity.Player,org.vivecraft.api.data.VRBodyPart,float,float,float,float)">...</a>
 */
public class HapticSerializer implements Serializer<HapticSerializer> {

    // Store the VRBodyPart as Object to avoid class loading ViveCraft at class init time
    private Object part;
    private float duration;
    private float frequency;
    private float amplitude;
    private float delay;

    /**
     * Default constructor for serializer
     */
    public HapticSerializer() {
    }

    public HapticSerializer(Object part, float duration, float frequency, float amplitude, float delay) {
        this.part = part;
        this.duration = duration;
        this.frequency = frequency;
        this.amplitude = amplitude;
        this.delay = delay;
    }

    public Object getPart() {
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

        // All ViveCraft API usage is inside this inner class to defer class loading
        ViveCraftHelper.sendPulse(this, weaponTitle, weaponStack, player, hand);
    }

    @Override
    public @NotNull HapticSerializer serialize(@NotNull SerializeData data) throws SerializerException {
        try {
            Class.forName("org.vivecraft.api.data.VRBodyPart");
        } catch (ClassNotFoundException e) {
            throw data.exception("Part", "Tried to use haptics when Vivecraft_Spigot_Extensions was not installed",
                    "Install here: https://www.spigotmc.org/resources/33166/");
        }

        // ViveCraft is confirmed present, safe to use ViveCraft types now
        return ViveCraftHelper.doSerialize(data);
    }

    /**
     * Inner helper class that isolates all ViveCraft API references.
     * This class is only loaded when ViveCraft is confirmed to be present,
     * preventing NoClassDefFoundError at HapticSerializer class init time.
     */
    static class ViveCraftHelper {

        static HapticSerializer doSerialize(SerializeData data) throws SerializerException {
            org.vivecraft.api.data.VRBodyPart part = data.of("Part").getEnum(org.vivecraft.api.data.VRBodyPart.class).orElse(null);
            float duration = (float) data.of("Duration").assertRange(0.0, null).assertExists().getDouble().orElseThrow();
            float frequency = (float) data.of("Frequency").assertRange(0.0, null).assertExists().getDouble().orElse(160.0F);
            float amplitude = (float) data.of("Amplitude").assertRange(0.0, null).assertExists().getDouble().orElse(1.0F);
            float delay = (float) data.of("Delay").assertRange(0.0, null).assertExists().getDouble().orElse(0.0F);
            return new HapticSerializer(part, duration, frequency, amplitude, delay);
        }

        static void sendPulse(HapticSerializer haptic, String weaponTitle, ItemStack weaponStack, Player player, EquipmentSlot hand) {
            org.vivecraft.api.data.VRBodyPart vrPart = (org.vivecraft.api.data.VRBodyPart) haptic.part;

            WeaponHapticEvent e = new WeaponHapticEvent(weaponTitle, weaponStack, player, hand, vrPart,
                    haptic.duration, haptic.frequency, haptic.amplitude, haptic.delay);
            Bukkit.getPluginManager().callEvent(e);
            if (e.isCancelled())
                return;

            org.vivecraft.api.data.VRBodyPart eventPart = e.getPart();
            if (eventPart == null) {
                if (hand == null)
                    throw new IllegalArgumentException("Found a haptic with no specific body part defined... This is likely a config error.");

                // Map EquipmentSlot to VRBodyPart
                eventPart = switch (hand) {
                    case HAND -> org.vivecraft.api.data.VRBodyPart.MAIN_HAND;
                    case OFF_HAND -> org.vivecraft.api.data.VRBodyPart.OFF_HAND;
                    default -> null;
                };
            }

            // If no body part is available, haptic everything
            if (eventPart == null) {
                for (org.vivecraft.api.data.VRBodyPart part : org.vivecraft.api.data.VRBodyPart.values()) {
                    org.vivecraft.api.VRAPI.instance().sendHapticPulse(player, part, e.getDuration(), e.getFrequency(), e.getAmplitude(), e.getDelay());
                }
            } else {
                org.vivecraft.api.VRAPI.instance().sendHapticPulse(player, eventPart, e.getDuration(), e.getFrequency(), e.getAmplitude(), e.getDelay());
            }
        }
    }
}
