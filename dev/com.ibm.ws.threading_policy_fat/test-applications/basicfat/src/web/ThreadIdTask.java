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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Task that returns the id of the thread on which it runs when used as a Callable.
 * When used as a Runnable, it stores the id of the thread upon which it most recently ran in the threadId field.
 */
public class ThreadIdTask implements Callable<Long>, Runnable {
    AtomicReference<Long> threadId = new AtomicReference<Long>();

    @Override
    public Long call() {
        System.out.println("> call " + toString());
        long threadId = Thread.currentThread().getId();
        System.out.println("< call " + toString() + " " + Long.toHexString(threadId));
        return threadId;
    }

    @Override
    public void run() {
        System.out.println("> run " + toString());
        long threadId = Thread.currentThread().getId();
        this.threadId.set(threadId);
        System.out.println("< run " + toString() + " " + Long.toHexString(threadId));
    }
}
