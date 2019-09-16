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

import static com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants.EXPECTED_TIMEOUT;
import static com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants.NEGATIVE_TIMEOUT;
import static com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants.NON_INTERRUPTABLE_TIMEOUT;
import static com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants.TEST_TIMEOUT;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants;
import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;
import com.ibm.ws.microprofile.faulttolerance_fat.util.Connection;

@RequestScoped
public class TimeoutBean {

    private int connectCCalls = 0;
    private int connectDCalls = 0;

    @Timeout
    public Connection connectA() throws ConnectException {
        try {
            Thread.sleep(20000);
            throw new ConnectException("Timeout did not interrupt");
        } catch (InterruptedException e) {
            //expected
            System.out.println("TimeoutBean Interrupted");
        }
        return null;

    }

    @Timeout
    public Connection connectB() throws ConnectException {
        throw new ConnectException("A simple exception");
    }

    @Timeout(EXPECTED_TIMEOUT)
    @Retry(maxRetries = 7, maxDuration = 30, durationUnit = ChronoUnit.SECONDS)
    public Connection connectC() throws InterruptedException, ConnectException {
        connectCCalls++;
        Thread.sleep(TEST_TIMEOUT);
        Connection result = new Connection() {
            @Override
            public String getData() {
                return "connectC";
            }
        };
        return result; // Should not be seen because timeout happens first
    }

    @Asynchronous
    @Timeout(EXPECTED_TIMEOUT)
    @Retry(maxRetries = 7, maxDuration = 30, durationUnit = ChronoUnit.SECONDS)
    public Future<Connection> connectD() throws InterruptedException, ConnectException {
        connectDCalls++;
        Thread.sleep(TEST_TIMEOUT);
        Connection result = new Connection() {
            @Override
            public String getData() {
                return "connectC";
            }
        };
        return CompletableFuture.completedFuture(result); // Should not be seen because timeout happens first
    }

    public int getConnectCCalls() {
        return connectCCalls;
    }

    public int getConnectDCalls() {
        return connectDCalls;
    }

    /**
     * Just sleeps for longer than the default 1000ms. Should not time out because Timeout.value = 0 -> no timeout.
     */
    @Timeout(0)
    public void connectE() throws InterruptedException {
        Thread.sleep(2000);
    }

    @Timeout(EXPECTED_TIMEOUT)
    @Fallback(MyFallbackHandler.class)
    public Connection connectF() throws InterruptedException, ConnectException {
        Thread.sleep(TEST_TIMEOUT);
        throw new ConnectException("connectF");
    }

    /**
     * Set the Timeout value to 5 seconds - which would lead to test failure - but this method's config
     * will be overridden to 500 millis in microprofile-config.properties so that the bean will
     * generate a TimeoutException as expected by the test.
     */
    @Timeout(5000)
    public Connection connectG() throws ConnectException {
        try {
            Thread.sleep(2000);
            throw new ConnectException("Timeout did not interrupt");
        } catch (InterruptedException e) {
            //expected
            System.out.println("TimeoutBean Interrupted");
        }
        return null;

    }

    /**
     * Used for testing timeout with workloads which are not interruptable
     *
     * @param milliseconds number of milliseconds to busy wait for
     */
    private void busyWait(long milliseconds) {
        long duration = Duration.ofMillis(milliseconds).toNanos();
        long start = System.nanoTime();
        while (System.nanoTime() - start < duration) {
            // Do nothing
        }
    }

    /**
     * Method is not interruptible but will time out
     */
    @Timeout(EXPECTED_TIMEOUT)
    public void busyWaitTimeout() {
        busyWait(NON_INTERRUPTABLE_TIMEOUT);
    }

    /**
     * Method is not interruptible and will not time out
     */
    @Timeout(NEGATIVE_TIMEOUT) // Use negative timeout here because the test is going to wait for this time to ensure an interrupt is not fired
    public void busyWaitNoTimeout() {
        busyWait(TestConstants.EXPECTED_TIMEOUT);
    }
}
