package me.deecaad.weaponmechanics.weapon.explode.regeneration;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import org.jetbrains.annotations.NotNull;

public class RegenerationData implements Serializer<RegenerationData> {

    private int ticksBeforeStart;
    private int maxBlocksPerUpdate;
    private int interval;

    /**
     * Empty constructor for serializer
     */
    public RegenerationData() {
    }

    public RegenerationData(int ticksBeforeStart, int maxBlocksPerUpdate, int interval) {
        this.ticksBeforeStart = ticksBeforeStart;
        this.maxBlocksPerUpdate = maxBlocksPerUpdate;
        this.interval = interval;
    }

    public int getTicksBeforeStart() {
        return ticksBeforeStart;
    }

    public void setTicksBeforeStart(int ticksBeforeStart) {
        this.ticksBeforeStart = ticksBeforeStart;
    }

    public int getMaxBlocksPerUpdate() {
        return maxBlocksPerUpdate;
    }

    public void setMaxBlocksPerUpdate(int maxBlocksPerUpdate) {
        this.maxBlocksPerUpdate = maxBlocksPerUpdate;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    @Override
    @NotNull public RegenerationData serialize(@NotNull SerializeData data) throws SerializerException {
        int ticksBeforeStart = data.of("Ticks_Before_Start").assertRange(1, null).getInt().orElse(1200); // 1 minute, in ticks
        int maxBlocksPerUpdate = data.of("Max_Blocks_Per_Update").assertRange(1, null).getInt().orElse(1);
        int interval = data.of("Ticks_Between_Updates").assertRange(1, null).getInt().orElse(1);

        return new RegenerationData(ticksBeforeStart, maxBlocksPerUpdate, interval);
    }
}
