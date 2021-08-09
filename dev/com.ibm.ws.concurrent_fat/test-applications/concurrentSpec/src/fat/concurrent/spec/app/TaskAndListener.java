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
package fat.concurrent.spec.app;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;

/**
 * A task that is also a ManagedTaskListener.
 */
public class TaskAndListener implements ManagedTask, ManagedTaskListener, Runnable {
    final BlockingQueue<String> events = new LinkedBlockingQueue<String>();
    long sleep;
    CountDownLatch startedLatch = new CountDownLatch(1);

    @Override
    public Map<String, String> getExecutionProperties() {
        return null;
    }

    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return this;
    }

    @Override
    public void run() {
        System.out.println("Running with sleep=" + sleep);
        startedLatch.countDown();
        if (sleep > 0)
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException x) {
            }
    }

    @Override
    public void taskAborted(Future<?> future, ManagedExecutorService executor, Object task, Throwable x) {
        events.add("ABORTED: canceled=" + future.isCancelled() + " exception=" + (x == null ? null : x.getClass().getName()));
    }

    @Override
    public void taskDone(Future<?> future, ManagedExecutorService executor, Object task, Throwable x) {
        events.add("DONE: canceled=" + future.isCancelled() + " exception=" + (x == null ? null : x.getClass().getName()));
    }

    @Override
    public void taskStarting(Future<?> future, ManagedExecutorService executor, Object task) {
        events.add("STARTING");
    }

    @Override
    public void taskSubmitted(Future<?> future, ManagedExecutorService executor, Object task) {
        events.add("SUBMITTED");
    }
}
