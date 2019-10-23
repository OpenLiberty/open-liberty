/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Task that expects to be canceled while running.
 */
public class CancelableTask extends DBIncrementTask {
    private static final long serialVersionUID = 3868336361814085081L;
    private static final LinkedBlockingQueue<String> canceledSignal = new LinkedBlockingQueue<String>();
    private static final LinkedBlockingQueue<String> startedSignal = new LinkedBlockingQueue<String>();

    private final boolean waitForCancel;

    public CancelableTask(String key, boolean waitForCancel) {
        super(key);
        this.waitForCancel = waitForCancel;
    }

    @Override
    public Integer call() throws Exception {
        startedSignal.add(execProps.get(IDENTITY_NAME));
        Integer result = super.call();
        if (waitForCancel) {
            String canceled = canceledSignal.poll(SchedulerFATServlet.TIMEOUT_NS, TimeUnit.NANOSECONDS);
            if (canceled == null)
                throw new Exception("Did not receive canceled signal within allotted interval");
        }
        return result;
    }

    /**
     * Notify that a CancelableTask has been canceled.
     * WARNING: This is only valid if no more than one CancelableTask is submitted/running at any given point in time.
     */
    static void notifyTaskCanceled(String key) {
        canceledSignal.add(key);
    }

    /**
     * Wait for the specified task to start.
     * WARNING: This is only valid if no more than one CancelableTask is submitted/running at any given point in time.
     */
    static void waitForStart(String key) throws Exception {
        String started = null;
        do
            started = CancelableTask.startedSignal.poll(SchedulerFATServlet.TIMEOUT_NS, TimeUnit.NANOSECONDS);
        while (started != null && !started.equals(key));
        if (started == null)
            throw new Exception("Task " + key + " not started within allotted interval");
    }
}
