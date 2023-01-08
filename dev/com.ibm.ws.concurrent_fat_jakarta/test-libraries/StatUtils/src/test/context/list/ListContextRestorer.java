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
package test.context.list;

import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.enterprise.concurrent.spi.ThreadContextRestorer;

/**
 * Example third-party thread context provider, to be used for testing purposes.
 * This context associates a list with a thread.
 */
public class ListContextRestorer implements ThreadContextRestorer {
    private final ConcurrentLinkedQueue<Integer> list;
    private boolean restored;

    ListContextRestorer(ConcurrentLinkedQueue<Integer> list) {
        this.list = list;
    }

    @Override
    public void endContext() {
        if (restored)
            throw new IllegalStateException("thread context was already restored");
        if (list == null)
            ListContext.local.remove();
        else
            ListContext.local.set(list);
        restored = true;
    }

    @Override
    public String toString() {
        return "ListContextRestorer@" + Integer.toHexString(hashCode()) + " for " +
               (list == null ? null : ("@" + Integer.toHexString(list.hashCode()) + list));
    }
}
