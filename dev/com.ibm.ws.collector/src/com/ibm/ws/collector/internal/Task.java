/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.collector.internal;

import java.util.concurrent.ExecutorService;

import com.ibm.ws.collector.EventsBuffer;
import com.ibm.ws.logging.collector.Formatter;
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
