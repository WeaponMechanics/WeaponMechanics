package me.deecaad.weaponmechanics.weapon.shoot.recoil;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Holds data for a recoil system with advanced AAA-like parameters.
 */
public class RecoilProfile implements Serializer<RecoilProfile> {

    private float recoilMeanX;
    private float recoilMeanY;
    private float recoilVarianceX;
    private float recoilVarianceY;
    private float recoilSpeed;
    private float damping;
    private float dampingRecovery;
    private float smoothingFactor;
    private float maxRecoilAccum;

    /**
     * Default constructor for serializer.
     */
    public RecoilProfile() {
    }

    public RecoilProfile(
        float recoilMeanX,
        float recoilMeanY,
        float recoilVarianceX,
        float recoilVarianceY,
        float recoilSpeed,
        float damping,
        float dampingRecovery,
        float smoothingFactor,
        float maxRecoilAccum) {
        this.recoilMeanX = recoilMeanX;
        this.recoilMeanY = recoilMeanY;
        this.recoilVarianceX = recoilVarianceX;
        this.recoilVarianceY = recoilVarianceY;
        this.recoilSpeed = recoilSpeed;
        this.damping = damping;
        this.dampingRecovery = dampingRecovery;
        this.smoothingFactor = smoothingFactor;
        this.maxRecoilAccum = maxRecoilAccum;
    }

    public float getRecoilMeanX() {
        return recoilMeanX;
    }

    public float getRecoilMeanY() {
        return recoilMeanY;
    }

    public float getRecoilVarianceX() {
        return recoilVarianceX;
    }

    public float getRecoilVarianceY() {
        return recoilVarianceY;
    }

    public float getRecoilSpeed() {
        return recoilSpeed;
    }

    public float getDamping() {
        return damping;
    }

    public float getDampingRecovery() {
        return dampingRecovery;
    }

    public float getSmoothingFactor() {
        return smoothingFactor;
    }

    public float getMaxRecoilAccum() {
        return maxRecoilAccum;
    }

    @Override
    public @Nullable String getKeyword() {
        return "Recoil";
    }

    @Override
    public @NotNull RecoilProfile serialize(@NotNull SerializeData data) throws SerializerException {
        Set<String> oldKeys = Set.of("Push_Time", "Recover_Time", "Horizontal", "Vertical", "Recoil_Pattern", "Modify_Recoil_When");
        for (String key : oldKeys) {
            if (data.has(key)) {
                throw SerializerException.builder()
                    .locationRaw(data.of(key).getLocation())
                    .addMessage("Old configs detected. In 4.0.0, WeaponMechanics' Recoil system has been rewritten.")
                    .addMessage("Please visit: https://cjcrafter.gitbook.io/weaponmechanics/weapon-modules/shoot/recoil")
                    .build();
            }
        }

        float recoilMeanX = (float) data.of("Mean_X").getDouble().orElse(0.0);
        float recoilMeanY = (float) data.of("Mean_Y").getDouble().orElse(0.0);
        float recoilVarianceX = (float) data.of("Variance_X").assertRange(0.0, null).getDouble().orElse(0.0);
        float recoilVarianceY = (float) data.of("Variance_Y").assertRange(0.0, null).getDouble().orElse(0.0);
        float recoilSpeed = (float) data.of("Speed").getDouble().orElse(1.0);
        float damping = (float) data.of("Damping").getDouble().orElse(0.0);
        float dampingRecovery = (float) data.of("Damping_Recovery").assertRange(0.0, 1.0).getDouble().orElse(0.0);
        float smoothingFactor = (float) data.of("Smoothing").assertRange(0.0, 1.0).getDouble().orElse(0.5);
        float maxRecoilAccum = (float) data.of("Max_Accumulation").getDouble().orElse(360);

        return new RecoilProfile(
            recoilMeanX,
            recoilMeanY,
            recoilVarianceX,
            recoilVarianceY,
            recoilSpeed,
            damping,
            dampingRecovery,
            smoothingFactor,
            maxRecoilAccum);
    }
}
