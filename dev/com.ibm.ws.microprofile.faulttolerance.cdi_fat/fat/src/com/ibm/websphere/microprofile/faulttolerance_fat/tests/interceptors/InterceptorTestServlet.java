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
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.interceptors;

import static com.ibm.websphere.microprofile.faulttolerance_fat.tests.interceptors.InterceptionRecorder.InterceptionThread.ASYNC_THREAD;
import static com.ibm.websphere.microprofile.faulttolerance_fat.tests.interceptors.InterceptionRecorder.InterceptionThread.CALLER_THREAD;
import static com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants.TEST_TIMEOUT;
import static com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants.TIMEOUT;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.Test;

import com.ibm.websphere.microprofile.faulttolerance_fat.tests.interceptors.InterceptionRecorder.InterceptionEntry;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants;

import componenttest.app.FATServlet;

@WebServlet("/InterceptorTest")
public class InterceptorTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    private InterceptorBean bean;

    @Resource
    private ExecutorService executor;

    /**
     * This is a basic test to ensure that interceptors work in normal circumstances
     */
    @Test
    public void testInterceptorRecorder() {
        InterceptionRecorder recorder = new InterceptionRecorder();
        bean.serviceInterceptors(recorder);

        assertThat(recorder.getEntries(), contains(new InterceptionEntry(EarlyInterceptor.class, CALLER_THREAD),
                                                   new InterceptionEntry(LateInterceptor.class, CALLER_THREAD)));
    }

    @Test
    public void testInterceptorAsync() throws Exception {
        InterceptionRecorder recorder = new InterceptionRecorder();
        bean.serviceAsyncInterceptors(recorder).get(TIMEOUT, MILLISECONDS);

        assertThat(recorder.getEntries(), contains(new InterceptionEntry(EarlyInterceptor.class, CALLER_THREAD),
                                                   new InterceptionEntry(LateInterceptor.class, ASYNC_THREAD)));
    }

    @Test
    public void testInterceptorRetry() {
        InterceptionRecorder recorder = new InterceptionRecorder();
        bean.serviceRetryInterceptors(recorder, new AtomicInteger(0));

        // Method should fail twice and then succeed, therefore the late interceptor should record three times
        assertThat(recorder.getEntries(), contains(new InterceptionEntry(EarlyInterceptor.class, CALLER_THREAD),
                                                   new InterceptionEntry(LateInterceptor.class, CALLER_THREAD),
                                                   new InterceptionEntry(LateInterceptor.class, CALLER_THREAD),
                                                   new InterceptionEntry(LateInterceptor.class, CALLER_THREAD)));
    }

    @Test
    public void testInterceptorCircuitBreaker() {
        // Run two failing calls to open the circuit breaker
        expectException(RuntimeException.class, () -> bean.serviceCircuitBreakerInterceptors(new InterceptionRecorder(), true));
        expectException(RuntimeException.class, () -> bean.serviceCircuitBreakerInterceptors(new InterceptionRecorder(), true));

        InterceptionRecorder recorder = new InterceptionRecorder();
        expectException(CircuitBreakerOpenException.class, () -> bean.serviceCircuitBreakerInterceptors(recorder, false));

        // LateInterceptor should not run because CircuitBreakerOpenException is thrown first
        assertThat(recorder.getEntries(), contains(new InterceptionEntry(EarlyInterceptor.class, CALLER_THREAD)));
    }

    @Test
    public void testInterceptorBulkhead() throws Exception {
        CompletableFuture<Void> waitLatch = new CompletableFuture<>();
        CompletableFuture<Void> startLatch = new CompletableFuture<>();
        try {
            // First submit one task to fill the bulkhead
            executor.submit(() -> bean.serviceBulkheadInterceptors(new InterceptionRecorder(), waitLatch, startLatch));
            startLatch.get(TEST_TIMEOUT, MILLISECONDS);

            // Now check the second task which should get a bulkheadException
            AtomicReference<InterceptionRecorder> recorderHolder = new AtomicReference<InterceptionRecorder>(null);
            Future<?> future = executor.submit(() -> {
                // InteceptionRecorder captures the caller thread when it's created, so we need to create it inside here
                InterceptionRecorder recorder = new InterceptionRecorder();
                recorderHolder.set(recorder);

                bean.serviceBulkheadInterceptors(recorder, waitLatch, startLatch);
            });
            expectException(BulkheadException.class, future);

            // LateInterceptor should not run because BulkheadException is thrown first
            assertThat(recorderHolder.get().getEntries(), contains(new InterceptionEntry(EarlyInterceptor.class, CALLER_THREAD)));
        } finally {
            waitLatch.complete(null);
        }
    }

    private void expectException(Class<?> exceptionClazz, Runnable task) {
        try {
            task.run();
            fail("Expected " + exceptionClazz + " was not thrown");
        } catch (Exception e) {
            assertThat("Exception thrown", e, instanceOf(exceptionClazz));
        }
    }

    private void expectException(Class<?> exceptionClazz, Future<?> future) throws Exception {
        try {
            future.get(TestConstants.TEST_TIMEOUT, MILLISECONDS);
            fail("Expected " + exceptionClazz + " was not thrown");
        } catch (ExecutionException e) {
            assertThat("Exception thrown", e.getCause(), instanceOf(exceptionClazz));
        }
    }

}
