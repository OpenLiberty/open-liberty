/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015, 2016
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.collector.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.Future;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.logging.synch.ThreadLocalHandler;

/**
 * Implementation of the task interface.
 * Each source will have a task created for it, that will run in a separate thread.
 */
public class TaskImpl extends Task implements Runnable {

    private static final TraceComponent tc = Tr.register(TaskImpl.class);

    private volatile Future<?> future = null;

    @FFDCIgnore(value = { InterruptedException.class, IllegalArgumentException.class })
    @Override
    public void run() {
        //Set the thread local to indicate that any trace or logging
        //event should not be routed back to the handler
        ThreadLocalHandler.set(Boolean.TRUE);

        //Set the thread name to something that will help us diagnose which threads are which
        //in case something goes wrong
        String originalThreadName = Thread.currentThread().getName();
        setThreadName(originalThreadName + "-collector-" + this.config.getSourceName() + "-" + handlerName);

        try {
            boolean done = false;
            while (!done) {
                try {
                    Object event = bufferMgr.getNextEvent(handlerName);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Received event ", this, this.config.getSourceName(), event);
                    }
                    processEvent(event);
                } catch (IllegalArgumentException exit) {
                    //Exit
                    done = true;
                } catch (InterruptedException exit) {
                    //Exit
                    done = true;
                } catch (Exception e) {
                    //FFDC
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Exception caught while running task.  Task will continue.", e);
                    }
                }
            }
        } finally {
            //Clean up and release resources
            executorSrvc = null;
            formatter = null;
            bufferMgr = null;
            setThreadName(originalThreadName);
            ThreadLocalHandler.remove();
            setConfig(null);
        }
    }

    @Override
    public void stop() {
        if (future != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Stopping task", this, this.config.getSourceName());
            }
            future.cancel(true);
            future = null;
        }
    }

    @Override
    public void start() {
        if (future == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Starting task", this, this.config.getSourceName());
            }
            future = executorSrvc.submit(this);
        }
    }

    private void processEvent(Object event) {
        long startTime = System.nanoTime();

        Object formattedEvent = formatter.formatEvent(config.getSourceName(), config.getLocation(), event, config.getTags(), config.getMaxFieldLength());
        if (formattedEvent != null)
            eventsBuffer.add(formattedEvent);

        traceTime(tc, startTime, "FormatAndQueue");
    }

    private static void traceTime(TraceComponent tc, long startTime, String label) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            long endTime = System.nanoTime();
            String s = String.format(label + ": %10.3f ms", (endTime - startTime) / 1000000.0f);
            Tr.event(tc, s);
        }
    }

    private static void setThreadName(final String name) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                Thread.currentThread().setName(name);
                return null;
            }
        });
    }
}
