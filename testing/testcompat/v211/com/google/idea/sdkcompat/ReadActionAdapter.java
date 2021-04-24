package com.google.idea.sdkcompat;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.Result;

public abstract class ReadActionAdapter<T> extends ReadAction<T> {
    /** wildcard generics added in 2021.1 */
    @Override
    protected void run(Result<? super T> result) {
        doRun(result);
    }
    protected abstract void  doRun(Result<? super T> result);
}
