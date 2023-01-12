package com.atask;

import com.atask.util.Assert;
import com.atask.util.Utils;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// 非线程安全
// 只能在同一线程添加任务，以及在同一线程调用 await 方法
public final class TaskGroup {

    private final DefaultThreadPoolExecutor executor;
    private final String name;
    private final String id;

    final AtomicInteger counter = new AtomicInteger(0);
    final Data data = new Data();

    private final Deque<Task> runningTaskQueue = new ConcurrentLinkedDeque<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition isEmpty = lock.newCondition();
    private final AtomicInteger taskCount = new AtomicInteger(0);

    TaskGroup(String name, DefaultThreadPoolExecutor executor) {
        this.name = name;
        this.id = Utils.generateId();
        this.executor = executor;
    }

    public void whenComplete(final Runnable runnable) {
        new Thread(() -> {
            await();
            if (runnable != null) {
                runnable.run();
            }
        }).start();
    }

    /**
     * 等待任务组中的所有任务执行完毕
     * <p>
     *     该方法会阻塞当前线程，直到任务组中的所有任务执行完毕
     *     该方法只能在同一线程中调用
     * </p>
     * <p>之前该方法使用死循环实现，这有可能造成阻塞线程永远无法停止</p>
     * <p>所以在新版本中放弃死循环的阻塞方式，改为使用 {@link Condition} 的方式实现</p>
     */
    public void await() {
        lock.lock();
        try {
            isEmpty.await();
        } catch (InterruptedException ignore) {

        } finally {
            lock.unlock();
        }
    }

    public int getCounter() {
        return counter.get();
    }

    public Data getData() {
        return this.data;
    }

    public void go(Item item) {
        Assert.notNull(item);

        item.setTaskGroup(this);
        runningTaskQueue.offer(item);
        taskCount.incrementAndGet();
        executor.submit(item, this);
    }

    public Builder buildItem(Executor executor) {
        Assert.notNull(executor);

        return new Builder(executor, this);
    }

    public Item newItem(Executor executor) {
        Assert.notNull(executor);

        return new Builder(executor, this).build();
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public List<Task> getRunningTasks() {
        return new LinkedList<>(runningTaskQueue);
    }

    public static final class Item extends BaseTask {

        private TaskGroup taskGroup;

        private Item(String type, String id, Executor executor, TaskGroup taskGroup) {
            super(type, id, executor);
            this.taskGroup = taskGroup;
        }

        private void setTaskGroup(TaskGroup group) {
            this.taskGroup = group;
        }

        public String getGroupName() {
            return this.taskGroup.name;
        }

    }

    public static final class Builder extends Task.Builder {

        private final TaskGroup taskGroup;

        private Builder(Executor executor, TaskGroup taskGroup) {
            super(executor);
            this.taskGroup = taskGroup;
        }

        @Override
        public Item build() {
            Item item = new Item(this.type, this.id, executor, taskGroup);
            item.setProgress(this.progress);
            item.setCallback(this.callback);
            return item;
        }
    }

    protected static class GroupItemExecutor extends TaskExecutor {

        private final TaskGroup group;

        protected GroupItemExecutor(BaseTask task, TaskGroup group) {
            super(task);
            this.group = group;
        }

        @Override
        public void run() {
            super.run();
        }

        @Override
        protected Context createContext() {
            return new Context(task, group);
        }

        @Override
        protected void finallyExecute() {
            try {
                super.finallyExecute();
            } finally {
                this.group.runningTaskQueue.remove(this.task);
                this.group.taskCount.decrementAndGet();
            }
            // 尝试将任务数设置为 0，如果设置成功，然后尝试唤醒等待的线程    
            if (this.group.taskCount.compareAndSet(0, 0)) {
                this.group.lock.lock();
                try {
                    this.group.isEmpty.signalAll();
                } finally {
                    this.group.lock.unlock();
                }
            }
        }
    }

}
