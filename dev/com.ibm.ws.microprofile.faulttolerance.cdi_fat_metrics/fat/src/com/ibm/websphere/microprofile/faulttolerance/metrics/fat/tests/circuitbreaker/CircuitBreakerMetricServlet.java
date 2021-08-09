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
package com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.circuitbreaker;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/circuit-breaker-metric")
public class CircuitBreakerMetricServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    private CircuitBreakerMetricBean testBean;

    @Inject
    private MetricRegistry registry;

    @Test
    public void testCircuitBreakerMetrics() {
        String methodName = CircuitBreakerMetricBean.class.getCanonicalName() + ".doWorkWithExeception";
        Counter callsSucceeded = registry.counter("ft." + methodName + ".circuitbreaker.callsSucceeded.total");
        Counter callsFailed = registry.counter("ft." + methodName + ".circuitbreaker.callsFailed.total");
        Counter callsPrevented = registry.counter("ft." + methodName + ".circuitbreaker.callsPrevented.total");

        // Note: Circuit Breaker under test will open if 2/4 requests are failures
        // Only requests which throw a TestException are considered failures

        // Failing call (exception is in failOn)
        try {
            testBean.doWorkWithExeception(new TestException());
        } catch (TestException e) {
        }
        assertEquals("incorrect calls succeeded", 0, callsSucceeded.getCount());
        assertEquals("incorrect calls failed", 1, callsFailed.getCount());
        assertEquals("incorrect calls prevented", 0, callsPrevented.getCount());

        // Succeeding call that threw exception (exception is not in failOn)
        try {
            testBean.doWorkWithExeception(new RuntimeException());
        } catch (RuntimeException e) {
        }
        assertEquals("incorrect calls succeeded", 1, callsSucceeded.getCount());
        assertEquals("incorrect calls failed", 1, callsFailed.getCount());
        assertEquals("incorrect calls prevented", 0, callsPrevented.getCount());

        // Succeeding call (no exception)
        testBean.doWorkWithExeception(null);
        assertEquals("incorrect calls succeeded", 2, callsSucceeded.getCount());
        assertEquals("incorrect calls failed", 1, callsFailed.getCount());
        assertEquals("incorrect calls prevented", 0, callsPrevented.getCount());

        // Failing call (to open the breaker)
        try {
            testBean.doWorkWithExeception(new TestException());
        } catch (TestException e) {
        }
        assertEquals("incorrect calls succeeded", 2, callsSucceeded.getCount());
        assertEquals("incorrect calls failed", 2, callsFailed.getCount());
        assertEquals("incorrect calls prevented", 0, callsPrevented.getCount());

        // Prevented call (breaker is open)
        try {
            testBean.doWorkWithExeception(new TestException());
        } catch (CircuitBreakerOpenException e) {
        }
        assertEquals("incorrect calls succeeded", 2, callsSucceeded.getCount());
        assertEquals("incorrect calls failed", 2, callsFailed.getCount());
        assertEquals("incorrect calls prevented", 1, callsPrevented.getCount());
    }

}
