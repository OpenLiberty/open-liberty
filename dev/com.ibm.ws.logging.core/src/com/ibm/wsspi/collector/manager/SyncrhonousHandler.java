package com.ibm.wsspi.collector.manager;

public interface SyncrhonousHandler extends Handler {
    
    void synchronousWrite(Object event);
}
