package me.deecaad.weaponmechanics.weapon.melee;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.compatibility.HitBox;
import me.deecaad.core.file.Configuration;
import me.deecaad.core.file.IValidator;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.CastData;
import me.deecaad.core.mechanics.MechanicManager;
import me.deecaad.core.mechanics.Mechanics;
import me.deecaad.core.placeholder.PlaceholderData;
import me.deecaad.core.placeholder.PlaceholderMessage;
import me.deecaad.core.utils.NumberUtil;
import me.deecaad.core.utils.StringUtil;
import me.deecaad.core.utils.ray.EntityTraceResult;
import me.deecaad.core.utils.ray.RayTrace;
import me.deecaad.core.utils.ray.RayTraceResult;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.compatibility.IWeaponCompatibility;
import me.deecaad.weaponmechanics.compatibility.WeaponCompatibilityAPI;
import me.deecaad.weaponmechanics.weapon.WeaponHandler;
import me.deecaad.weaponmechanics.weapon.trigger.TriggerType;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponMeleeMissEvent;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import me.deecaad.weaponmechanics.wrappers.HandData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class MeleeHandler implements IValidator {

    private WeaponHandler weaponHandler;

    /**
     * Default constructor for validator
     */
    public MeleeHandler() {
    }

    public MeleeHandler(WeaponHandler weaponHandler) {
        this.weaponHandler = weaponHandler;
    }

    /**
     * Tries to use melee
     *
     * @param entityWrapper the entity who used trigger
     * @param weaponTitle the weapon title
     * @param weaponStack the weapon stack
     * @param slot the slot used on trigger
     * @param triggerType the trigger type trying to activate melee
     * @param dualWield whether this was dual wield
     * @return true if was able to melee
     */
    public boolean tryUse(EntityWrapper entityWrapper, String weaponTitle, ItemStack weaponStack, EquipmentSlot slot, TriggerType triggerType, boolean dualWield, @Nullable LivingEntity knownVictim) {
        if (triggerType != TriggerType.MELEE)
            return false;
        Configuration config = WeaponMechanics.getInstance().getWeaponConfigurations();
        if (!config.getBoolean(weaponTitle + ".Melee.Enable_Melee")) {

            // Change weapon title to match the attachment
            weaponTitle = config.getString(weaponTitle + ".Melee.Melee_Attachment");

            // Melee isn't used for this weapon nor the melee attachment is defined
            if (weaponTitle == null)
                return false;
        }

        HandData handData = entityWrapper.getMainHandData();

        int meleeHitDelay = config.getInt(weaponTitle + ".Melee.Melee_Hit_Delay");
        if (meleeHitDelay != 0 && !NumberUtil.hasMillisPassed(handData.getLastMeleeTime(), meleeHitDelay))
            return false;

        int meleeMissDelay = config.getInt(weaponTitle + ".Melee.Melee_Miss.Melee_Miss_Delay");
        if (meleeMissDelay != 0 && !NumberUtil.hasMillisPassed(handData.getLastMeleeMissTime(), meleeMissDelay))
            return false;

        double meleeRange = config.getDouble(weaponTitle + ".Melee.Melee_Range");
        LivingEntity shooter = entityWrapper.getEntity();
        Location eyeLocation = shooter.getEyeLocation();
        Vector direction = eyeLocation.getDirection();
        EntityTraceResult hit = getHit(shooter, eyeLocation, direction, meleeRange, knownVictim);

        if (hit != null) {
            boolean result = weaponHandler.getShootHandler().shootWithoutTrigger(entityWrapper, weaponTitle, weaponStack, slot, triggerType, dualWield);
            if (result) {
                weaponHandler.getHitHandler().handleMeleeHit(hit, shooter, direction, weaponTitle, weaponStack, slot);
            }
            return result;
        }

        // Handle permissions
        boolean hasPermission = weaponHandler.getInfoHandler().hasPermission(shooter, weaponTitle);
        if (!hasPermission) {
            if (shooter instanceof Player player) {
                String permissionMessage = WeaponMechanics.getInstance().getConfiguration().getString("Messages.Permissions.Use_Weapon", "<red>You do not have permission to use " + weaponTitle);
                PlaceholderMessage message = new PlaceholderMessage(StringUtil.colorAdventure(permissionMessage));
                Component component = message.replaceAndDeserialize(PlaceholderData.of(player, weaponStack, weaponTitle, slot));
                WeaponMechanics.getInstance().getAdventure().player(player).sendMessage(component);
            }
            return false;
        }

        boolean consumeOnMiss = config.getBoolean(weaponTitle + ".Melee.Melee_Miss.Consume_On_Miss");
        MechanicManager missMechanics = config.getObject(weaponTitle + ".Melee.Melee_Miss.Mechanics", MechanicManager.class);

        WeaponMeleeMissEvent event = new WeaponMeleeMissEvent(weaponTitle, weaponStack, shooter, slot, meleeMissDelay / 50, missMechanics, consumeOnMiss);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled())
            return false;

        // Handle miss
        if (event.isConsume()) {
            weaponHandler.getShootHandler().shootWithoutTrigger(entityWrapper, weaponTitle, weaponStack, slot, triggerType, dualWield);
        }

        if (event.getMechanics() != null) {
            event.getMechanics().use(new CastData(shooter, weaponTitle, weaponStack));
        }

        if (event.getMeleeMissDelay() != 0) {
            handData.setLastMeleeMissTime(System.currentTimeMillis());
        }
        return true;
    }

    private @Nullable EntityTraceResult getHit(LivingEntity shooter, Location eyeLocation, Vector direction, double range, @Nullable LivingEntity knownVictim) {

        Vector eyeLocationToVector = eyeLocation.toVector();

        if (knownVictim == null) {

            if (range <= 0) {
                return null;
            }

            RayTrace rayTrace = new RayTrace()
                .withEntityFilter(entity -> entity.getEntityId() == shooter.getEntityId() || entity.getPassengers().contains(shooter));
            List<RayTraceResult> hits = rayTrace.cast(eyeLocation.getWorld(), eyeLocationToVector, direction, range);

            if (hits == null)
                return null;

            RayTraceResult firstHit = hits.get(0);

            // If first hit isn't entity
            if (!(firstHit instanceof EntityTraceResult entityHit))
                return null;

            // If first entity hit isn't in range
            if (entityHit.getHitMin() > range)
                return null;

            return entityHit;
        }

        // Simply check where known victim was hit and whether it was in range
        HitBox entityBox = HitBox.getHitbox(knownVictim);
        if (entityBox == null)
            return null;

        RayTraceResult rayTraceResult = entityBox.rayTrace(eyeLocationToVector, direction);
        if (rayTraceResult == null || (range > 0 && rayTraceResult.getHitMin() > range))
            return null; // Didn't hit in range

        return (EntityTraceResult) rayTraceResult;
    }

    @Override
    public String getKeyword() {
        return "Melee";
    }

    public List<String> getAllowedPaths() {
        return Collections.singletonList(".Melee");
    }

    @Override
    public void validate(Configuration configuration, SerializeData data) throws SerializerException {
        boolean enableMelee = data.of("Enable_Melee").getBool().orElse(false);
        String meleeAttachment = data.of("Melee_Attachment").get(String.class).orElse(null);
        if (!enableMelee && meleeAttachment == null) {
            throw data.exception(null, "You must use either 'Enable_Melee: true' or 'Melee_Attachment: <weapon>'",
                "You cannot use the 'Melee' key without 1 of those 2 options");
        }

        int meleeHitDelay = data.of("Melee_Hit_Delay").assertRange(0, null).getInt().orElse(0);
        if (meleeHitDelay != 0) {
            // Convert to millis
            configuration.set(data.getKey() + ".Melee_Hit_Delay", meleeHitDelay * 50);
        }

        int meleeMissDelay = data.of("Melee_Miss.Melee_Miss_Delay").assertRange(0, null).getInt().orElse(0);
        if (meleeMissDelay != 0) {
            // Convert to millis
            configuration.set(data.getKey() + ".Melee_Miss.Melee_Miss_Delay", meleeMissDelay * 50);
        }
    }
}
