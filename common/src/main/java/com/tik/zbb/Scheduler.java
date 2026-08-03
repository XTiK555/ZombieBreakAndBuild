package com.tik.zbb;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.Objects;

public final class Scheduler
{
    private final Long2ObjectOpenHashMap<TaskBucket> tasksByDueTick = new Long2ObjectOpenHashMap<>();

    private TaskBucket activeBucket;
    private long currentTick;
    private boolean ticking;

    
    public void tick()
    {
        if (ticking) throw new IllegalStateException("Scheduler.tick() cannot be called recursively");

        ticking = true;

        try
        {
            currentTick++;

            TaskBucket bucket = tasksByDueTick.remove(currentTick);
            if (bucket == null) return;

            activeBucket = bucket;

            ScheduledTask task;

            while ((task = bucket.pollFirst()) != null)
            {
                if (task.state != ScheduledTask.PENDING) continue;

                task.state = ScheduledTask.RUNNING;
                Runnable action = task.action;

                try
                {
                    action.run();
                }
                catch (Exception exception)
                {
                    Constants.LOG.error("Scheduled task {} failed at scheduler tick {}", action, currentTick, exception);
                }
                finally
                {
                    task.action = null;
                    task.owner = null;
                    task.state = ScheduledTask.COMPLETED;
                }
            }
        }
        finally
        {
            if (activeBucket != null && !activeBucket.isEmpty())
            {
                cancelBucket(activeBucket);
            }

            activeBucket = null;
            ticking = false;
        }
    }

    public ScheduledTask schedule(Runnable action, long delayTicks)
    {
        Objects.requireNonNull(action, "action");

        if (delayTicks < 0) throw new IllegalArgumentException("delayTicks must be greater than or equal to 0");

        long normalizedDelay = Math.max(1L, delayTicks);
        long dueTick = Math.addExact(currentTick, normalizedDelay);
        ScheduledTask task = new ScheduledTask(this, action, dueTick);
        TaskBucket bucket = tasksByDueTick.get(dueTick);

        if (bucket == null)
        {
            bucket = new TaskBucket();
            tasksByDueTick.put(dueTick, bucket);
        }

        bucket.addLast(task);

        return task;
    }

    public ScheduledTask scheduleNextTick(Runnable action)
    {
        return schedule(action, 1);
    }

    public void clear()
    {
        if (activeBucket != null)
        {
            cancelBucket(activeBucket);
        }

        for (TaskBucket bucket : tasksByDueTick.values())
        {
            cancelBucket(bucket);
        }

        tasksByDueTick.clear();
    }

    public long getCurrentTick()
    {
        return currentTick;
    }

    public boolean isEmpty()
    {
        return tasksByDueTick.isEmpty() && (activeBucket == null || activeBucket.isEmpty());
    }

    private boolean cancelTask(ScheduledTask task)
    {
        if (task.owner != this || task.state != ScheduledTask.PENDING) return false;

        task.state = ScheduledTask.CANCELLED;
        task.action = null;
        task.owner = null;

        TaskBucket bucket = task.bucket;

        if (bucket != null)
        {
            bucket.unlink(task);

            if (bucket.isEmpty() && tasksByDueTick.get(task.dueTick) == bucket)
            {
                tasksByDueTick.remove(task.dueTick);
            }
        }

        return true;
    }

    private static void cancelBucket(TaskBucket bucket)
    {
        ScheduledTask task;

        while ((task = bucket.pollFirst()) != null)
        {
            if (task.state == ScheduledTask.PENDING)
            {
                task.state = ScheduledTask.CANCELLED;
                task.action = null;
                task.owner = null;
            }
        }
    }

    public static final class ScheduledTask
    {
        private static final byte PENDING = 0;
        private static final byte RUNNING = 1;
        private static final byte COMPLETED = 2;
        private static final byte CANCELLED = 3;

        private Scheduler owner;
        private Runnable action;

        private final long dueTick;

        private byte state = PENDING;

        private TaskBucket bucket;
        private ScheduledTask previous;
        private ScheduledTask next;

        private ScheduledTask(Scheduler owner, Runnable action, long dueTick)
        {
            this.owner = owner;
            this.action = action;
            this.dueTick = dueTick;
        }

        public boolean cancel()
        {
            Scheduler scheduler = owner;

            return scheduler != null && scheduler.cancelTask(this);
        }

        public boolean isPending()
        {
            return state == PENDING;
        }

        public boolean isRunning()
        {
            return state == RUNNING;
        }

        public boolean isCompleted()
        {
            return state == COMPLETED;
        }

        public boolean isCancelled()
        {
            return state == CANCELLED;
        }

        public boolean isDone()
        {
            return state == COMPLETED || state == CANCELLED;
        }

        public long getDueTick()
        {
            return dueTick;
        }
    }

    private static final class TaskBucket
    {
        private ScheduledTask first;
        private ScheduledTask last;

        private void addLast(ScheduledTask task)
        {
            task.bucket = this;
            task.previous = last;

            if (last == null)
            {
                first = task;
            }
            else
            {
                last.next = task;
            }

            last = task;
        }

        private ScheduledTask pollFirst()
        {
            ScheduledTask task = first;

            if (task != null)
            {
                unlink(task);
            }

            return task;
        }

        private void unlink(ScheduledTask task)
        {
            if (task.bucket != this) return;

            ScheduledTask previous = task.previous;
            ScheduledTask next = task.next;

            if (previous == null)
            {
                first = next;
            }
            else
            {
                previous.next = next;
            }

            if (next == null)
            {
                last = previous;
            }
            else
            {
                next.previous = previous;
            }

            task.bucket = null;
            task.previous = null;
            task.next = null;
        }

        private boolean isEmpty()
        {
            return first == null;
        }
    }
}