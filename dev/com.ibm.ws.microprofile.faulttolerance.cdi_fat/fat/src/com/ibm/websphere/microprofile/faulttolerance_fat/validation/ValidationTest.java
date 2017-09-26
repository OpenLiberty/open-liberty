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
package com.ibm.websphere.microprofile.faulttolerance_fat.validation;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.websphere.microprofile.faulttolerance_fat.suite.FATSuite;
import com.ibm.ws.fat.util.SharedServer;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@Mode(TestMode.FULL)
public class ValidationTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = FATSuite.MULTI_MODULE_SERVER;

    @Test
    public void testAsyncMethodNotReturningFuture() throws Exception {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(AsyncMethodNotReturningFuture.class)
                        .failsWith("CWMFT5001E")
                        .run();
    }

    @Test
    public void testAsyncClassNotReturningFuture() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(AsyncClassNotReturningFuture.class)
                        .failsWith("CWMFT5001E")
                        .run();
    }

    @Test
    public void testFallbackMethodNotExist() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(FallbackMethodNotExist.class)
                        .failsWith("CWMFT5003E")
                        .run();
    }

    @Test
    public void testFallbackMethodWrongParameters() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(FallbackMethodWrongParameters.class)
                        .failsWith("CWMFT5003E")
                        .run();
    }

    @Test
    public void testFallbackMethodWrongReturnType() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(FallbackMethodWrongReturnType.class)
                        .failsWith("CWMFT5002E")
                        .run();
    }

    @Test
    public void testFallbackHandlerWrongType() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(FallbackHandlerWrongType.class)
                        .failsWith("CWMFT5008E")
                        .run();
    }

    @Test
    public void testFallbackDefinesHandlerAndMethod() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(FallbackDefinesHandlerAndMethod.class)
                        .failsWith("CWMFT5009E")
                        .run();
    }

    @Test
    public void testRetryNegativeDelay() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(RetryNegativeDelay.class)
                        .failsWith("CWMFT5010E")
                        .run();
    }

    @Test
    public void testRetryNegativeDuration() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(RetryNegativeDuration.class)
                        .failsWith("CWMFT5010E")
                        .run();
    }

    @Test
    public void testRetryNegativeJitter() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(RetryNegativeJitter.class)
                        .failsWith("CWMFT5010E")
                        .run();
    }

    @Test
    public void testRetryNegativeRetries() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(RetryNegativeRetries.class)
                        .failsWith("CWMFT5010E")
                        .run();
    }

    @Test
    public void testRetryDelayLongerThanDuration() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(RetryDelayLongerThanDuration.class)
                        .failsWith("CWMFT5017E")
                        .run();
    }

    @Test
    public void testRetryJitterLongerThanDelay() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(RetryJitterLongerThanDelay.class)
                        .succeedsWith("CWMFT5019W")
                        .run();
    }

    @Test
    public void testTimeoutNegative() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(TimeoutNegative.class)
                        .failsWith("CWMFT5011E")
                        .run();
    }

    @Test
    public void testBulkheadConcurrentNegative() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(BulkheadConcurrentNegative.class)
                        .failsWith("CWMFT5016E")
                        .run();
    }

    @Test
    public void testBulkheadQueueNegative() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(BulkheadQueueNegative.class)
                        .failsWith("CWMFT5016E")
                        .run();
    }

    @Test
    public void testCircuitBreakerDelayNegative() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(CircuitBreakerDelayNegative.class)
                        .failsWith("CWMFT5012E")
                        .run();
    }

    @Test
    public void testCircuitBreakerThresholdZero() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(CircuitBreakerThresholdZero.class)
                        .failsWith("CWMFT5014E")
                        .run();
    }

    @Test
    public void testCircuitBreakerThresholdNegative() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(CircuitBreakerThresholdNegative.class)
                        .failsWith("CWMFT5014E")
                        .run();
    }

    @Test
    public void testCircuitBreakerRatioOne() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(CircuitBreakerRatioOne.class)
                        .succeeds()
                        .run();
    }

    @Test
    public void testCircuitBreakerRatioZero() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(CircuitBreakerRatioZero.class)
                        .succeeds()
                        .run();
    }

    @Test
    public void testCircuitBreakerRatioNegative() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(CircuitBreakerRatioNegative.class)
                        .failsWith("CWMFT5013E")
                        .run();
    }

    @Test
    public void testCircuitBreakerRatioTooLarge() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(CircuitBreakerRatioTooLarge.class)
                        .failsWith("CWMFT5013E")
                        .run();
    }

    @Test
    public void testCircuitBreakerSuccessThresholdZero() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(CircuitBreakerSuccessThresholdZero.class)
                        .failsWith("CWMFT5015E")
                        .run();
    }

    @Test
    public void testCircuitBreakerSuccessThresholdNegative() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(CircuitBreakerSuccessThresholdNegative.class)
                        .failsWith("CWMFT5015E")
                        .run();
    }

    @Test
    public void testCircuitBreakerFailOnEmpty() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(CircuitBreakerFailOnEmpty.class)
                        .failsWith("CWMFT5018E")
                        .run();
    }

}
