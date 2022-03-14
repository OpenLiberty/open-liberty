/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.test.ee.context.priority;

import jakarta.enterprise.concurrent.spi.ThreadContextRestorer;
import jakarta.enterprise.concurrent.spi.ThreadContextSnapshot;

/**
 * Example third-party thread context snapshot, to be used for testing purposes.
 * This context propagates the current thread priority to the task or action.
 */
public class PriorityContextSnapshot implements ThreadContextSnapshot {
    /**
     * The thread priority value to propagate to contextual tasks or actions.
     */
    private final int priorityForPropagation;

    /**
     * Construct a snapshot of the specified thread priority.
     *
     * @param priority thread priority value to propagate to contextual tasks or actions.
     */
    PriorityContextSnapshot(int priority) {
        this.priorityForPropagation = priority;
    }

    @Override
    public ThreadContextRestorer begin() {
        Thread thread = Thread.currentThread();
        ThreadContextRestorer priorityContextRestorer = new PriorityContextRestorer(thread.getPriority());
        thread.setPriority(priorityForPropagation);
        return priorityContextRestorer;
    }

    @Override
    public String toString() {
        return "PriorityContextSnapshot@" + Integer.toHexString(hashCode()) + "(" + priorityForPropagation + ")";
    }
}
