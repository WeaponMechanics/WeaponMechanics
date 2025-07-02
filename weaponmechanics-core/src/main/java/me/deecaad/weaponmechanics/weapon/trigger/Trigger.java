package me.deecaad.weaponmechanics.weapon.trigger;

import me.deecaad.core.file.*;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.MainHand;
import org.jetbrains.annotations.NotNull;

@SearcherFilter(SearchMode.ON_DEMAND)
public class Trigger implements Serializer<Trigger> {

    private TriggerType mainhand;
    private TriggerType offhand;
    private Circumstance circumstance;
    private TriggerType dualWieldMainHand;
    private TriggerType dualWieldOffHand;

    /**
     * Default constructor for serializer
     */
    public Trigger() {
    }

    public Trigger(TriggerType mainhand, TriggerType offhand, Circumstance circumstance, TriggerType dualWieldMainHand, TriggerType dualWieldOffHand) {
        this.mainhand = mainhand;
        this.offhand = offhand;
        this.circumstance = circumstance;
        this.dualWieldMainHand = dualWieldMainHand;
        this.dualWieldOffHand = dualWieldOffHand;
    }

    /**
     * Checks if trigger is valid
     *
     * @param triggerType the trigger type
     * @param slot the slot used
     * @param entityWrapper the entity's wrapper from whom to check
     * @return true if trigger is valid
     */
    public boolean check(TriggerType triggerType, EquipmentSlot slot, EntityWrapper entityWrapper) {
        TriggerType typeCheck;
        boolean isDual = entityWrapper.isDualWielding();
        LivingEntity livingEntity = entityWrapper.getEntity();

        if (slot == EquipmentSlot.HAND) {
            if (isDual && dualWieldMainHand != null) {
                typeCheck = dualWieldMainHand;
                if (typeCheck.isRightOrLeft() && livingEntity.getType() == EntityType.PLAYER && ((Player) livingEntity).getMainHand() == MainHand.LEFT) {
                    // Invert if player has inverted main hand...
                    typeCheck = typeCheck == TriggerType.RIGHT_CLICK ? TriggerType.LEFT_CLICK : TriggerType.RIGHT_CLICK;
                }
            } else {
                typeCheck = mainhand;
            }
        } else {
            if (isDual && dualWieldOffHand != null) {
                typeCheck = dualWieldOffHand;
                if (typeCheck.isRightOrLeft() && livingEntity.getType() == EntityType.PLAYER && ((Player) livingEntity).getMainHand() == MainHand.LEFT) {
                    // Invert if player has inverted main hand...
                    typeCheck = typeCheck == TriggerType.RIGHT_CLICK ? TriggerType.LEFT_CLICK : TriggerType.RIGHT_CLICK;
                }
            } else {
                typeCheck = offhand;
            }
        }

        if (typeCheck == null || typeCheck != triggerType)
            return false;

        return checkCircumstances(entityWrapper);
    }

    /**
     * @param entityWrapper the entity's wrapper from whom to check
     * @return true if circumstances are valid
     */
    public boolean checkCircumstances(EntityWrapper entityWrapper) {
        return circumstance == null || !circumstance.deny(entityWrapper);
    }

    /**
     * @return the trigger for main hand
     */
    public TriggerType getMainhand() {
        return this.mainhand;
    }

    /**
     * @return the trigger for off hand
     */
    public TriggerType getOffhand() {
        return this.offhand;
    }

    public TriggerType getDualWieldMainHand() {
        return dualWieldMainHand;
    }

    public TriggerType getDualWieldOffHand() {
        return dualWieldOffHand;
    }

    @Override
    public String getKeyword() {
        return "Trigger";
    }

    @Override
    @NotNull public Trigger serialize(@NotNull SerializeData data) throws SerializerException {
        TriggerType main = data.of("Main_Hand").getEnum(TriggerType.class).orElse(null);
        TriggerType off = data.of("Off_Hand").getEnum(TriggerType.class).orElse(null);

        if (main == null && off == null) {
            throw data.exception(null, "At least one of Main_Hand or Off_Hand should be used");
        }

        TriggerType dualMain = data.of("Dual_Wield.Main_Hand").getEnum(TriggerType.class).orElse(null);
        TriggerType dualOff = data.of("Dual_Wield.Off_Hand").getEnum(TriggerType.class).orElse(null);

        if (isDisabled(main))
            throw data.exception("Main_Hand", "Tried to use trigger which is disabled in config.yml");
        if (isDisabled(off))
            throw data.exception("Off_Hand", "Tried to use trigger which is disabled in config.yml");
        if (isDisabled(dualMain))
            throw data.exception("Dual_Wield.Main_Hand", "Tried to use trigger which is disabled in config.yml");
        if (isDisabled(dualOff))
            throw data.exception("Dual_Wield.Off_Hand", "Tried to use trigger which is disabled in config.yml");

        Circumstance circumstance = data.of("Circumstance").serialize(Circumstance.class).orElse(null);

        // Check to make sure the gun denies swapping hands, otherwise this
        // won't work.
        if (dualMain == TriggerType.SWAP_HANDS || dualOff == TriggerType.SWAP_HANDS) {
            String weaponTitle = data.getKey().split("\\.")[0];

            if (data.getConfig().get(weaponTitle + ".Info.Cancel.Swap_Hands", false) instanceof Boolean bool && !bool) {
                throw data.exception(null, "When using 'SWAP_HANDS', make sure that '" + weaponTitle + ".Info.Cancel.Swap_Hands: true'",
                    "For value: " + dualMain + " & For value: " + dualOff);
            }
        }

        return new Trigger(main, off, circumstance, dualMain, dualOff);
    }

    private boolean isDisabled(TriggerType trigger) {
        if (trigger == null)
            return false;

        Configuration config = WeaponMechanics.getInstance().getConfiguration();
        return switch (trigger) {
            case START_SNEAK, END_SNEAK, DOUBLE_SNEAK ->
                config.getBoolean("Disabled_Trigger_Checks.Sneak");
            case START_SPRINT, END_SPRINT -> config.getBoolean("Disabled_Trigger_Checks.Sprint");
            case RIGHT_CLICK, LEFT_CLICK, MELEE ->
                config.getBoolean("Disabled_Trigger_Checks.Right_And_Left_Click");
            case DROP_ITEM -> config.getBoolean("Disabled_Trigger_Checks.Drop_Item");
            case JUMP -> config.getBoolean("Disabled_Trigger_Checks.Jump");
            case DOUBLE_JUMP -> config.getBoolean("Disabled_Trigger_Checks.Double_Jump");
            case START_SWIM, END_SWIM -> config.getBoolean("Disabled_Trigger_Checks.Swim");
            case START_GLIDE, END_GLIDE -> config.getBoolean("Disabled_Trigger_Checks.Glide");
            case SWAP_HANDS -> config.getBoolean("Disabled_Trigger_Checks.Swap_Hand_Items");
            case START_WALK, END_WALK, START_STAND, END_STAND ->
                config.getBoolean("Disabled_Trigger_Checks.Standing_And_Walking");
            case START_IN_MIDAIR, END_IN_MIDAIR ->
                config.getBoolean("Disabled_Trigger_Checks.In_Midair");
        };
    }
}
