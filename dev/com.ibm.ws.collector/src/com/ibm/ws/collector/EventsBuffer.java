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
package com.ibm.ws.collector;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.logging.synch.ThreadLocalHandler;

public class EventsBuffer {

    private static final TraceComponent tc = Tr.register(EventsBuffer.class);

    private final long bufferMaxSize;
    private final Target target;
    private List<Object> events;

    private Timer timer;
    private final long period;

    public EventsBuffer(Target target, long bufferMaxSize, long period) {
        this.target = target;
        this.bufferMaxSize = bufferMaxSize;
        events = new ArrayList<Object>();
        this.period = period;
    }

    public synchronized void start() {
        if (timer == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Starting timer", this.target, bufferMaxSize, period);
            }
            timer = new Timer("EventBufferTimer");
            timer.schedule(new EventsBufferTimerTask(), 0, period);
        }
    }

    public synchronized void stop() {
        if (timer != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Stopping timer", this.target);
            }
            timer.cancel();
            timer = null;
        }
    }

    public synchronized void add(Object event) {
        events.add(event);
        if (events.size() >= bufferMaxSize) {
            flushBuffer();
        }
    }

    public synchronized void flushBuffer() {
        if (events.size() > 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Events: " + events.size());
            }
            long startTime = System.nanoTime();

            target.sendEvents(events);
            events = new ArrayList<Object>();

            traceTime(tc, startTime, "Flush");
        }
    }

    class EventsBufferTimerTask extends TimerTask {
        @Override
        @Trivial
        public void run() {
            ThreadLocalHandler.set(Boolean.TRUE);
            flushBuffer();
            ThreadLocalHandler.remove();
        }
    }

    private static void traceTime(TraceComponent tc, long startTime, String label) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            long endTime = System.nanoTime();
            String s = String.format(label + ": %10.3f ms", (endTime - startTime) / 1000000.0f);
            Tr.event(tc, s);
        }
    }

}
