package me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile;

import me.deecaad.core.compatibility.CompatibilityAPI;
import me.deecaad.core.compatibility.entity.FakeEntity;
import me.deecaad.core.file.Configuration;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.serializers.VectorSerializer;
import me.deecaad.core.mechanics.CastData;
import me.deecaad.core.mechanics.MechanicManager;
import me.deecaad.core.mechanics.Mechanics;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.explode.Explosion;
import me.deecaad.weaponmechanics.weapon.explode.ExplosionTrigger;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class Projectile implements Serializer<Projectile> {

    private ProjectileSettings projectileSettings;
    private Sticky sticky;
    private Through through;
    private Bouncy bouncy;
    private MechanicManager mechanics;

    /**
     * Empty constructor to be used as serializer
     */
    public Projectile() {
    }

    public Projectile(ProjectileSettings projectileSettings, Sticky sticky, Through through, Bouncy bouncy, MechanicManager mechanics) {
        this.projectileSettings = projectileSettings;
        this.sticky = sticky;
        this.through = through;
        this.bouncy = bouncy;
        this.mechanics = mechanics;
    }

    /**
     * Shoots this projectile with given location and motion
     *
     * @param shooter the living entity used to shoot
     * @param location the location from where to shoot
     * @param motion the motion of projectile
     * @param weaponStack the weapon stack used to shoot
     * @param weaponTitle the weapon title used to shoot
     */
    public WeaponProjectile shoot(LivingEntity shooter, Location location, Vector motion, ItemStack weaponStack, String weaponTitle, EquipmentSlot hand) {
        return shoot(create(shooter, location, motion, weaponStack, weaponTitle, hand), location);
    }

    /**
     * Shoots created projectile object
     *
     * @param projectile the created projectile object
     * @param location the location containing pitch and yaw
     */
    public WeaponProjectile shoot(WeaponProjectile projectile, Location location) {
        if (mechanics != null) {
            CastData cast = new CastData(projectile.getShooter(), projectile.getWeaponTitle(), projectile.getWeaponStack());
            cast.setTargetLocation(() -> projectile.getLocation().toLocation(projectile.getWorld()));
            mechanics.use(cast);
        }

        EntityType type = projectileSettings.getProjectileDisguise();
        if (type != null) {

            FakeEntity fakeEntity;
            Object data = projectileSettings.getDisguiseData();
            if (type == EntityType.ARMOR_STAND && data != null) {
                // Armor stand height * eye height multiplier
                // 1.975 * 0.85 = 1.67875
                Location offset = new Location(location.getWorld(), 0, -1.67875, 0);

                // Add the first offset before actually spawning
                location.add(offset);

                fakeEntity = CompatibilityAPI.getEntityCompatibility().generateFakeEntity(location, type, data);

                fakeEntity.setEquipment(EquipmentSlot.HEAD, (ItemStack) data);
                fakeEntity.setInvisible(true);

                // Set the offset for new packets
                fakeEntity.setOffset(offset);
            } else {
                fakeEntity = CompatibilityAPI.getEntityCompatibility().generateFakeEntity(location, type, data);
            }

            projectile.spawnDisguise(fakeEntity);
        }

        // Handle explosions
        Explosion explosion = WeaponMechanics.getInstance().getWeaponConfigurations().getObject(projectile.getWeaponTitle() + ".Explosion", Explosion.class);
        if (explosion != null)
            explosion.handleExplosion(projectile.getShooter(), projectile, ExplosionTrigger.SPAWN);

        WeaponMechanics.getInstance().getProjectileSpawner().spawn(projectile);
        return projectile;
    }

    /**
     * Creates this projectile with given location and motion without shooting it
     *
     * @param shooter the living entity used to shoot
     * @param location the location from where to shoot
     * @param motion the motion of projectile
     * @param weaponStack the weapon stack used to shoot
     * @param weaponTitle the weapon title used to shoot
     */
    public WeaponProjectile create(LivingEntity shooter, Location location, Vector motion, ItemStack weaponStack, String weaponTitle, EquipmentSlot hand) {
        return new WeaponProjectile(projectileSettings, shooter, location, motion, weaponStack, weaponTitle, hand, sticky, through, bouncy);
    }

    @Override
    public @Nullable String getKeyword() {
        return "Projectile";
    }

    @Override
    public boolean canUsePathTo() {
        return false;
    }

    @Override
    public @NotNull Projectile serialize(@NotNull SerializeData data) throws SerializerException {
        // Most people will probably use default projectiles
        if (data.of().is(String.class)) {
            String projectileTitle = data.of().assertExists().get(String.class).get();
            Configuration config = WeaponMechanics.getInstance().getProjectileConfigurations();
            Projectile projectile = config.getObject(projectileTitle, Projectile.class);
            if (projectile != null)
                return projectile;

            List<String> projectiles = config.entries().stream()
                    .filter(entry -> entry.getValue() instanceof Projectile)
                    .map(Map.Entry::getKey)
                    .toList();

            throw SerializerException.builder()
                    .location(data.getFile(), data.getKey())
                    .buildInvalidOption(projectileTitle, projectiles);
        }

        ProjectileSettings projectileSettings = data.of("Projectile_Settings").assertExists().serialize(ProjectileSettings.class).get();

        Sticky sticky = data.of("Sticky").serialize(Sticky.class).orElse(null);
        Through through = data.of("Through").serialize(Through.class).orElse(null);
        Bouncy bouncy = data.of("Bouncy").serialize(Bouncy.class).orElse(null);
        MechanicManager mechanics = data.of("Mechanics").serialize(MechanicManager.class).orElse(null);
        return new Projectile(projectileSettings, sticky, through, bouncy, mechanics);
    }
}