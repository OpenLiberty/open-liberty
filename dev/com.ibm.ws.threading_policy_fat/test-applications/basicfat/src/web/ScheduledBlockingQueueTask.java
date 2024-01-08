/*******************************************************************************
 * Copyright (c) 2021,2023 IBM Corporation and others.
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
package web;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.ws.threading.ScheduledCustomExecutorTask;

/**
 * A task that directs its execution to the specified PolicyExecutor
 * when submitted to the Liberty scheduled executor.
 * When the task runs, it optionally executes a supplied action
 * and then adds to a LinkedBlockingQueue that tests can
 * poll to identify how many executions have occurred.
 */
public class ScheduledBlockingQueueTask extends LinkedBlockingQueue<Integer> implements Runnable, ScheduledCustomExecutorTask {
    private static final long serialVersionUID = 1514933845687256072L;

    /**
     * Optional list of actions to take upon task executions.
     * The previous value of the counter is used as the index into the actions list
     * to determine which action to run upon which execution.
     */
    private final Runnable[] actions;

    /**
     * Counter to increment with each execution.
     */
    private final AtomicInteger counter = new AtomicInteger();

    /**
     * Executor upon which to run executions of this task.
     */
    private final PolicyExecutor executor;

    /**
     * Increments a counter every time the run method is invoked,
     * adding the value to the queue.
     *
     * @param executor policy executor on which to run the task.
     * @param actions  optional list of actions to take upon execution.
     *                     At most one action per execution, using the previous counter value
     *                     as the index into the actions list.
     */
    public ScheduledBlockingQueueTask(PolicyExecutor executor, Runnable... actions) {
        this.actions = actions;
        this.executor = executor;
    }

    @Override
    public PolicyExecutor getExecutor() {
        return executor;
    }

    /**
     * Limit to 1 catch-up execution when delayed.
     */
    @Override
    public long getNextFixedRateExecutionTime(long expectedExecutionTime, long period) {
        long missedExecutions = (System.nanoTime() - expectedExecutionTime) / period;
        if (missedExecutions < 0)
            missedExecutions = 0;
        return expectedExecutionTime + period * (1 + missedExecutions);
    }

    @Override
    public Exception resubmitFailed(Exception failure) {
        return failure;
    }

    @Override
    public void run() {
        int count = counter.incrementAndGet();
        System.out.println("> run #" + count + " " + toString());
        if (count <= actions.length) {
            Runnable action = actions[count - 1];
            if (action != null)
                action.run();
        }
        add(count);
        System.out.println("< run #" + count + " " + toString());
    }

    @Override
    public String toString() {
        return "ScheduledBlockingQueueTask@" + Integer.toHexString(System.identityHashCode(this)) + super.toString();
    }
}
