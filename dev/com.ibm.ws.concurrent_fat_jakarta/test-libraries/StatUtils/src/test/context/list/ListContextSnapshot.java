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
import jakarta.enterprise.concurrent.spi.ThreadContextSnapshot;

/**
 * Example third-party thread context provider, to be used for testing purposes.
 * This context associates a list with a thread.
 */
public class ListContextSnapshot implements ThreadContextSnapshot {
    private final ConcurrentLinkedQueue<Integer> list;

    ListContextSnapshot(ConcurrentLinkedQueue<Integer> list) {
        this.list = list;
    }

    @Override
    public ThreadContextRestorer begin() {
        ThreadContextRestorer restorer = new ListContextRestorer(ListContext.local.get());
        if (list == null)
            ListContext.local.set(new ConcurrentLinkedQueue<Integer>());
        else
            ListContext.local.set(list);
        return restorer;
    }

    @Override
    public String toString() {
        return "ListContextSnapshot@" + Integer.toHexString(hashCode()) + " of " +
               (list == null ? null : ("@" + Integer.toHexString(list.hashCode()) + list));
    }
}
