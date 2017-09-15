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

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Retry;

import com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants;

@RequestScoped
public class BulkheadBean {

    private final AtomicInteger connectBCounter = new AtomicInteger(0);
    private final AtomicInteger connectATokens = new AtomicInteger(0);
    private final AtomicInteger connectBTokens = new AtomicInteger(0);

    private final AtomicInteger connectDCounter = new AtomicInteger(0);
    private final AtomicInteger connectCTokens = new AtomicInteger(0);
    private final AtomicInteger connectDTokens = new AtomicInteger(0);

    @Bulkhead(value = 2, waitingTaskQueue = 2)
    public Boolean connectA(String data) throws InterruptedException {
        System.out.println("connectA starting " + data);
        int token = connectATokens.incrementAndGet();
        try {
            if (token > 2) {
                throw new RuntimeException("Too many threads in connectA[" + data + "]: " + token);
            }
            Thread.sleep(TestConstants.WORK_TIME);
            return Boolean.TRUE;
        } finally {
            connectATokens.decrementAndGet();
            System.out.println("connectA complete " + data);
        }
    }

    @Bulkhead(value = 2, waitingTaskQueue = 2)
    @Retry(maxRetries = 2)
    public Boolean connectB(String data) throws InterruptedException {
        System.out.println("connectB starting " + data);
        int counter = connectBCounter.incrementAndGet();
        int token = connectBTokens.incrementAndGet();
        try {
            if (token > 2) {
                throw new RuntimeException("Too many threads in connectB[" + data + "]: " + token);
            }
            if (counter < 2) {
                throw new RuntimeException("Intentional exception");
            }
            Thread.sleep(TestConstants.WORK_TIME);
            return Boolean.TRUE;
        } finally {
            connectBTokens.decrementAndGet();
            System.out.println("connectB complete " + data);
        }
    }

    @Bulkhead(value = 2, waitingTaskQueue = 2)
    @Retry(maxRetries = 0)
    public Boolean connectC(String data) throws InterruptedException {
        System.out.println("connectC starting " + data);
        int token = connectCTokens.incrementAndGet();
        try {
            if (token > 2) {
                throw new RuntimeException("Too many threads in connectC[" + data + "]: " + token);
            }
            Thread.sleep(TestConstants.WORK_TIME);
            return Boolean.TRUE;
        } finally {
            connectCTokens.decrementAndGet();
            System.out.println("connectC complete " + data);
        }
    }

    @Bulkhead(value = 2, waitingTaskQueue = 2)
    @Retry(maxRetries = 0)
    public Boolean connectD(String data) throws InterruptedException {
        System.out.println("connectD starting " + data);
        int counter = connectDCounter.incrementAndGet();
        int token = connectDTokens.incrementAndGet();
        try {
            if (token > 2) {
                throw new RuntimeException("Too many threads in connectD[" + data + "]: " + token);
            }
            if (counter < 2) {
                throw new RuntimeException("Intentional exception");
            }
            Thread.sleep(TestConstants.WORK_TIME);
            return Boolean.TRUE;
        } finally {
            connectDTokens.decrementAndGet();
            System.out.println("connectD complete " + data);
        }
    }
}
