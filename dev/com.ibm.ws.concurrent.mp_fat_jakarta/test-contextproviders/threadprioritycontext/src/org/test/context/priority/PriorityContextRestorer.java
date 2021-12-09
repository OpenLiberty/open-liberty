/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.test.context.priority;

import org.eclipse.microprofile.context.spi.ThreadContextController;

/**
 * Example third-party thread context restorer, to be used for testing purposes.
 * This context propagates the current thread priority to the task or action.
 */
public class PriorityContextRestorer implements ThreadContextController {
    private boolean restored = false;
    private final int priorityToRestore;

    PriorityContextRestorer(int priorityToRestore) {
        this.priorityToRestore = priorityToRestore;
    }

    @Override
    public void endContext() {
        if (restored)
            throw new IllegalStateException("thread context was already restored");
        Thread.currentThread().setPriority(priorityToRestore);
        restored = true;
    }

    @Override
    public String toString() {
        return "PriorityContextRestorer@" + Integer.toHexString(hashCode()) + "(" + priorityToRestore + ")";
    }
}
