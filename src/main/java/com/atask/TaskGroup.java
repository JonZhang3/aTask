package com.atask;

import com.atask.util.Assert;

import java.util.concurrent.atomic.AtomicInteger;

public final class TaskGroup {

    private final DefaultThreadPoolExecutor executor;
    private final String name;

    protected final AtomicInteger counter = new AtomicInteger(0);
    protected final Data data = new Data();

    private final AtomicInteger latch = new AtomicInteger(0);

    protected TaskGroup(String name, DefaultThreadPoolExecutor executor) {
        this.name = name;
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

    public void await() {
        while (true) {
            if (latch.get() == 0) break;
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
        latch.incrementAndGet();
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

    public static final class Item extends BaseTask {

        private TaskGroup taskGroup;

        protected Item(String type, String id, Executor executor, TaskGroup taskGroup) {
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

        protected Builder(Executor executor, TaskGroup taskGroup) {
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

    protected static class ItemExecutor extends TaskExecutor {

        private final TaskGroup group;

        protected ItemExecutor(BaseTask task, TaskGroup group) {
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
            super.finallyExecute();
            this.group.latch.decrementAndGet();
        }
    }


}