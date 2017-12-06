/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.collector.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayDeque;
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

    private static final TraceComponent tc = Tr.register(TaskImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

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
            //Initializing throttling variables -numEvents to track the no. of events, windowDuration=1 second (1000 Milli Seconds)
            int windowDuration = 1000;
            int maxEvents = 0;
            //initialize Throttler class
            Throttler throttle = new Throttler(maxEvents, windowDuration);
            while (!done) {
                //Events Throttling config parameter - maxEvents
                maxEvents = config.getMaxEvents();
                try {
                    Object event = bufferMgr.getNextEvent(handlerName);
                    if (maxEvents > 0) {
                        throttle.setMaxEvents(maxEvents);
                        throttle.go();
                    }
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

        } finally

        {
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

    /**
     * Throttler class is used to slow down the number of events processed if the event handling rate
     * exceeds maxEvents specified in the windowDuration specified.
     */
    private class Throttler {
        private int maxEvents;
        private final long windowDuration;
        private final ArrayDeque ringBuffer = new ArrayDeque();
        private boolean throttling = false;

        /**
         * Initialize the maximum number of events going through at windowDuration ms
         *
         * @param maxEvents maximum number of events specified
         * @param windowDuration the window time frame in milliseconds for the number of maxEvents specified
         */
        public Throttler(int maxEvents, long windowDuration) {
            if (maxEvents < 0 || windowDuration < 1) {
                throw new IllegalArgumentException("maxEvents cannot be less than 0 or windowDuration cannot be less than 1");
            }
            this.maxEvents = maxEvents;
            this.windowDuration = windowDuration;

        }

        /**
         * Calculates the number of of milliseconds needed to sleep after the event is
         * received.
         *
         * @return returns the number of milliseconds needed to sleep.
         *         If it is 0 seconds, event will process without sleeping, else return
         *         the number of seconds needed to sleep.
         */
        long go() {
            long currentTime = System.currentTimeMillis();
            long oldestTime = 0;
            //add new time if there are less than maxEvents in ringBuffer
            if (ringBuffer.size() < maxEvents) {
                oldestTime = 0;
            }
            //ringBuffer is at maxEvents, take out the oldest event in buffer and add the newest
            else {
                oldestTime = (Long) ringBuffer.removeFirst();
            }
            long holdTime = windowDuration - (currentTime - oldestTime);
            if (holdTime > 0) {
                try {
                    Thread.sleep(holdTime);
                } catch (InterruptedException e) {

                }
                //change currentTime to the new currentTime after the sleep
                currentTime = System.currentTimeMillis();
                ringBuffer.add(currentTime);
                if (!this.throttling) {
                    Tr.info(tc, "MAXEVENTS_EXCEEDS_MAXRATE", Thread.currentThread().getName(), maxEvents);
                    this.throttling = true;
                }
                if (TraceComponent.isAnyTracingEnabled()) {
                    Tr.debug(tc, "Thread" + Thread.currentThread().getName() + " slept for " + holdTime + " ms due to throttling. Throttle rate set to " +
                                 this.maxEvents + " per " + windowDuration + " milliseconds.");
                }
            } else {
                //if holdTime < 0 and has been windowDuration ms since the last event
                if (this.throttling && (currentTime - (Long) ringBuffer.getLast()) >= windowDuration) {
                    Tr.info(tc, "MAXEVENTS_NOTEXCEED_RATE");
                    this.throttling = false;
                }
                ringBuffer.add(currentTime);
                holdTime = 0;
            }
            return holdTime;

        }

        /** Set maxEvents when it's changed */
        public void setMaxEvents(int maxEvents) {
            if (maxEvents < 0) {
                throw new IllegalArgumentException("maxEvents cannot be less than 0");
            }
            //if the new maxEvents size is less than the current ringBuffer size
            while (ringBuffer.size() > maxEvents) {
                ringBuffer.removeFirst();
            }
            this.maxEvents = maxEvents;
        }

    }

}
