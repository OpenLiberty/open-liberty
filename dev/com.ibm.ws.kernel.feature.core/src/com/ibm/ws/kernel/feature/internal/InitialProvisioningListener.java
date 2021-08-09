/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal;

import java.util.concurrent.CountDownLatch;

import com.ibm.ws.kernel.feature.internal.ShutdownHookManager.ShutdownHookListener;

/**
 *
 */
public class InitialProvisioningListener implements ShutdownHookListener {

    /** Latch that is notified when initial feature provisioning has completed. */
    protected final CountDownLatch initialProvisioningLatch = new CountDownLatch(1);

    /**
     * Countdown the initial provisioning latch if the JVM is shutdown
     */
    @Override
    public void shutdownHookInvoked() {
        initialProvisioningLatch.countDown();
    }

    /**
     * Wait on the initial provisioning latch
     */
    public void await() throws InterruptedException {
        initialProvisioningLatch.await();
    }

    /**
     * Countdown the initial provisioning latch
     */
    public void countDown() {
        initialProvisioningLatch.countDown();
    }
}
