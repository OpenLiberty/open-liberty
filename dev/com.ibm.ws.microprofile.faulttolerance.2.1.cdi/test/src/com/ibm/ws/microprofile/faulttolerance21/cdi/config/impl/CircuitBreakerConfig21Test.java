/*******************************************************************************
 * Copyright (c) 2019, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance21.cdi.config.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.config.xml.ConfigVariables;
import com.ibm.ws.kernel.service.util.ServiceCaller;
import com.ibm.ws.microprofile.config13.impl.Config13ProviderResolverImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProviderResolver;
import com.ibm.ws.microprofile.faulttolerance20.impl.ProviderResolverImpl20;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import io.openliberty.microprofile.config.internal.serverxml.OSGiConfigUtils;
import io.openliberty.microprofile.config.internal.serverxml.TestServiceCaller;

@SuppressWarnings("restriction")
public class CircuitBreakerConfig21Test {

    @Before
    public void before() throws Exception {
        setOSGiCallers(
            new TestServiceCaller<CDIService>(CDIService.class),
            new TestServiceCaller<ConfigVariables>(ConfigVariables.class),
            new TestServiceCaller<VariableRegistry>(VariableRegistry.class));
        ConfigProviderResolver.setInstance(new Config13ProviderResolverImpl());
        FaultToleranceProviderResolver.setInstance(new ProviderResolverImpl20());
    }

    @After
    public void after() throws Exception {
        ((Config13ProviderResolverImpl) ConfigProviderResolver.instance()).shutdown();
        ConfigProviderResolver.setInstance(null);
        FaultToleranceProviderResolver.setInstance(null);
        setOSGiCallers(
            new ServiceCaller<CDIService>(OSGiConfigUtils.class, CDIService.class),
            new ServiceCaller<ConfigVariables>(OSGiConfigUtils.class, ConfigVariables.class),
            new ServiceCaller<VariableRegistry>(OSGiConfigUtils.class, VariableRegistry.class));
    }

    private static void setOSGiCallers(ServiceCaller<CDIService> cdiCaller,
                                       ServiceCaller<ConfigVariables> configVarsCaller,
                                       ServiceCaller<VariableRegistry> varRegistryCaller) throws Exception {
        Field cdiField = OSGiConfigUtils.class.getDeclaredField("cdiServiceCaller");
        cdiField.setAccessible(true);
        cdiField.set(null, cdiCaller);

        Field configVarsField = OSGiConfigUtils.class.getDeclaredField("configVariablesCaller");
        configVarsField.setAccessible(true);
        configVarsField.set(null, configVarsCaller);

        Field varRegField = OSGiConfigUtils.class.getDeclaredField("variableRegistryCaller");
        varRegField.setAccessible(true);
        varRegField.set(null, varRegistryCaller);
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
