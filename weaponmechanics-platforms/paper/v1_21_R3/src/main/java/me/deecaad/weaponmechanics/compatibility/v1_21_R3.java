package me.deecaad.weaponmechanics.compatibility;

import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class v1_21_R3 implements IWeaponCompatibility {

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

    public v1_21_R3() {
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
            nms.setLastHurtByPlayer(((CraftPlayer) source).getHandle());
    }

    @Override
    public void setKiller(org.bukkit.entity.LivingEntity victim, Player killer) {
        ((CraftLivingEntity) victim).getHandle().lastHurtByMob = ((CraftPlayer) killer).getHandle();
    }
}