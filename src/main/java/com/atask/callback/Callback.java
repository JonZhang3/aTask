package com.atask.callback;

import com.atask.Context;

@FunctionalInterface
public interface Callback {

    void call(Context ctx, Exception error);

}
