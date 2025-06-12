package me.deecaad.weaponmechanics.wrappers;

import com.cjcrafter.foliascheduler.TaskImplementation;
import net.kyori.adventure.bossbar.BossBar;
import org.jetbrains.annotations.Nullable;

public class MessageHelper {

    private BossBar bossBar;
    private TaskImplementation<Void> bossBarTask;
    private TaskImplementation<Void> expTask;

    public @Nullable BossBar getBossBar() {
        return bossBar;
    }

    public void setBossBar(@Nullable BossBar bossBar) {
        this.bossBar = bossBar;
    }

    public @Nullable TaskImplementation<Void> getBossBarTask() {
        return bossBarTask;
    }

    public void setBossBarTask(@Nullable TaskImplementation<Void> bossBarTask) {
        this.bossBarTask = bossBarTask;
    }

    public @Nullable TaskImplementation<Void> getExpTask() {
        return expTask;
    }

    public void setExpTask(@Nullable TaskImplementation<Void> expTask) {
        this.expTask = expTask;
    }
}