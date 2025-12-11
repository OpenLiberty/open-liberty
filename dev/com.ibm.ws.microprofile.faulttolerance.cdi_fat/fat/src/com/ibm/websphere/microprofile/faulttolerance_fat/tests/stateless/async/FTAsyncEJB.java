/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.async;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ejb.Stateless;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;

import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.TestException;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.BarrierFactory.Barrier;

@Stateless
public class FTAsyncEJB {

    @Asynchronous // FT Asynchronous, not EJB Asynchronous
    public Future<Void> test(AtomicInteger counter, Barrier barrier) {
        counter.incrementAndGet();
        barrier.await();
        return CompletableFuture.completedFuture(null);
    }

    @Asynchronous // FT Asynchronous, not EJB Asynchronous
    @Retry(maxRetries = 3, jitter = 0)
    public Future<Void> testRetry(AtomicInteger failureCountdown) throws TestException {
        if (failureCountdown.getAndDecrement() > 0) {
            throw new TestException();
        }
        return CompletableFuture.completedFuture(null);
    }
}
