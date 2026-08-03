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
 * A bean that is annotated at the class level to require a Lock of type WRITE.
 * Unannotated methods should follow the class level annotation.
 * Annotated methods override the class level annotation.
 */
@Lock(type = Lock.Type.WRITE,
      accessTimeout = Lock.UNLIMITED)
@ApplicationScoped
public class WriteLockBean {
    /**
     * Maximum amount of time for an operation to remain delayed.
     */
    static final long MAX_DELAY_MS = TimeUnit.MINUTES.toMillis(5);

    private int number;

    @Lock(type = Lock.Type.READ,
          accessTimeout = Lock.UNLIMITED)
    public int blockingReadNumber() {
        return number;
    }

    public void blockingWriteNumber(int newNumber) {
        number = newNumber;
    }

    @Lock(type = Lock.Type.READ,
          accessTimeout = Lock.IMMEDIATE)
    public int delayedReadNumber(CountDownLatch methodisRunning,
                                 CountDownLatch delayer) //
                    throws InterruptedException, TimeoutException {
        methodisRunning.countDown();
        if (delayer.await(MAX_DELAY_MS, TimeUnit.MILLISECONDS))
            return number;
        else
            throw new TimeoutException("timed out");
    }

    @Lock(type = Lock.Type.WRITE,
          accessTimeout = Lock.IMMEDIATE)
    public boolean delayedWriteNumber(CountDownLatch methodisRunning,
                                      CountDownLatch delayer,
                                      int newNumber) //
                    throws InterruptedException, TimeoutException {
        methodisRunning.countDown();
        if (delayer.await(MAX_DELAY_MS, TimeUnit.MILLISECONDS)) {
            number = newNumber;
            return true;
        } else {
            return false;
        }
    }

    // type defaults to WRITE
    public void interruptRepeatedly(Thread threadToInterrupt,
                                    CountDownLatch methodisRunning,
                                    CountDownLatch doneInterrupting) {
        methodisRunning.countDown();
        while (!Thread.interrupted()) {
            try {
                TimeUnit.MILLISECONDS.sleep(200);
            } catch (InterruptedException x) {
                break;
            }
            threadToInterrupt.interrupt();
        }
        doneInterrupting.countDown();
    }

    @Lock(type = Lock.Type.WRITE,
          accessTimeout = 7L,
          unit = TimeUnit.DAYS)
    public void interruptSelf() throws InterruptedException {
        Thread.currentThread().interrupt();
        Thread.sleep(1000); // cause InterruptedException to be raised
    }

    @Lock(type = Lock.Type.READ,
          accessTimeout = 100L,
          unit = TimeUnit.MILLISECONDS)
    public int readNumber() {
        return number;
    }

    @Lock(// type defaults to WRITE
          accessTimeout = 111222333L,
          unit = TimeUnit.NANOSECONDS)
    public void writeNumber(int newNumber) {
        number = newNumber;
    }
}