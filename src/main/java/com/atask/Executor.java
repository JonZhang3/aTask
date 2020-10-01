package com.atask;

@FunctionalInterface
public interface Executor extends IExecutor {

    void execute(Context ctx);

}
