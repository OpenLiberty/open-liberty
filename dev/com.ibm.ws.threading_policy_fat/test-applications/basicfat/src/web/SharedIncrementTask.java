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

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Task that increments a counter which can be shared by multiple tasks to record how many have run.
 */
public class SharedIncrementTask implements Callable<Integer>, Runnable {
    private final AtomicInteger counter;

    public SharedIncrementTask() {
        counter = new AtomicInteger();
    }

    public SharedIncrementTask(AtomicInteger counter) {
        this.counter = counter;
    }

    @Override
    public Integer call() throws Exception {
        int count = counter.incrementAndGet();
        System.out.println("call " + toString() + " execution #" + count);
        return count;
    }

    public int count() {
        return counter.get();
    }

    @Override
    public void run() {
        int count = counter.incrementAndGet();
        System.out.println("run " + toString() + " execution #" + count);
    }
}
