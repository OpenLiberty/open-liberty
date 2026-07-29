/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package test.jakarta.concurrency32cdi.web;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.concurrent.Lock;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * A bean that is annotated at the class level to require a Lock of type READ.
 * Unannotated methods should follow the class level annotation.
 * Annotated methods override the class level annotation.
 */
@ApplicationScoped
@Lock(type = Lock.Type.READ,
      accessTimeout = Lock.IMMEDIATE)
public class ReadLockBean {
    /**
     * Maximum amount of time for an operation to remain delayed.
     */
    static final long MAX_DELAY_MS = TimeUnit.MINUTES.toMillis(5);

    private String value;

    @Lock(type = Lock.Type.READ,
          accessTimeout = Lock.UNLIMITED)
    public String blockingReadValue() {
        return value;
    }

    @Lock(// type defaults to WRITE
          accessTimeout = Lock.UNLIMITED)
    public void blockingWriteValue(String newValue) {
        value = newValue;
    }

    public String delayedReadValue(CountDownLatch methodisRunning,
                                   CountDownLatch delayer) //
                    throws InterruptedException, TimeoutException {
        methodisRunning.countDown();
        if (delayer.await(MAX_DELAY_MS, TimeUnit.MILLISECONDS))
            return value;
        else
            throw new TimeoutException("timed out");
    }

    @Lock(type = Lock.Type.WRITE,
          accessTimeout = Lock.IMMEDIATE)
    public void delayedWriteValue(CountDownLatch methodisRunning,
                                  CountDownLatch delayer,
                                  String newValue) {
        methodisRunning.countDown();
        try {
            if (delayer.await(MAX_DELAY_MS, TimeUnit.MILLISECONDS))
                value = newValue;
            else
                throw new AssertionError(new TimeoutException("timed out"));
        } catch (InterruptedException x) {
            throw new AssertionError(x);
        }
    }

    public String readValue() {
        return value;
    }

    @Lock // type defaults to WRITE
    public void writeValue(String newValue) {
        value = newValue;
    }
}