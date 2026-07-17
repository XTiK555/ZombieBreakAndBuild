package com.tik.zbb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        ScheduledTask task = new ScheduledTask(action);
        long dueTick = currentTick + Math.max(1L, delayTicks);
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
