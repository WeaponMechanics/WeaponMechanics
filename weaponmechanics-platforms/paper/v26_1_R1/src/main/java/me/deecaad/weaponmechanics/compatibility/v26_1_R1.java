package me.deecaad.weaponmechanics.compatibility;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.wrappers.ZoomData;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class v26_1_R1 implements IWeaponCompatibility {

    private final Set<Relative> RELATIVE_FLAGS = new HashSet<>(Arrays.asList(
        Relative.X,
        Relative.Y,
        Relative.Z,
        Relative.DELTA_X,
        Relative.DELTA_Y,
        Relative.DELTA_Z,
        Relative.X_ROT,
        Relative.Y_ROT));

    private final Set<Relative> ABSOLUTE_FLAGS = new HashSet<>(Arrays.asList(
        Relative.X,
        Relative.Y,
        Relative.Z,
        Relative.DELTA_X,
        Relative.DELTA_Y,
        Relative.DELTA_Z));

    public v26_1_R1() {
    }

    @Override
    public void modifyCameraRotation(Player player, float yaw, float pitch, boolean absolute) {
        pitch *= -1;
        var movement = new PositionMoveRotation(Vec3.ZERO, Vec3.ZERO, yaw, pitch);
        var packet = ClientboundPlayerPositionPacket.of(0, movement, absolute ? ABSOLUTE_FLAGS : RELATIVE_FLAGS);
        ((CraftPlayer) player).getHandle().connection.send(packet);
    }

    @Override
    public void logDamage(org.bukkit.entity.LivingEntity victim, org.bukkit.entity.LivingEntity source, double health, double damage, boolean isMelee) {
        DamageSources factory = ((CraftLivingEntity) source).getHandle().damageSources();
        DamageSource damageSource;

        if (isMelee) {
            if (source instanceof CraftPlayer player) {
                damageSource = factory.playerAttack(player.getHandle());
            } else {
                damageSource = factory.mobAttack(((CraftLivingEntity) source).getHandle());
            }
        } else {
            damageSource = factory.thrown(null, ((CraftLivingEntity) source).getHandle());
        }

        LivingEntity nms = ((CraftLivingEntity) victim).getHandle();
        nms.combatTracker.recordDamage(damageSource, (float) health);
        nms.setLastHurtByMob(((CraftLivingEntity) source).getHandle());
        if (source instanceof Player)
            nms.setLastHurtByPlayer(((CraftPlayer) source).getHandle(), 100);
    }

    @Override
    public void setKiller(org.bukkit.entity.LivingEntity victim, Player killer) {
        ((CraftLivingEntity) victim).getHandle().setLastHurtByPlayer(((CraftPlayer) killer).getHandle(), 100);
    }

    @Override
    public TaskImplementation<Void> playAdsSettleAnimation(Player player, int durationTicks) {
        ((CraftPlayer) player).getHandle().resetAttackStrengthTicker();

        AttributeInstance attribute = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attribute != null) {
            removeAdsSpeedModifier(attribute);

            double requiredSpeed = 20.0 / Math.max(1, durationTicks);
            double addValue = requiredSpeed - attribute.getValue();
            attribute.addModifier(new AttributeModifier(ZoomData.ADS_SPEED_MODIFIER_KEY, addValue, AttributeModifier.Operation.ADD_NUMBER));
        }

        return WeaponMechanics.getInstance().getFoliaScheduler().entity(player).runDelayed(() -> {
            AttributeInstance current = player.getAttribute(Attribute.ATTACK_SPEED);
            if (current != null) {
                removeAdsSpeedModifier(current);
            }
        }, durationTicks);
    }

    private void removeAdsSpeedModifier(AttributeInstance attribute) {
        for (AttributeModifier modifier : new ArrayList<>(attribute.getModifiers())) {
            if (ZoomData.ADS_SPEED_MODIFIER_KEY.equals(modifier.getKey())) {
                attribute.removeModifier(modifier);
                break;
            }
        }
    }
}
