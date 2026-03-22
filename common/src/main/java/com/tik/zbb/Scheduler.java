package com.tik.zbb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Scheduler
{
    private final List<ScheduledTask> tasks = new ArrayList<>();
    private final List<ScheduledTask> pendingTasks = new ArrayList<>();
    private boolean ticking = false;

    public void tick()
    {
        ticking = true;

        Iterator<ScheduledTask> iterator = tasks.iterator();

        while (iterator.hasNext())
        {
            ScheduledTask task = iterator.next();

            if (task.cancelled)
            {
                iterator.remove();
                continue;
            }

            task.ticksLeft--;

            if (task.ticksLeft <= 0)
            {
                try
                {
                    task.action.run();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                iterator.remove();
            }
        }

        ticking = false;

        if (!pendingTasks.isEmpty())
        {
            tasks.addAll(pendingTasks);
            pendingTasks.clear();
        }
    }

    public ScheduledTask schedule(Runnable action, int delayTicks)
    {
        ScheduledTask task = new ScheduledTask(action, delayTicks);

        if (ticking)
            pendingTasks.add(task);
        else
            tasks.add(task);

        return task;
    }

    public void clear()
    {
        tasks.clear();
        pendingTasks.clear();
    }

    public static class ScheduledTask
    {
        private final Runnable action;
        private int ticksLeft;
        private boolean cancelled;

        private ScheduledTask(Runnable action, int delayTicks)
        {
            this.action = action;
            this.ticksLeft = delayTicks;
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
