package com.atask;

@FunctionalInterface
public interface ResultExecutor<T> extends IExecutor {

    T execute(Context ctx) throws Exception;

}
