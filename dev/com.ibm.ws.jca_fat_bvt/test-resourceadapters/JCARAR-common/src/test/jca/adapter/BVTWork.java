/*******************************************************************************
 * Copyright (c) 2012,2022 IBM Corporation and others.
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
package test.jca.adapter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.resource.spi.work.Work;

/**
 * Generic work that runs a Callable.
 */
public abstract class BVTWork<T> implements Callable<T>, Work {

    private final AtomicBoolean canceled = new AtomicBoolean();
    private final ConcurrentLinkedQueue<Thread> executionThreads = new ConcurrentLinkedQueue<Thread>();
    private final BlockingQueue<Object> results = new LinkedBlockingQueue<Object>();

    /**
     * Wait for a result. Raises an error if the work fails
     */
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        Object result = results.poll(timeout, unit);
        if (result instanceof Error)
            throw (Error) result;
        else if (result instanceof RuntimeException)
            throw (RuntimeException) result;
        else {
            @SuppressWarnings("unchecked")
            T t = (T) result;
            return t;
        }
    }

    @Override
    public void release() {
        canceled.set(true);
        for (Thread th = executionThreads.poll(); th != null; th = executionThreads.poll())
            th.interrupt();
    }

    @Override
    public void run() {
        if (canceled.get())
            return;
        executionThreads.add(Thread.currentThread());
        try {
            results.add(call());
        } catch (Throwable x) {
            results.add(x);
        } finally {
            executionThreads.remove(Thread.currentThread());
        }
    }
}
