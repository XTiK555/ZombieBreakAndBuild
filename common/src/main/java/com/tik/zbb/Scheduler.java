package com.tik.zbb;

import java.util.*;

public final class Scheduler
{
    private final Map<Long, List<ScheduledTask>> tasksByDueTick = new HashMap<>();
    private long currentTick;

    public void tick()
    {
        List<ScheduledTask> dueTasks = tasksByDueTick.remove(++currentTick);
        if (dueTasks == null)
        {
            return;
        }

        for (ScheduledTask task : dueTasks)
        {
            if (!task.cancelled)
            {
                try
                {
                    task.action.run();
                }
                catch (Exception e)
                {
                    Constants.LOG.error("Scheduled task {} failed at tick {}", task.action, currentTick, e);
                }
            }
        }
    }

    public ScheduledTask schedule(Runnable action, int delayTicks)
    {
        Objects.requireNonNull(action, "action");
        long normalizedDelayTicks = delayTicks <= 0 ? 1 : Math.addExact(delayTicks, 1);
        long dueTick = Math.addExact(currentTick, normalizedDelayTicks);

        ScheduledTask task = new ScheduledTask(action);
        tasksByDueTick.computeIfAbsent(dueTick, ignored -> new ArrayList<>()).add(task);

        return task;
    }

    public void clear()
    {
        tasksByDueTick.clear();
        currentTick = 0;
    }

    public static class ScheduledTask
    {
        private final Runnable action;
        private boolean cancelled;

        private ScheduledTask(Runnable action)
        {
            this.action = action;
        }

        public void cancel()
        {
            this.cancelled = true;
        }

        public boolean isCancelled()
        {
            return cancelled;
        }
    }
}
