package me.deecaad.weaponmechanics.weapon.trigger;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.CastData;
import me.deecaad.core.mechanics.MechanicManager;
import me.deecaad.core.mechanics.Mechanics;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Circumstance implements Serializer<Circumstance> {

    private List<CircumstanceData> circumstances;
    private MechanicManager denyMechanics;

    /**
     * Default constructor for serializer
     */
    public Circumstance() {
    }

    public Circumstance(List<CircumstanceData> circumstances, MechanicManager denyMechanics) {
        this.circumstances = circumstances;
        this.denyMechanics = denyMechanics;
    }

    /**
     * If entity wrapper is null, this will always return true
     *
     * @param entityWrapper the entity wrapper or null if not used
     * @return whether to deny
     */
    public boolean deny(@Nullable EntityWrapper entityWrapper) {
        if (entityWrapper == null)
            return true;

        for (CircumstanceData circumstance : this.circumstances) {
            if (circumstance.deny(entityWrapper)) {
                if (denyMechanics != null) {
                    CastData cast = new CastData(entityWrapper.getEntity(), null, null);
                    cast.placeholders().put("deny_reason", circumstance.circumstanceType.getHumanName());
                    denyMechanics.use(cast);
                }
                return true;
            }
        }
        return false;
    }

    @NotNull @Override
    public Circumstance serialize(@NotNull SerializeData data) throws SerializerException {
        ConfigurationSection circumstanceSection = data.of().assertExists().get(ConfigurationSection.class).get();
        List<CircumstanceData> circumstances = new ArrayList<>(1);
        MechanicManager denyMechanics = data.of("Deny_Mechanics").serialize(MechanicManager.class).orElse(null);

        for (String type : circumstanceSection.getKeys(false)) {
            if (type.equals("Deny_Mechanics"))
                continue;

            String typeToUpper = type.toUpperCase(Locale.ROOT);

            // safe to get the optional without checking... we are in the loop of existing keys :)
            String value = data.of(type).get(String.class).get();
            if (!value.equalsIgnoreCase("DENY") && !value.equalsIgnoreCase("REQUIRED")) {
                throw data.exception(type, "Only DENY and REQUIRED are allowed, now there was " + value + "!");
            }

            try {
                circumstances.add(new CircumstanceData(CircumstanceType.valueOf(typeToUpper), value.equalsIgnoreCase("REQUIRED")));
            } catch (IllegalArgumentException e) {
                throw SerializerException.builder()
                    .locationRaw(data.of(type).getLocation())
                    .buildInvalidEnumOption(type, CircumstanceType.class);
            }
        }

        return new Circumstance(circumstances, denyMechanics);
    }

    public record CircumstanceData(CircumstanceType circumstanceType, boolean required) {
        public boolean deny(EntityWrapper entityWrapper) {
            return required != switch (circumstanceType) {
                case RELOADING -> entityWrapper.isReloading();
                case ZOOMING -> entityWrapper.isZooming();
                case SNEAKING -> entityWrapper.isSneaking();
                case STANDING -> entityWrapper.isStanding();
                case WALKING -> entityWrapper.isWalking();
                case RIDING -> entityWrapper.isRiding();
                case SPRINTING -> entityWrapper.isSprinting();
                case DUAL_WIELDING -> entityWrapper.isDualWielding();
                case SWIMMING -> entityWrapper.isSwimming();
                case IN_MIDAIR -> entityWrapper.isInMidair();
                case GLIDING -> entityWrapper.isGliding();
                case AMMO_EMPTY -> entityWrapper.isAmmoEmpty();
            };
        }
    }

    private enum CircumstanceType {
        RELOADING,
        ZOOMING,
        SNEAKING,
        STANDING,
        WALKING,
        RIDING,
        SPRINTING,
        DUAL_WIELDING,
        SWIMMING,
        IN_MIDAIR,
        GLIDING,
        AMMO_EMPTY;

        private final String humanName;

        CircumstanceType() {
            this.humanName = name().toLowerCase(Locale.ROOT).replace("_", " ");
        }

        public String getHumanName() {
            return humanName;
        }
    }
}