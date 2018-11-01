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
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.time.Duration;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance.spi.Executor;
import com.ibm.ws.microprofile.faulttolerance.spi.ExecutorBuilder;
import com.ibm.ws.microprofile.faulttolerance.spi.FTExecutionContext;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProvider;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;
import com.ibm.ws.microprofile.faulttolerance.test.util.TestFunction;

/**
 *
 */
public class TimeoutTest extends AbstractFTTest {

    @Test
    public void testTimeout() {
        TimeoutPolicy timeout = FaultToleranceProvider.newTimeoutPolicy();
        timeout.setTimeout(Duration.ofMillis(500));

        ExecutorBuilder<String, String> builder = FaultToleranceProvider.newExecutionBuilder();
        builder.setTimeoutPolicy(timeout);

        Executor<String> executor = builder.build();

        String id = "testTimeout";
        TestFunction callable = new TestFunction(Duration.ofMillis(1000), id);

        String executions = "NOT_RUN";
        FTExecutionContext context = executor.newExecutionContext(id, (Method) null, id);
        try {
            executions = executor.execute(callable, context);
            fail("Exception not thrown");
        } catch (TimeoutException t) {
            //expected
        } finally {
            context.close();
        }
        assertEquals("NOT_RUN", executions);
    }

}
