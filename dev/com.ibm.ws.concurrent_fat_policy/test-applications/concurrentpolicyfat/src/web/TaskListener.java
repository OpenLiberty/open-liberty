/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;

import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedTaskListener;

/**
 * General purpose ManagedTaskListener, intended for one-time use, that records the parameters supplied to listener methods
 * and performs various operations on them per configurable instructions.
 */
class TaskListener implements ManagedTaskListener {
    static final int NUM_EVENTS = ConcurrentPolicyFATServlet.DONE + 1;

    // The following are instructions to perform actions when listener methods run,
    private final Boolean[] cancelMayInterrupt = new Boolean[NUM_EVENTS]; // null means don't cancel
    final boolean[] doGet = new boolean[NUM_EVENTS];
    private final String[] doLookup = new String[NUM_EVENTS];
    private final boolean[] doRethrow = new boolean[NUM_EVENTS];
    private final long[] doSleepNanos = new long[NUM_EVENTS];

    // The following are populated when the listener methods are invoked,
    final Throwable[] exception = new Throwable[NUM_EVENTS]; // exception supplied to taskAborted/taskDone
    final ManagedExecutorService[] executor = new ManagedExecutorService[NUM_EVENTS];
    final Throwable[] failure = new Throwable[NUM_EVENTS];
    final Future<?>[] future = new Future[NUM_EVENTS];
    final String[] futureToString = new String[NUM_EVENTS];
    final boolean[] invoked = new boolean[NUM_EVENTS];
    final Boolean[] isCancelled = new Boolean[NUM_EVENTS];
    final Boolean[] isDone = new Boolean[NUM_EVENTS];
    final CountDownLatch[] latch = new CountDownLatch[NUM_EVENTS]; // latch is decremented AFTER populating fields
    final Object[] result = new Object[NUM_EVENTS];
    final Boolean[] resultOfCancel = new Boolean[NUM_EVENTS];
    final Object[] resultOfLookup = new Object[NUM_EVENTS];
    final Object[] task = new Object[NUM_EVENTS];
    final long[] threadId = new long[NUM_EVENTS];

    TaskListener() {
        doGet[ConcurrentPolicyFATServlet.DONE] = true;
        for (int i = 0; i < NUM_EVENTS; i++)
            latch[i] = new CountDownLatch(1);
    }

    TaskListener cancelMayInterrupt(boolean mayInterrupt, int... events) {
        for (int event : events)
            cancelMayInterrupt[event] = mayInterrupt;
        return this;
    }

    TaskListener doGet(boolean get, int... events) {
        for (int event : events)
            doGet[event] = get;
        return this;
    }

    TaskListener doLookup(String name, int... events) {
        for (int event : events)
            doLookup[event] = name;
        return this;
    }

    TaskListener doRethrow(boolean rethrow, int... events) {
        for (int event : events)
            doRethrow[event] = rethrow;
        return this;
    }

    TaskListener doSleepNanos(long nanos, int... events) {
        for (int event : events)
            doSleepNanos[event] = nanos;
        return this;
    }

    void handleEvent(String eventName, int event, Future<?> future, ManagedExecutorService executor, Object task, Throwable exception) {
        String s = "> " + getClass().getSimpleName() + '@' + Integer.toHexString(hashCode()) + '.' + eventName + ' ' + future + " from " + executor + " task " + task;
        if (exception != null)
            s += " " + exception.getClass().getSimpleName();
        if (doLookup[event] != null)
            s += "\r\n lookup " + doLookup[event];
        if (cancelMayInterrupt[event] != null)
            s += "\r\n cancel " + cancelMayInterrupt[event];
        if (doSleepNanos[event] > 0)
            s += "\r\n sleep " + doSleepNanos[event] + "ns";
        if (doGet[event])
            s += "\r\n get";
        System.out.println(s);

        this.executor[event] = executor;
        this.exception[event] = exception;
        this.future[event] = future;
        this.invoked[event] = true;
        this.task[event] = task;
        this.threadId[event] = Thread.currentThread().getId();

        try {
            futureToString[event] = future.toString();
            isCancelled[event] = future.isCancelled();
            isDone[event] = future.isDone();

            if (doLookup[event] != null)
                resultOfLookup[event] = new InitialContext().lookup(doLookup[event]);

            if (cancelMayInterrupt[event] != null)
                resultOfCancel[event] = future.cancel(cancelMayInterrupt[event]);

            if (doSleepNanos[event] > 0)
                TimeUnit.NANOSECONDS.sleep(doSleepNanos[event]);

            if (doGet[event])
                result[event] = future.get(1, TimeUnit.NANOSECONDS);

            System.out.println("< " + eventName);
        } catch (Throwable x) {
            failure[event] = x;
            System.out.println("< " + eventName + (doRethrow[event] ? " rethrowing " : " caught ") + x.getClass().getSimpleName());
            x.printStackTrace(System.out);
            if (doRethrow[event]) {
                if (x instanceof RuntimeException)
                    throw (RuntimeException) x;
                else if (x instanceof Error)
                    throw (Error) x;
                else
                    throw new RuntimeException(x);
            }
        } finally {
            latch[event].countDown();
        }
    }

    @Override
    public void taskAborted(Future<?> future, ManagedExecutorService executor, Object task, Throwable failure) {
        handleEvent("taskAborted", ConcurrentPolicyFATServlet.ABORTED, future, executor, task, failure);
    }

    @Override
    public void taskDone(Future<?> future, ManagedExecutorService executor, Object task, Throwable failure) {
        handleEvent("taskDone", ConcurrentPolicyFATServlet.DONE, future, executor, task, failure);
    }

    @Override
    public void taskStarting(Future<?> future, ManagedExecutorService executor, Object task) {
        handleEvent("taskStarting", ConcurrentPolicyFATServlet.STARTING, future, executor, task, null);
    }

    @Override
    public void taskSubmitted(Future<?> future, ManagedExecutorService executor, Object task) {
        handleEvent("taskSubmitted", ConcurrentPolicyFATServlet.SUBMITTED, future, executor, task, null);
    }
}
