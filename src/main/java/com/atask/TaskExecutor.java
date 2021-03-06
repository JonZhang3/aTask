package com.atask;

class TaskExecutor implements Runnable {

    protected final BaseTask task;

    protected TaskExecutor(BaseTask task) {
        this.task = task;
    }

    @Override
    public void run() {
        if (task.setState(State.QUEUED, State.RUNNING)) {
            task.setStartTime(System.currentTimeMillis());
            Context context = createContext();
            try {
                task.getExecutor().execute(context);
                context.onSuccess();
            } catch (Exception e) {
                context.onError(e);
                throw e;
            } finally {
                finallyExecute();
            }
        } else {
            finallyExecute();
        }
    }

    protected Context createContext() {
        return new Context(task);
    }

    protected void finallyExecute() {
        task.setEndTime(System.currentTimeMillis());
    }

}
