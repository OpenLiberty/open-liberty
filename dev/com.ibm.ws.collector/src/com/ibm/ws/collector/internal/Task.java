/*******************************************************************************
 * Copyright (c) 2016, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

    protected boolean monitorWebAppRemoval = false;

    public void setMonitorWebAppRemoval(boolean shouldMonitor) {
        this.monitorWebAppRemoval = shouldMonitor;
    }

    public boolean getMonitorWebAppRemoval() {
        return monitorWebAppRemoval;
    }

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

    /**
     * Read the next event from the buffer and check if it contains the specified message ID.
     * This method does NOT emit the event to the eventsBuffer.
     *
     * @param messageId The message ID to search for (e.g., "CWWKT0017I")
     * @return true if the message ID is found in the event, false otherwise
     */
    public boolean checkNextEventForMessage(String messageId) {
        if (bufferMgr == null || handlerName == null) {
            return false;
        }

        try {
            Object event = bufferMgr.getNextEvent(handlerName);
            if (event != null) {
                java.lang.reflect.Method getMessageMethod = event.getClass().getMethod("getMessage");
                String message = (String) getMessageMethod.invoke(event);

                if (message != null && message.startsWith(messageId)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignore exceptions and return false
        }
        return false;
    }
}
