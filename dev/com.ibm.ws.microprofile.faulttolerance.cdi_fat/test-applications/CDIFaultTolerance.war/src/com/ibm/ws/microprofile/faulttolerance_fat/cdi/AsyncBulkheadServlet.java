/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance_fat.cdi;

import static com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants.TEST_TIMEOUT;
import static com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.FutureAsserts.assertFutureHasResult;
import static com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.FutureAsserts.assertFutureThrowsException;
import static com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.SyntheticTask.InterruptionAction.RETURN;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.Test;

import com.ibm.websphere.microprofile.faulttolerance_fat.suite.BasicTest;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncBulkheadBean;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncBulkheadBean2;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.SyntheticTask;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.SyntheticTaskManager;

import componenttest.app.FATServlet;

/**
 * Servlet implementation class Test
 */
@WebServlet("/asyncbulkhead")
public class AsyncBulkheadServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    AsyncBulkheadBean bean1;
    @Inject
    AsyncBulkheadBean2 bean4;

    public SyntheticTaskManager syntheticTaskManager = new SyntheticTaskManager();

    @BasicTest
    @Test
    public void testAsyncBulkheadSmall() throws Exception {

        syntheticTaskManager.runTest(() -> {
            // First two tasks should start
            SyntheticTask<Void> task1 = syntheticTaskManager.newTask();
            SyntheticTask<Void> task2 = syntheticTaskManager.newTask();
            Future<Void> future1 = bean1.runTask(task1);
            Future<Void> future2 = bean1.runTask(task2);
            task1.assertStarts();
            task2.assertStarts();

            // Next two tasks should queue
            SyntheticTask<Void> task3 = syntheticTaskManager.newTask();
            SyntheticTask<Void> task4 = syntheticTaskManager.newTask();
            Future<Void> future3 = bean1.runTask(task3);
            Future<Void> future4 = bean1.runTask(task4);
            SyntheticTask.assertAllNotStarting(task3, task4);

            // Next task should be rejected
            try {
                SyntheticTask<Void> task5 = syntheticTaskManager.newTask();
                Future<Void> future5 = bean1.runTask(task5);
                future5.get(TEST_TIMEOUT, MILLISECONDS);
                fail("BulkheadException not thrown when bulkhead is full");
            } catch (BulkheadException e) {
                // Expected for 1.0
            } catch (ExecutionException e) {
                // Expected for 2.0
                assertThat(e.getCause(), instanceOf(BulkheadException.class));
            }

            task1.complete();
            task2.complete();
            task3.complete();
            task4.complete();
            assertFutureHasResult(future1, null);
            assertFutureHasResult(future2, null);
            assertFutureHasResult(future3, null);
            assertFutureHasResult(future4, null);
        });
    }

    @Test
    public void testAsyncBulkheadTimeout() throws Exception {

        syntheticTaskManager.runTest(() -> {
            // runTaskWithTimeout has a timeout of 2s
            // These tasks should run in parallel but should time out after 2s

            SyntheticTask<Void> task1 = syntheticTaskManager.newTask();
            task1.onInterruption(RETURN);
            SyntheticTask<Void> task2 = syntheticTaskManager.newTask();
            task2.onInterruption(RETURN);
            Future<Void> future1 = bean1.runTaskWithTimeout(task1);
            Future<Void> future2 = bean1.runTaskWithTimeout(task2);

            // Check both tasks start
            task1.assertStarts();
            task2.assertStarts();

            // Check they're running in parallel
            assertFalse(future1.isDone());
            assertFalse(future2.isDone());

            // Check they eventually time out
            assertFutureThrowsException(future1, org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException.class);
            assertFutureThrowsException(future2, org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException.class);

            SyntheticTask<Void> task3 = syntheticTaskManager.newTask();
            Future<Void> future3 = bean1.runTaskWithTimeout(task3);
            task3.complete();
            assertFutureHasResult(future3, null);
        });
    }

    /**
     * Test overriding method level Bulkhead annotation attributes through config.
     */
    @Test
    public void testAsyncBulkheadSmallConfig() throws Exception {
        syntheticTaskManager.runTest(() -> {
            // Bulkhead size is set to 3 in annotation but overriden to 2 in config
            // First two tasks should start
            SyntheticTask<Void> task1 = syntheticTaskManager.newTask();
            SyntheticTask<Void> task2 = syntheticTaskManager.newTask();
            bean1.runTaskWithConfig(task1);
            bean1.runTaskWithConfig(task2);
            task1.assertStarts();
            task2.assertStarts();

            // Third task should queue due to value set in config
            SyntheticTask<Void> task3 = syntheticTaskManager.newTask();
            bean1.runTaskWithConfig(task3);
            task3.assertNotStarting();
        });
    }

    /**
     * Test overriding class level Bulkhead annotation attributes through config.
     */
    @Test
    public void testAsyncBulkheadSmallClassScopeConfig() throws Exception {
        syntheticTaskManager.runTest(() -> {
            // Bulkhead size is set to 3 in annotation but overriden to 2 in config
            // First two tasks should start
            SyntheticTask<Void> task1 = syntheticTaskManager.newTask();
            SyntheticTask<Void> task2 = syntheticTaskManager.newTask();
            bean4.runTask(task1);
            bean4.runTask(task2);
            task1.assertStarts();
            task2.assertStarts();

            // Third task should queue due to value set in config
            SyntheticTask<Void> task3 = syntheticTaskManager.newTask();
            bean4.runTask(task3);
            task3.assertNotStarting();
        });
    }

    @Test
    public void testAsyncRetryAroundBulkhead() throws Exception {
        // This test uses sleeps because FT 1.x blocks while retrying around a bulkhead exception
        // which means we can't easily complete one of running tasks while the retry for
        // the last task is taking place.

        // First two tasks should run
        Future<Void> future1 = bean1.runTaskWithSlowRetries(this::sleep);
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);
        Future<Void> future2 = bean1.runTaskWithSlowRetries(this::sleep);
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);

        // Second two tasks should queue
        Future<Void> future3 = bean1.runTaskWithSlowRetries(this::sleep);
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);
        Future<Void> future4 = bean1.runTaskWithSlowRetries(this::sleep);
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);

        // Fifth task should be rejected by the bulkhead but retry until it can run
        Future<Void> future5 = bean1.runTaskWithSlowRetries(this::sleep);

        assertFutureHasResult(future1, null);
        assertFutureHasResult(future2, null);
        assertFutureHasResult(future3, null);
        assertFutureHasResult(future4, null);
        assertFutureHasResult(future5, null);
    }

    private Void sleep() throws Exception {
        Thread.sleep(1000);
        return null;
    }

    @Test
    public void testAsyncRetryAroundBulkheadFail() throws Exception {
        syntheticTaskManager.runTest(() -> {
            // First two tasks should start
            SyntheticTask<Void> task1 = syntheticTaskManager.newTask();
            SyntheticTask<Void> task2 = syntheticTaskManager.newTask();
            Future<Void> future1 = bean1.runTaskWithFastRetries(task1);
            Future<Void> future2 = bean1.runTaskWithFastRetries(task2);
            task1.assertStarts();
            task2.assertStarts();

            // Next two tasks should queue
            SyntheticTask<Void> task3 = syntheticTaskManager.newTask();
            SyntheticTask<Void> task4 = syntheticTaskManager.newTask();
            Future<Void> future3 = bean1.runTaskWithFastRetries(task3);
            Future<Void> future4 = bean1.runTaskWithFastRetries(task4);
            SyntheticTask.assertAllNotStarting(task3, task4);

            // Next task should retry but still be rejected since none of the running tasks finish
            try {
                SyntheticTask<Void> task5 = syntheticTaskManager.newTask();
                Future<Void> future5 = bean1.runTaskWithFastRetries(task5);
                future5.get(TEST_TIMEOUT, MILLISECONDS);
                fail("BulkheadException not thrown when bulkhead is full");
            } catch (BulkheadException e) {
                // Expected for 1.0
            } catch (ExecutionException e) {
                // Expected for 2.0
                assertThat(e.getCause(), instanceOf(BulkheadException.class));
            }

            task1.complete();
            task2.complete();
            task3.complete();
            task4.complete();
            assertFutureHasResult(future1, null);
            assertFutureHasResult(future2, null);
            assertFutureHasResult(future3, null);
            assertFutureHasResult(future4, null);
        });
    }

}
