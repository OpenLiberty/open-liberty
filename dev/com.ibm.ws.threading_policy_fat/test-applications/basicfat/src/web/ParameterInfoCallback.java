/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import java.util.concurrent.TimeUnit;

import com.ibm.ws.threading.PolicyTaskCallback;
import com.ibm.ws.threading.PolicyTaskFuture;

/**
 * Callback that records information about the parameters that are supplied to it.
 */
public class ParameterInfoCallback extends PolicyTaskCallback {
    public static final int SUBMIT = 0, START = 1, CANCEL = 2, END = 3, NUM_CALLBACKS = END + 1;
    public final PolicyTaskFuture<?>[] future = new PolicyTaskFuture<?>[NUM_CALLBACKS];
    public final Boolean[] isCanceled = new Boolean[NUM_CALLBACKS];
    public final Boolean[] isDone = new Boolean[NUM_CALLBACKS];
    public final CountDownLatch[] latch = new CountDownLatch[NUM_CALLBACKS]; // latch is decremented AFTER populating fields
    public final long[] nsAccept = new long[NUM_CALLBACKS];
    public final long[] nsQueue = new long[NUM_CALLBACKS];
    public final long[] nsRun = new long[NUM_CALLBACKS];
    public final Object[] result = new Object[NUM_CALLBACKS];
    public Object startContext;
    public final Object[] task = new Object[NUM_CALLBACKS];

    public ParameterInfoCallback() {
        for (int i = 0; i < NUM_CALLBACKS; i++)
            latch[i] = new CountDownLatch(1);
    }

    @Override
    public void onCancel(Object task, PolicyTaskFuture<?> future, boolean timedOut, boolean whileRunning) {
        nsAccept[CANCEL] = future.getElapsedAcceptTime(TimeUnit.NANOSECONDS);
        nsQueue[CANCEL] = future.getElapsedQueueTime(TimeUnit.NANOSECONDS);
        nsRun[CANCEL] = future.getElapsedRunTime(TimeUnit.NANOSECONDS);
        System.out.println("onCancel " + future + " accept/queue/run " + nsAccept[CANCEL] + '/' + nsQueue[CANCEL] + '/' + nsRun[CANCEL]);
        this.future[CANCEL] = future;
        this.isCanceled[CANCEL] = future.isCancelled();
        this.isDone[CANCEL] = future.isDone();
        this.task[CANCEL] = task;
        try {
            this.result[CANCEL] = future.get(1, TimeUnit.NANOSECONDS);
        } catch (Throwable x) {
            this.result[CANCEL] = x;
        } finally {
            this.latch[CANCEL].countDown();
        }
    }

    @Override
    public void onEnd(Object task, PolicyTaskFuture<?> future, Object startObj, boolean aborted, int pending, Throwable failure) {
        nsAccept[END] = future.getElapsedAcceptTime(TimeUnit.NANOSECONDS);
        nsQueue[END] = future.getElapsedQueueTime(TimeUnit.NANOSECONDS);
        nsRun[END] = future.getElapsedRunTime(TimeUnit.NANOSECONDS);
        System.out.println("onEnd " + future + " accept/queue/run " + nsAccept[END] + '/' + nsQueue[END] + '/' + nsRun[END]);
        this.future[END] = future;
        this.isCanceled[END] = future.isCancelled();
        this.isDone[END] = future.isDone();
        this.task[END] = task;
        try {
            this.result[END] = future.get(1, TimeUnit.NANOSECONDS);
        } catch (Throwable x) {
            this.result[END] = x;
        } finally {
            this.latch[END].countDown();
        }
    }

    @Override
    public Object onStart(Object task, PolicyTaskFuture<?> future) {
        nsAccept[START] = future.getElapsedAcceptTime(TimeUnit.NANOSECONDS);
        nsQueue[START] = future.getElapsedQueueTime(TimeUnit.NANOSECONDS);
        nsRun[START] = future.getElapsedRunTime(TimeUnit.NANOSECONDS);
        System.out.println("onStart " + future + " accept/queue/run " + nsAccept[START] + '/' + nsQueue[START] + '/' + nsRun[START]);
        this.future[START] = future;
        this.isCanceled[START] = future.isCancelled();
        this.isDone[START] = future.isDone();
        this.task[START] = task;
        this.latch[START].countDown();
        return this;
    }

    @Override
    public void onSubmit(Object task, PolicyTaskFuture<?> future, int invokeAnyCount) {
        nsAccept[SUBMIT] = future.getElapsedAcceptTime(TimeUnit.NANOSECONDS);
        nsQueue[SUBMIT] = future.getElapsedQueueTime(TimeUnit.NANOSECONDS);
        nsRun[SUBMIT] = future.getElapsedRunTime(TimeUnit.NANOSECONDS);
        System.out.println("onSubmit " + future + " accept/queue/run " + nsAccept[SUBMIT] + '/' + nsQueue[SUBMIT] + '/' + nsRun[SUBMIT]);
        this.future[SUBMIT] = future;
        this.isCanceled[SUBMIT] = future.isCancelled();
        this.isDone[SUBMIT] = future.isDone();
        this.task[SUBMIT] = task;
        this.latch[SUBMIT].countDown();
    }
}
