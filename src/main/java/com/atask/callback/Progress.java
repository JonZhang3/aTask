package com.atask.callback;

@FunctionalInterface
public interface Progress {

    void call(int progress);

}
