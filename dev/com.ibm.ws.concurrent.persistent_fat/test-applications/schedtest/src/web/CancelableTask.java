/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Task that expects to be canceled while running.
 */
public class CancelableTask extends DBIncrementTask {
    private static final long serialVersionUID = 3868336361814085081L;
    private static final ConcurrentHashMap<String, CountDownLatch> canceledSignals = new ConcurrentHashMap<String, CountDownLatch>();
    private static final ConcurrentHashMap<String, CountDownLatch> startedSignals = new ConcurrentHashMap<String, CountDownLatch>();

    private final boolean waitForCancel;

    public CancelableTask(String key, boolean waitForCancel) {
        super(key);
        this.waitForCancel = waitForCancel;
        canceledSignals.put(key, new CountDownLatch(1));
        startedSignals.put(key, new CountDownLatch(1));
    }

    @Override
    public Integer call() throws Exception {
        startedSignals.get(key).countDown();
        Integer result = super.call();
        if (waitForCancel) {
            if (!canceledSignals.get(key).await(SchedulerFATServlet.TIMEOUT_NS, TimeUnit.NANOSECONDS))
                throw new Exception("Did not receive canceled signal within allotted interval");
        }
        return result;
    }

    /**
     * Notify that a CancelableTask has been canceled.
     */
    static void notifyTaskCanceled(String key) {
        canceledSignals.get(key).countDown();
    }

    /**
     * Wait for the specified task to start.
     */
    static void waitForStart(String key) throws Exception {
        boolean started = startedSignals.get(key).await(SchedulerFATServlet.TIMEOUT_NS, TimeUnit.NANOSECONDS);
        if (!started)
            throw new Exception("Task " + key + " not started within allotted interval");
    }
}
