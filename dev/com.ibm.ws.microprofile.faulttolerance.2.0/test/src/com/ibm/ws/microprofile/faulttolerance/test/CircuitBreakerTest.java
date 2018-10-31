/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.ExecutionException;
import com.ibm.ws.microprofile.faulttolerance.spi.Executor;
import com.ibm.ws.microprofile.faulttolerance.spi.ExecutorBuilder;
import com.ibm.ws.microprofile.faulttolerance.spi.FTExecutionContext;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProvider;
import com.ibm.ws.microprofile.faulttolerance.test.util.TestException;
import com.ibm.ws.microprofile.faulttolerance.test.util.TestFunction;

/**
 *
 */
public class CircuitBreakerTest extends AbstractFTTest {

    @Test
    public void testCircuitBreaker() {
        CircuitBreakerPolicy circuitBreaker = FaultToleranceProvider.newCircuitBreakerPolicy();
        circuitBreaker.setFailureRatio(1.0);
        circuitBreaker.setRequestVolumeThreshold(2);

        ExecutorBuilder<String, String> builder = FaultToleranceProvider.newExecutionBuilder();
        builder.setCircuitBreakerPolicy(circuitBreaker);

        Executor<String> executor = builder.build();

        TestFunction callable = new TestFunction(-1, "testCircuitBreaker");

        String id = "testCircuitBreaker1";
        String executions = "NOT_RUN";
        FTExecutionContext context = executor.newExecutionContext(id, (Method) null, id);
        try {
            executions = executor.execute(callable, context);
            fail("Exception not thrown");
        } catch (ExecutionException t) {
            //expected
            assertTrue(t.getCause() instanceof TestException);
        } finally {
            context.close();
        }
        id = "testCircuitBreaker2";
        context = executor.newExecutionContext(id, (Method) null, id);
        try {
            executions = executor.execute(callable, context);
            fail("Exception not thrown");
        } catch (ExecutionException t) {
            //expected
            assertTrue(t.getCause() instanceof TestException);
        } finally {
            context.close();
        }

        id = "testCircuitBreaker3";
        context = executor.newExecutionContext(id, (Method) null, id);
        try {
            executions = executor.execute(callable, context);
            fail("Exception not thrown");
        } catch (CircuitBreakerOpenException t) {
            //expected
        } finally {
            context.close();
        }

        assertEquals("NOT_RUN", executions);
    }

}
