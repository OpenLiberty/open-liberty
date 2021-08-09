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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;

import com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants;

@ApplicationScoped
public class BulkheadBean {

    private final AtomicInteger connectATokens = new AtomicInteger(0);

    @Bulkhead(value = 2)
    public Boolean connectA(String data, CountDownLatch wait, CountDownLatch notify) throws InterruptedException {
        System.out.println("connectA starting " + data);
        try {
            notify.countDown(); //notify that we're started
            int token = connectATokens.incrementAndGet();
            if (token > 2) {
                throw new RuntimeException("Too many threads in connectA[" + data + "]: " + token);
            }
            wait.await(TestConstants.TEST_TIMEOUT, TimeUnit.MILLISECONDS); //wait to be released again
            return Boolean.TRUE;
        } finally {
            connectATokens.decrementAndGet();
            System.out.println("connectA complete " + data);
        }
    }

    @Bulkhead(value = 2)
    @CircuitBreaker(failureRatio = 1.0, requestVolumeThreshold = 1)
    public Boolean connectB(String data, CountDownLatch wait, CountDownLatch notify) throws InterruptedException {
        System.out.println("connectB starting " + data);
        try {
            notify.countDown(); //notify that we're started
            wait.await(TestConstants.TEST_TIMEOUT, TimeUnit.MILLISECONDS); //wait to be released again
            return Boolean.TRUE;
        } finally {
            System.out.println("connectB complete " + data);
        }
    }

    @Bulkhead(value = 2)
    @Fallback(fallbackMethod = "fallback")
    public Boolean connectC(String data, CountDownLatch wait, CountDownLatch notify) throws InterruptedException {
        System.out.println("connectC starting " + data);
        try {
            notify.countDown(); //notify that we're started
            wait.await(TestConstants.TEST_TIMEOUT, TimeUnit.MILLISECONDS); //wait to be released again
            return Boolean.TRUE;
        } finally {
            System.out.println("connectC complete " + data);
        }
    }

    public Boolean fallback(String data, CountDownLatch wait, CountDownLatch notify) throws InterruptedException {
        System.out.println("fallback starting " + data);
        try {
            notify.countDown(); //notify that we're started
            wait.await(TestConstants.TEST_TIMEOUT, TimeUnit.MILLISECONDS); //wait to be released again
            return Boolean.FALSE; //fallback returns false!
        } finally {
            System.out.println("fallback complete " + data);
        }
    }
}
