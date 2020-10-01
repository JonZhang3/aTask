package com.atask;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class DefaultThreadPoolExecutor extends ThreadPoolExecutor {

    private final Deque<Task> runningQueue = new ConcurrentLinkedDeque<>();
    private final LinkedBlockingDeque<Task> completedQueue = new LinkedBlockingDeque<>();

    private final CompletedTaskHandler completedTaskHandler;

    public DefaultThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                     BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
                                     RejectedExecutionHandler handler, CompletedTaskHandler completedTaskHandler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        if (completedTaskHandler == null) {
            this.completedTaskHandler = new DefaultCompletedTaskHandler();
        } else {
            this.completedTaskHandler = completedTaskHandler;
        }
        startHandleCompletedTask();
    }

    public void submit(Task task) {
        if (task instanceof BaseTask) {
            BaseTask bTask = (BaseTask) task;
            TaskExecutor taskExecutor = new TaskExecutor(bTask);
            RunnableFuture<Object> future = newTaskFor(task, taskExecutor, null);
            bTask.setFuture(future);
            bTask.setState(State.INIT, State.QUEUED);
            execute(future);
        } else if (task instanceof ResultBaseTask) {
            ResultBaseTask bTask = (ResultBaseTask) task;
            ResultTaskExecutor executor = new ResultTaskExecutor(bTask);
            RunnableFuture future = newTaskFor(task, executor);
            bTask.setFuture(future);
            bTask.setState(State.INIT, State.QUEUED);
            execute(future);
        }
        runningQueue.offer(task);
    }

    public void submit(TaskGroup.Item item, TaskGroup group) {
        TaskGroup.ItemExecutor executor = new TaskGroup.ItemExecutor(item, group);
        RunnableFuture<Object> future = newTaskFor(item, executor, null);
        item.setFuture(future);
        item.setState(State.INIT, State.QUEUED);
        execute(future);
        runningQueue.offer(item);
    }


    private <T> RunnableFuture<T> newTaskFor(Task task, Callable<T> callable) {
        return new CustomFutureTask<>(task, callable);
    }


    private <T> RunnableFuture<T> newTaskFor(Task task, Runnable runnable, T value) {
        return new CustomFutureTask<>(task, runnable, value);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        if (r instanceof CustomFutureTask) {
            CustomFutureTask<?> futureTask = (CustomFutureTask<?>) r;
            Task task = futureTask.getTask();
            runningQueue.remove(task);
            completedQueue.offer(task);
        }
    }

    private void startHandleCompletedTask() {
        new Thread(() -> {
            while (true) {
                try {
                    Task take = completedQueue.take();
                    DefaultThreadPoolExecutor.this.completedTaskHandler.handle(take);
                } catch (Throwable ignore) {
                }
            }
        }).start();
    }

    public final List<Task> getRunningTasks() {
        return new LinkedList<>(runningQueue);
    }

    private static class CustomFutureTask<V> extends FutureTask<V> {

        private final Task task;

        public CustomFutureTask(Task task, Callable<V> callable) {
            super(callable);
            this.task = task;
        }

        public CustomFutureTask(Task task, Runnable runnable, V result) {
            super(runnable, result);
            this.task = task;
        }

        public final Task getTask() {
            return task;
        }
    }

}
