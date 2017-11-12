package com.ibm.wsspi.collector.manager;

public interface SyncrhonousHandler extends Handler {

    boolean isSynchronous();
    
    void synchronousWrite(Object event);
}
