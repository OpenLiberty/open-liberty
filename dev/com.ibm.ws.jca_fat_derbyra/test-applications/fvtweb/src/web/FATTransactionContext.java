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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.resource.spi.work.TransactionContext;
import javax.resource.spi.work.WorkContextLifecycleListener;

/**
 * Transaction inflow context that implements WorkContextLifecycleListener
 */
class FATTransactionContext extends TransactionContext implements WorkContextLifecycleListener {
    private static final long serialVersionUID = 5661421729635409167L;

    final AtomicInteger contextSetupCompletedCount = new AtomicInteger();
    final Queue<String> contextSetupFailureCodes = new ConcurrentLinkedQueue<String>();

    public FATTransactionContext() {
        super();
    }

    /**
     * @see javax.resource.spi.work.WorkContextLifecycleListener#contextSetupComplete()
     */
    @Override
    public void contextSetupComplete() {
        contextSetupCompletedCount.incrementAndGet();
    }

    /**
     * @see javax.resource.spi.work.WorkContextLifecycleListener#contextSetupFailed(java.lang.String)
     */
    @Override
    public void contextSetupFailed(String errorCode) {
        contextSetupFailureCodes.add(errorCode);
    }
}
