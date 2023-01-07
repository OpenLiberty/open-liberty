/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package test.context.priority;

import jakarta.enterprise.concurrent.spi.ThreadContextRestorer;

/**
 * Example third-party thread context provider, to be used for testing purposes.
 * This context provider makes the priority of a thread part of the context that
 * gets propagated.
 */
public class PriorityContextRestorer implements ThreadContextRestorer {
    private boolean restored;
    private final int priority;

    PriorityContextRestorer(int priority) {
        this.priority = priority;
    }

    @Override
    public void endContext() {
        if (restored)
            throw new IllegalStateException("thread context was already restored");
        Thread.currentThread().setPriority(priority);
        restored = true;
    }

    @Override
    public String toString() {
        return "PriorityContextRestorer@" + Integer.toHexString(hashCode()) + ":" + priority;
    }
}
