package me.deecaad.weaponmechanics.weapon.reload;

import com.cjcrafter.foliascheduler.EntitySchedulerImplementation;
import com.cjcrafter.foliascheduler.TaskImplementation;
import me.deecaad.weaponmechanics.WeaponMechanics;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public abstract class ChainTask {

    private final int delay;
    private ChainTask nextTask;

    public ChainTask(int delay) {
        this.delay = delay;
    }

    /**
     * @param nextTask the next task in the chain
     * @return the next task that was set
     */
    public ChainTask setNextTask(ChainTask nextTask) {
        this.nextTask = nextTask;
        return nextTask;
    }

    /**
     * @return the delay after this task should be executed
     */
    public int getDelay() {
        return this.delay;
    }

    public @Nullable CompletableFuture<TaskImplementation<Void>> startChain(EntitySchedulerImplementation scheduler) {
        TaskImplementation<Void> task = scheduler.runDelayed(this::run, delay);
        onSchedule(task);

        if (task == null) {
            WeaponMechanics.getInstance().getDebugger().warning("Tried to reload on an invalid entity? Check Entity#isValid");
            return null;
        }

        return task.asFuture().thenCompose(scheduledTask -> {
            if (nextTask == null)
                return CompletableFuture.completedFuture(null);
            return nextTask.startChain(scheduler);
        });
    }

    public boolean hasNext() {
        return this.nextTask != null;
    }

    /**
     * Does stuff when chain task is run
     */
    public abstract void run(TaskImplementation<Void> scheduledTask);

    /**
     * Setup this task right after its delay counter has started (e.g. store task id somewhere)
     */
    public abstract void onSchedule(TaskImplementation<Void> scheduledTask);
}
