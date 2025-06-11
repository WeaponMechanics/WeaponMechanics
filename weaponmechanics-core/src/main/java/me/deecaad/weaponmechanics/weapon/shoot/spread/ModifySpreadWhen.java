package me.deecaad.weaponmechanics.weapon.shoot.spread;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.weaponmechanics.weapon.shoot.AModifyWhen;
import me.deecaad.weaponmechanics.weapon.shoot.NumberModifier;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import org.jetbrains.annotations.NotNull;

public class ModifySpreadWhen extends AModifyWhen {

    /**
     * Empty constructor to be used as serializer
     */
    public ModifySpreadWhen() {
    }

    public ModifySpreadWhen(NumberModifier always, NumberModifier zooming, NumberModifier sneaking,
        NumberModifier crawling, NumberModifier standing, NumberModifier walking,
        NumberModifier riding, NumberModifier sprinting, NumberModifier dualWielding,
        NumberModifier swimming, NumberModifier inMidair, NumberModifier gliding) {
        super(always, zooming, sneaking, crawling, standing, walking, riding, sprinting, dualWielding, swimming, inMidair, gliding);
    }

    @Override
    public double applyChanges(EntityWrapper entityWrapper, double tempNumber) {
        return Math.max(super.applyChanges(entityWrapper, tempNumber), 0.0);
    }

    @Override
    public String getKeyword() {
        return "Modify_Spread_When";
    }

    @Override
    @NotNull public ModifySpreadWhen serialize(@NotNull SerializeData data) throws SerializerException {
        NumberModifier always = getModifierHandler(data.of("Always"));
        NumberModifier zooming = getModifierHandler(data.of("Zooming"));
        NumberModifier sneaking = getModifierHandler(data.of("Sneaking"));
        NumberModifier crawling = getModifierHandler(data.of("Crawling"));
        NumberModifier standing = getModifierHandler(data.of("Standing"));
        NumberModifier walking = getModifierHandler(data.of("Walking"));
        NumberModifier riding = getModifierHandler(data.of("Riding"));
        NumberModifier sprinting = getModifierHandler(data.of("Sprinting"));
        NumberModifier dualWielding = getModifierHandler(data.of("Dual_Wielding"));
        NumberModifier swimming = getModifierHandler(data.of("Swimming"));
        NumberModifier inMidair = getModifierHandler(data.of("In_Midair"));
        NumberModifier gliding = getModifierHandler(data.of("Gliding"));

        if (always == null && zooming == null && sneaking == null && crawling == null
            && standing == null && walking == null && riding == null && sprinting == null
            && dualWielding == null && swimming == null && inMidair == null && gliding == null) {

            throw data.exception(null, "Tried to use Modify_Spread_When without any arguments");
        }
        return new ModifySpreadWhen(always, zooming, sneaking, crawling, standing, walking, riding, sprinting, dualWielding, swimming, inMidair, gliding);
    }

    private NumberModifier getModifierHandler(SerializeData.ConfigAccessor data) throws SerializerException {
        String value = data.get(Object.class).map(Object::toString).orElse(null);
        if (value == null)
            return null;
        try {
            boolean percentage = value.endsWith("%");
            double number = Double.parseDouble(value.split("%")[0]);
            if (!percentage) {
                number *= 0.01;
            }

            return new NumberModifier(number, percentage);
        } catch (NumberFormatException e) {
            throw SerializerException.builder()
                .locationRaw(data.getLocation())
                .addMessage("Remember that you can use percentages like '10%' to add 10% more recoil")
                .build();
        }
    }
}