/*******************************************************************************
 * Copyright (c) 2021,2023 IBM Corporation and others.
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
package concurrent.cdi4.web;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.concurrent.Asynchronous;

import javax.naming.InitialContext;
import javax.naming.NamingException;

public class MyManagedBean {

    @Asynchronous
    public CompletableFuture<Object> asyncLookup(String jndiName) {
        try {
            return Asynchronous.Result.complete(InitialContext.doLookup(jndiName));
        } catch (NamingException x) {
            throw new CompletionException(x);
        }
    }

    /**
     * Exchanges a reference to the running thread for a CountDownLatch,
     * and then awaits the latch.
     * This is useful for giving the test case access to the thread upon which
     * the asynchronous method is running and holding up its completion.
     */
    @Asynchronous(executor = "java:app/env/concurrent/sampleExecutorRef")
    public CompletableFuture<Boolean> exchangeAndAwait(Exchanger<Object> exchanger, long timeout, TimeUnit unit) {
        try {
            CountDownLatch latch = (CountDownLatch) exchanger.exchange(Thread.currentThread(), timeout, unit);
            return Asynchronous.Result.complete(latch.await(timeout, unit));
        } catch (InterruptedException | TimeoutException x) {
            throw new CompletionException(x);
        }
    }
}
