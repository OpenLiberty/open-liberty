/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.collector.internal;

import java.util.concurrent.ExecutorService;

import com.ibm.ws.collector.EventsBuffer;
import com.ibm.ws.collector.Formatter;
import com.ibm.wsspi.collector.manager.BufferManager;

/**
 * Abstract class that defines the semantics of a task
 */
public abstract class Task {

    protected String handlerName;

    protected TaskConfig config;

    protected BufferManager bufferMgr;

    protected ExecutorService executorSrvc;

    protected Formatter formatter;

    protected EventsBuffer eventsBuffer;

    public abstract void start();

    public abstract void stop();

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public TaskConfig getConfig() {
        return config;
    }

    public void setConfig(TaskConfig config) {
        this.config = config;
    }

    public void setExecutorService(ExecutorService executorSrvc) {
        this.executorSrvc = executorSrvc;
    }

    public void setFormatter(Formatter formatter) {
        this.formatter = formatter;
    }

    public void setBufferMgr(BufferManager bufferMgr) {
        this.bufferMgr = bufferMgr;
    }

    public void setEventsBuffer(EventsBuffer eventsBuffer) {
        this.eventsBuffer = eventsBuffer;
    }
}
