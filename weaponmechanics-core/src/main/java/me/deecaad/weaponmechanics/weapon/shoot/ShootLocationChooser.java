package me.deecaad.weaponmechanics.weapon.shoot;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.serializers.VectorProvider;
import me.deecaad.core.file.serializers.VectorSerializer;
import me.deecaad.core.utils.EntityTransform;
import me.deecaad.core.utils.Quaternion;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MainHand;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vivecraft.api.VRAPI;
import org.vivecraft.api.data.VRBodyPartData;
import org.vivecraft.api.data.VRPose;

import java.util.List;

/**
 * People can choose where the bullet is offset from depending on VR/Scope.
 */
public class ShootLocationChooser implements Serializer<ShootLocationChooser> {

    private static final Vector UP = new Vector(0, 1, 0);

    private @NotNull ShootLocation def; // default
    private @Nullable ShootLocation vr; // When in VR
    private @Nullable ShootLocation scope; // When scoping

    /**
     * Default constructor for serializer
     */
    public ShootLocationChooser() {
    }

    public ShootLocationChooser(@NotNull ShootLocation def, @Nullable ShootLocation vr, @Nullable ShootLocation scope) {
        this.def = def;
        this.vr = vr;
        this.scope = scope;
    }

    public @NotNull ShootLocation getDefault() {
        return def;
    }

    public @Nullable ShootLocation getVR() {
        return vr;
    }

    public @Nullable ShootLocation getScope() {
        return scope;
    }

    public @NotNull Location offset(@NotNull EntityWrapper wrapper, boolean isMainHand) {
        LivingEntity shooter = wrapper.getEntity();
        boolean isRightHand = isMainHand;
        if (shooter instanceof HumanEntity human) {
            isRightHand = isMainHand == (human.getMainHand() == MainHand.RIGHT);
        }

        // source=where the bullet is shot from, direction=where the bullet is shot to
        Location source = null;

        // Check if this entity is a Vivecraft player
        if (shooter instanceof Player player && Bukkit.getPluginManager().getPlugin("Vivecraft_Spigot_Extensions") != null) {
            VRPose pose = VRAPI.instance().getVRPose(player);
            if (pose != null) {
                VRBodyPartData controller = isMainHand ? pose.getMainHand() : pose.getOffHand();
                source = controller.getPos().toLocation(player.getWorld());
                Vector forward = controller.getDir();
                source.setDirection(forward);

                if (vr != null) {
                    Quaternion localRotation = Quaternion.lookAt(forward, UP);
                    vr.offset(isMainHand, source, localRotation);
                    return source;
                }
            }
        }

        // Update source and direction if not already set
        if (source == null) {
            source = shooter.getEyeLocation();
        }

        // Scoping
        Quaternion localRotation = new EntityTransform(shooter).getLocalRotation();
        if (wrapper.getHandData(isMainHand).getZoomData().isZooming() && scope != null) {
            scope.offset(isRightHand, source, localRotation);
            return source;
        }

        // Default
        def.offset(isRightHand, source, localRotation);
        return source;
    }

    @Override
    public @Nullable String getKeyword() {
        return "Offsets";
    }

    @Override
    public @Nullable List<String> getParentKeywords() {
        return List.of("Shoot");
    }

    @Override
    public @NotNull ShootLocationChooser serialize(@NotNull SerializeData data) throws SerializerException {
        ShootLocation def = data.of().serialize(ShootLocation.class).orElse(null);
        ShootLocation vr = data.of("Vive").serialize(ShootLocation.class).orElse(null);
        ShootLocation scope = data.of("Scope").serialize(ShootLocation.class).orElse(null);

        if (def == null) {
            throw data.exception(null, "Somehow, the default shoot location in the ShootLocationChooser is null",
                "Your config is likely malformed. Try to format it correctly.");
        }

        return new ShootLocationChooser(def, vr, scope);
    }

    /**
     * Gets the controller position of the given entity. If the entity is not a Vivecraft player, or the
     * Vivecraft plugin is not installed, this will return <code>null</code>.
     *
     * @param entity The entity to get the controller position from.
     * @param isMainHand <code>true</code> if the main hand is being used.
     * @return The controller position, or <code>null</code>
     */
    public static @Nullable Location getControllerPos(@NotNull LivingEntity entity, boolean isMainHand) {
        if (entity instanceof Player player && Bukkit.getPluginManager().getPlugin("Vivecraft_Spigot_Extensions") != null) {
            VRPose pose = VRAPI.instance().getVRPose(player);
            if (pose != null) {
                VRBodyPartData controller = isMainHand ? pose.getMainHand() : pose.getOffHand();
                Location location = controller.getPos().toLocation(player.getWorld());
                location.setDirection(controller.getDir());
                return location;
            }
        }
        return null;
    }

    /**
     * Keeps track of the left and right shoot locations, since the position should probably be
     * different for each arm.
     */
    public static class ShootLocation implements Serializer<ShootLocation> {

        private VectorProvider left;
        private VectorProvider right;

        /**
         * Default constructor for serializer.
         */
        public ShootLocation() {
        }

        public ShootLocation(@NotNull VectorProvider left, @NotNull VectorProvider right) {
            this.left = left;
            this.right = right;
        }

        public void offset(boolean isRightHand, @NotNull Location source, @Nullable Quaternion localRotation) {
            if (isRightHand) {
                source.add(right.provide(localRotation));
            } else {
                source.add(left.provide(localRotation));
            }
        }

        public @NotNull ShootLocation serialize(@NotNull SerializeData data) throws SerializerException {
            VectorProvider left = data.of("Left_Hand").assertExists().serialize(VectorSerializer.class).get();
            VectorProvider right = data.of("Right_Hand").assertExists().serialize(VectorSerializer.class).get();
            return new ShootLocation(left, right);
        }
    }
}
