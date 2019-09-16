/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Task that increments a static counter.
 */
public class SharedCounterTask implements Callable<Long>, Runnable {
    static final AtomicLong counter = new AtomicLong();

    @Override
    public Long call() {
        return counter.incrementAndGet();
    }

    @Override
    public void run() {
        counter.incrementAndGet();
    }
}
