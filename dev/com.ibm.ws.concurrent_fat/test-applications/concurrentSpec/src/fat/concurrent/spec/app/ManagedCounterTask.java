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
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * A simple task that increments a counter each time it runs
 */
class ManagedCounterTask implements Callable<Integer>, ManagedTask, Runnable {
    static final TraceComponent tc = Tr.register(ManagedCounterTask.class);

    final AtomicInteger counter;
    final Map<String, String> executionProperties = new TreeMap<String, String>();
    final ConcurrentLinkedQueue<Long> executionTimes = new ConcurrentLinkedQueue<Long>();
    boolean failToGetExecutionProperties;
    boolean failToGetManagedTaskListener;
    ManagedTaskListener listener;

    ManagedCounterTask() {
        this.counter = new AtomicInteger();
    }

    @Override
    public Integer call() throws Exception {
        int i = counter.incrementAndGet();
        Tr.debug(this, tc, "call " + i);
        executionTimes.add(System.currentTimeMillis());
        return i;
    }

    @Override
    public Map<String, String> getExecutionProperties() {
        if (failToGetExecutionProperties)
            throw new NumberFormatException("Intentionally caused failure");
        return executionProperties;
    }

    @Override
    public ManagedTaskListener getManagedTaskListener() {
        if (failToGetManagedTaskListener)
            throw new IllegalMonitorStateException("Intentionally caused failure");
        return listener;
    };

    @Override
    public void run() {
        int i = counter.incrementAndGet();
        Tr.debug(this, tc, "run " + i);
        executionTimes.add(System.currentTimeMillis());
    }
}