package me.deecaad.weaponmechanics.weapon.shoot.spread;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.utils.RandomUtil;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class Spread implements Serializer<Spread> {

    private SpreadImage spreadImage;

    private double baseSpread;
    private ModifySpreadWhen modifySpreadWhen;
    private ChangingSpread changingSpread;

    /**
     * Default constructor for serializer
     */
    public Spread() {
    }

    public Spread(SpreadImage spreadImage) {
        this.spreadImage = spreadImage;
    }

    public Spread(double baseSpread, ModifySpreadWhen modifySpreadWhen, ChangingSpread changingSpread) {
        this(null, baseSpread, modifySpreadWhen, changingSpread);
    }

    public Spread(SpreadImage spreadImage, double baseSpread, ModifySpreadWhen modifySpreadWhen, ChangingSpread changingSpread) {
        this.spreadImage = spreadImage;
        this.baseSpread = baseSpread;
        this.modifySpreadWhen = modifySpreadWhen;
        this.changingSpread = changingSpread;
    }

    public SpreadImage getSpreadImage() {
        return spreadImage;
    }

    public double getBaseSpread() {
        return baseSpread;
    }

    public ModifySpreadWhen getModifySpreadWhen() {
        return modifySpreadWhen;
    }

    public ChangingSpread getChangingSpread() {
        return changingSpread;
    }

    /**
     * Returns normalized spread direction
     *
     * @param entityWrapper the entity involved
     * @param shootLocation the shoot location
     * @param mainHand whether or not main hand was used
     * @param updateSpreadChange whether or not to allow updating current spread change
     * @return the normalized spread direction
     */
    public Vector getNormalizedSpreadDirection(EntityWrapper entityWrapper, Location shootLocation, boolean mainHand, boolean updateSpreadChange) {
        return getNormalizedSpreadDirection(entityWrapper, shootLocation, mainHand, updateSpreadChange, baseSpread);
    }

    /**
     * Returns normalized spread direction
     *
     * @param entityWrapper the entity involved
     * @param shootLocation the shoot location
     * @param mainHand whether or not main hand was used
     * @param updateSpreadChange whether or not to allow updating current spread change
     * @param spread the spread
     * @return the normalized spread direction
     */
    public Vector getNormalizedSpreadDirection(EntityWrapper entityWrapper, Location shootLocation, boolean mainHand, boolean updateSpreadChange, double spread) {
        double yaw = Math.toRadians(shootLocation.getYaw()), pitch = Math.toRadians(shootLocation.getPitch());
        if (spreadImage != null) {
            Point point = spreadImage.getLocation();
            yaw += point.getYaw();
            pitch += point.getPitch();
        }

        if (modifySpreadWhen != null)
            spread = modifySpreadWhen.applyChanges(entityWrapper, spread);
        if (changingSpread != null)
            spread = changingSpread.applyChanges(entityWrapper, spread, mainHand, updateSpreadChange);

        return getNormalizedSpreadDirection(yaw, pitch, spread);
    }

    /**
     * Used to get random normalized spread direction
     *
     * @param yaw the yaw of direction, as radians
     * @param pitch the pitch of direction, as radians
     * @param spread the spread
     * @return the randomized direction based on given params as normalized vector
     */
    private Vector getNormalizedSpreadDirection(double yaw, double pitch, double spread) {
        // negative spread probably means the user subtracted too much... Max
        // out to 0 to avoid error
        spread = Math.max(spread, 0.0);

        // Create random numbers for horizontal and vertical spread
        double randomX = RandomUtil.range(-spread, spread);
        double randomY = RandomUtil.range(-spread, spread);
        double randomZ = RandomUtil.range(-spread, spread);

        double xz = Math.cos(pitch);

        // Last calculate the direction and add randomness to it
        // Then normalize it.
        return new Vector(-xz * Math.sin(yaw) + randomX,
            -Math.sin(pitch) + randomY,
            xz * Math.cos(yaw) + randomZ).normalize();
    }

    @Override
    public String getKeyword() {
        return "Spread";
    }

    @Override
    @NotNull public Spread serialize(@NotNull SerializeData data) throws SerializerException {
        SpreadImage spreadImage = data.of("Spread_Image").serialize(SpreadImage.class).orElse(null);

        double baseSpread = data.of("Base_Spread").assertRange(0.0, null).getDouble().orElse(0.0);
        baseSpread /= 100.0;

        ModifySpreadWhen modifySpreadWhen = (ModifySpreadWhen) data.of("Modify_Spread_When").serialize(new ModifySpreadWhen()).orElse(null);
        ChangingSpread changingSpread = data.of("Changing_Spread").serialize(ChangingSpread.class).orElse(null);
        return new Spread(spreadImage, baseSpread, modifySpreadWhen, changingSpread);
    }
}
