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

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * A simple task that increments a counter each time it runs
 */
class CounterTask implements Callable<Integer>, Runnable {
    static final TraceComponent tc = Tr.register(CounterTask.class);

    final AtomicInteger counter;
    final ConcurrentLinkedQueue<Long> executionTimes = new ConcurrentLinkedQueue<Long>();

    CounterTask() {
        this.counter = new AtomicInteger();
    }

    CounterTask(AtomicInteger counter) {
        this.counter = counter;
    }

    @Override
    public void run() {
        int i = counter.incrementAndGet();
        Tr.debug(this, tc, "run " + i);
        executionTimes.add(System.currentTimeMillis());
    }

    @Override
    public Integer call() throws Exception {
        int i = counter.incrementAndGet();
        Tr.debug(this, tc, "call " + i);
        executionTimes.add(System.currentTimeMillis());
        return i;
    };
}