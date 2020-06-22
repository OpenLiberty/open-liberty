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
package com.ibm.ws.microprofile.faulttolerance_fat.cdi;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.Test;

import com.ibm.websphere.microprofile.faulttolerance_fat.suite.BasicTest;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncRunnerBean;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.BulkheadBean;

import componenttest.app.FATServlet;

/**
 * Servlet implementation class Test
 */
@WebServlet("/bulkhead")
public class SyncBulkheadServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    BulkheadBean bean1;

    @Inject
    AsyncRunnerBean runner;

    @BasicTest
    @Test
    public void testSyncBulkheadSmall() throws Exception {
        CountDownLatch notify = new CountDownLatch(2); //the tasks notify that they are running
        CountDownLatch wait = new CountDownLatch(1); //and then wait to be released

        //connectA has a poolSize of 2
        //first two should be run straight away, in parallel
        Future<Boolean> future1 = runner.call(() -> {
            return bean1.connectA("One", wait, notify);
        });
        Future<Boolean> future2 = runner.call(() -> {
            return bean1.connectA("Two", wait, notify);
        });
        //wait for the first two to be properly started
        notify.await(TestConstants.TEST_TIMEOUT, TimeUnit.MILLISECONDS);

        //next two should be reject because the bulkhead is full
        Future<Boolean> future3 = runner.call(() -> {
            return bean1.connectA("Three", wait, notify);
        });
        Future<Boolean> future4 = runner.call(() -> {
            return bean1.connectA("Four", wait, notify);
        });

        try {
            future3.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS);
            throw new AssertionError("Exception not thrown");
        } catch (ExecutionException e) {
            //expected
            if (!(e.getCause() instanceof BulkheadException)) {
                throw new AssertionError("Cause was not a BulkheadException: " + e);
            }
        }

        try {
            future4.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS);
            throw new AssertionError("Exception not thrown");
        } catch (ExecutionException e) {
            //expected
            if (!(e.getCause() instanceof BulkheadException)) {
                throw new AssertionError("Cause was not a BulkheadException: " + e);
            }
        }

        wait.countDown(); //release the first two to complete

    }

    @Test
    public void testSyncBulkheadCircuitBreaker() throws Exception {
        CountDownLatch notify = new CountDownLatch(2); //the tasks notify that they are running
        CountDownLatch wait = new CountDownLatch(1); //and then wait to be released

        //connectA has a poolSize of 2
        //first two should be run straight away, in parallel
        Future<Boolean> future1 = runner.call(() -> {
            return bean1.connectB("One", wait, notify);
        });
        Future<Boolean> future2 = runner.call(() -> {
            return bean1.connectB("Two", wait, notify);
        });
        //wait for the first two to be properly started
        notify.await(TestConstants.TEST_TIMEOUT, TimeUnit.MILLISECONDS);

        //next one should be reject because the bulkhead is full
        Future<Boolean> future3 = runner.call(() -> {
            return bean1.connectB("Three", wait, notify);
        });
        try {
            future3.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS);
            throw new AssertionError("Exception not thrown");
        } catch (ExecutionException e) {
            //expected
            if (!(e.getCause() instanceof BulkheadException)) {
                throw new AssertionError("Cause was not a BulkheadException: " + e);
            }
        }

        //circuit should now be open so this one should fail with a CircuitBreakerOpenException
        Future<Boolean> future4 = runner.call(() -> {
            return bean1.connectB("Four", wait, notify);
        });

        try {
            future4.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS);
            throw new AssertionError("Exception not thrown");
        } catch (ExecutionException e) {
            //expected
            if (!(e.getCause() instanceof CircuitBreakerOpenException)) {
                throw new AssertionError("Cause was not a CircuitBreakerOpenException: " + e);
            }
        }

        wait.countDown(); //release the first two to complete

    }

    @Test
    public void testSyncBulkheadFallback() throws Exception {
        CountDownLatch notify = new CountDownLatch(2); //the tasks notify that they are running
        CountDownLatch wait = new CountDownLatch(1); //and then wait to be released

        //connectA has a poolSize of 2
        //first two should be run straight away, in parallel
        Future<Boolean> future1 = runner.call(() -> {
            return bean1.connectC("One", wait, notify);
        });
        Future<Boolean> future2 = runner.call(() -> {
            return bean1.connectC("Two", wait, notify);
        });
        //wait for the first two to be properly started
        notify.await(TestConstants.TEST_TIMEOUT, TimeUnit.MILLISECONDS);

        CountDownLatch notify2 = new CountDownLatch(2); //the tasks notify that they are running
        CountDownLatch wait2 = new CountDownLatch(1); //and then wait to be released

        //next two should be reject because the bulkhead is full ... but call the fallback instead
        Future<Boolean> future3 = runner.call(() -> {
            return bean1.connectC("Three", wait2, notify2);
        });
        Future<Boolean> future4 = runner.call(() -> {
            return bean1.connectC("Four", wait2, notify2);
        });

        notify2.await(TestConstants.TEST_TIMEOUT, TimeUnit.MILLISECONDS); //wait for the fallbacks to be called
        wait2.countDown(); //release the fallback calls

        Boolean result3 = future3.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS);
        if (result3 != Boolean.FALSE) { //fallback returns false, normal methods return true
            throw new AssertionError("Result was not true");
        }
        Boolean result4 = future4.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS);
        if (result4 != Boolean.FALSE) { //fallback returns false, normal methods return true
            throw new AssertionError("Result was not true");
        }

        wait.countDown(); //release the first two to complete

    }

}
