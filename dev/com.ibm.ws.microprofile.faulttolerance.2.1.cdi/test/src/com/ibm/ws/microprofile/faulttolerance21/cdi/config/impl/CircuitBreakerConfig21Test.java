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
package com.ibm.ws.microprofile.faulttolerance21.cdi.config.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.microprofile.config13.impl.Config13ProviderResolverImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProviderResolver;
import com.ibm.ws.microprofile.faulttolerance20.impl.ProviderResolverImpl20;

@SuppressWarnings("restriction")
public class CircuitBreakerConfig21Test {

    @Before
    public void before() {
        ConfigProviderResolver.setInstance(new Config13ProviderResolverImpl());
        FaultToleranceProviderResolver.setInstance(new ProviderResolverImpl20());
    }

    @After
    public void after() {
        ((Config13ProviderResolverImpl) ConfigProviderResolver.instance()).shutdown();
        ConfigProviderResolver.setInstance(null);
        FaultToleranceProviderResolver.setInstance(null);
    }

    @Test
    public void testCBDefaultValues() throws NoSuchMethodException, SecurityException {
        Method dummyTestMethod = CircuitBreakerConfig21Test.class.getMethod("dummyTestMethod");
        CircuitBreaker testCircuitBreaker = dummyTestMethod.getAnnotation(CircuitBreaker.class);
        CircuitBreakerConfig21Impl testCBConfig = new CircuitBreakerConfig21Impl(dummyTestMethod, CircuitBreakerConfig21Test.class, testCircuitBreaker);
        CircuitBreakerPolicy testCBPolicy = testCBConfig.generatePolicy();

        // Test the failOn() method has the correct default value
        assertArrayEquals("FailOn not initialised to the correct default value ", testCBPolicy.getFailOn(), new Class[] { Throwable.class });

        // Test the skipOn() method has the correct default value
        assertArrayEquals("SkipOn not initialised to the correct default value ", testCBPolicy.getSkipOn(), new Class[] {});

    }

    @Test
    public void testSetCBValues() throws NoSuchMethodException, SecurityException {
        Method dummyTestMethod = CircuitBreakerConfig21Test.class.getMethod("dummyTestMethod2");
        CircuitBreaker testCircuitBreaker = dummyTestMethod.getAnnotation(CircuitBreaker.class);
        CircuitBreakerConfig21Impl testCBConfig = new CircuitBreakerConfig21Impl(dummyTestMethod, CircuitBreakerConfig21Test.class, testCircuitBreaker);
        CircuitBreakerPolicy testCBPolicy = testCBConfig.generatePolicy();

        // Test the failOn() method has the correct specified value
        Duration durationToTest = Duration.of(1000, ChronoUnit.MILLIS);
        assertEquals("Delay not set to the correct value ", testCBPolicy.getDelay(), durationToTest);

        // Test the skipOn() method has the correct specified value
        assertArrayEquals("SkipOn not set to correct value", testCBPolicy.getSkipOn(), new Class[] { TestExceptionA.class });
    }

    @Test
    public void testNoMethodInConstructor() throws NoSuchMethodException, SecurityException {
        Method dummyTestMethod = CircuitBreakerConfig21Test.class.getMethod("dummyTestMethod");
        CircuitBreaker testCircuitBreaker = dummyTestMethod.getAnnotation(CircuitBreaker.class);

        // Test CircuitBreakerConfig21 constructor for when no annotatedMethod is passed
        CircuitBreakerConfig21Impl testCBConfig = new CircuitBreakerConfig21Impl(CircuitBreakerConfig21Test.class, testCircuitBreaker);
        CircuitBreakerPolicy testCBPolicy = testCBConfig.generatePolicy();

        assertNotNull("Policy not generated", testCBPolicy.getClass());
        assertArrayEquals("SkipOn not initialised correctly in policy", testCBPolicy.getSkipOn(), new Class[] {});
    }

    @CircuitBreaker
    public void dummyTestMethod() {
        // A default Circuit Breaker
    }

    @CircuitBreaker(delay = 1000, skipOn = TestExceptionA.class)
    public void dummyTestMethod2() {
        // A customised Circuit Breaker
    }

    @SuppressWarnings("serial")
    static class TestExceptionA extends Exception {
    }

}
