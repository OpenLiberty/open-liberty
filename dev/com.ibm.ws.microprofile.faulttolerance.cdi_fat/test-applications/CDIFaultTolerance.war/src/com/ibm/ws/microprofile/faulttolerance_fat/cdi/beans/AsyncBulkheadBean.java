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
package com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Timeout;

import com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants;

@RequestScoped
@Asynchronous
public class AsyncBulkheadBean {

    private final AtomicInteger connectBCounter = new AtomicInteger(0);
    private final AtomicInteger connectATokens = new AtomicInteger(0);
    private final AtomicInteger connectBTokens = new AtomicInteger(0);
    private final AtomicInteger connectCTokens = new AtomicInteger(0);

    @Bulkhead(value = 2, waitingTaskQueue = 2)
    public Future<Boolean> connectA(String data) throws InterruptedException {
        System.out.println("connectA starting " + data);
        int token = connectATokens.incrementAndGet();
        try {
            if (token > 2) {
                throw new RuntimeException("Too many threads in connectA[" + data + "]: " + token);
            }
            Thread.sleep(TestConstants.WORK_TIME);
            return CompletableFuture.completedFuture(Boolean.TRUE);
        } finally {
            connectATokens.decrementAndGet();
            System.out.println("connectA complete " + data);
        }
    }

    @Timeout(TestConstants.TIMEOUT)
    @Bulkhead(value = 2, waitingTaskQueue = 2)
    public Future<Boolean> connectB(String data) throws InterruptedException {
        System.out.println("connectB starting " + data);
        int token = connectBTokens.incrementAndGet();
        try {
            if (token > 2) {
                throw new RuntimeException("Too many threads in connectB[" + data + "]: " + token);
            }
            int counter = connectBCounter.incrementAndGet();
            System.out.println("connectB counter " + counter);
            if (counter <= 2) {
                System.out.println("connectB sleeping " + data);
                Thread.sleep(TestConstants.WORK_TIME);
                return CompletableFuture.completedFuture(Boolean.FALSE);
            } else {
                return CompletableFuture.completedFuture(Boolean.TRUE);
            }
        } finally {
            connectBTokens.decrementAndGet();
            System.out.println("connectB complete " + data);
        }
    }

    /**
     * Set the Bulkhead value to 3 - which would lead to failure - but this bean's config
     * will be overridden to a value of 2 in microprofile-config.properties, which is what the test expects.
     * If the value were 3, then the method will throw a RuntimeException reporting the instantiation of too
     * many threads.
     */
    @Bulkhead(value = 3, waitingTaskQueue = 2)
    public Future<Boolean> connectC(String data) throws InterruptedException {
        System.out.println("connectC starting " + data);
        int token = connectCTokens.incrementAndGet();
        try {
            if (token > 2) {
                throw new RuntimeException("Too many threads in connectC[" + data + "]: " + token);
            }
            Thread.sleep(TestConstants.WORK_TIME);
            return CompletableFuture.completedFuture(Boolean.TRUE);
        } finally {
            connectCTokens.decrementAndGet();
            System.out.println("connectC complete " + data);
        }
    }
}
