package io.openliberty.netty.internal.impl;

import java.util.concurrent.atomic.AtomicBoolean;

public class QuiesceState {
    private static final AtomicBoolean quiesceInProgress = new AtomicBoolean(false);

    public static boolean isQuiesceInProgress(){
        return quiesceInProgress.get();
    }

    public static void startQuiesce(){
        quiesceInProgress.set(true);
    }

    public static void stopQuiesce(){
        quiesceInProgress.set(false);
    }
}