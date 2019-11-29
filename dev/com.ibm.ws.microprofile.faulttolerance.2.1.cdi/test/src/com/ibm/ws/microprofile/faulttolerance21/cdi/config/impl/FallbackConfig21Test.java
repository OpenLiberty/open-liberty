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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.microprofile.config13.impl.Config13ProviderResolverImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProviderResolver;
import com.ibm.ws.microprofile.faulttolerance20.impl.ProviderResolverImpl20;

@SuppressWarnings("restriction")
public class FallbackConfig21Test {

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
    public void testFBDefaultValues() throws NoSuchMethodException, SecurityException {
        FallbackPolicy testFBPolicy = generateTestPolicy("dummyTestFallback");

        assertArrayEquals("ApplyOn not initialised to the correct default value ", testFBPolicy.getApplyOn(), new Class[] { Throwable.class });

        assertArrayEquals("SkipOn not initialised to the correct default value ", testFBPolicy.getSkipOn(), new Class[] {});

    }

    @Test
    public void testSetFBValues() throws NoSuchMethodException, SecurityException {
        FallbackPolicy testFBPolicy = generateTestPolicy("dummyTestFallback2");

        assertArrayEquals("ApplyOn not set to correct value", testFBPolicy.getApplyOn(), new Class[] { TestExceptionA.class });

        assertArrayEquals("SkipOn not set to correct value", testFBPolicy.getSkipOn(), new Class[] { TestExceptionB.class });
    }

    @Test
    public void testSettingMultipleFBValues() throws NoSuchMethodException, SecurityException {
        FallbackPolicy testFBPolicy = generateTestPolicy("dummyTestFallback3");

        assertArrayEquals("ApplyOn not set to correct value", testFBPolicy.getApplyOn(), new Class[] { TestExceptionA.class, TestExceptionB.class });

        assertArrayEquals("SkipOn not set to correct value", testFBPolicy.getSkipOn(), new Class[] { TestExceptionA.class, TestExceptionB.class });
    }

    private FallbackPolicy generateTestPolicy(String fallbackTestFallbackName) throws NoSuchMethodException, SecurityException {
        Method dummyTestMethod = FallbackConfig21Test.class.getDeclaredMethod(fallbackTestFallbackName);
        Fallback testFallback = dummyTestMethod.getAnnotation(Fallback.class);
        FallbackConfig21Impl testFBConfig = new FallbackConfig21Impl(dummyTestMethod, FallbackConfig21Test.class, testFallback);

        InvocationContext dummyContext = new DummyInvocationContext(FallbackConfig21Test.class.getMethod("dummyFallbackMethod"), null);

        FallbackPolicy testFBPolicy = testFBConfig.generatePolicy(dummyContext, null);
        return testFBPolicy;
    }

    @Fallback(fallbackMethod = "dummyFallbackMethod")
    private void dummyTestFallback() {
        // A default Fallback
    }

    @Fallback(fallbackMethod = "dummyFallbackMethod", applyOn = TestExceptionA.class, skipOn = TestExceptionB.class)
    private void dummyTestFallback2() {
        // A customised Fallback
    }

    @Fallback(fallbackMethod = "dummyFallbackMethod", applyOn = { TestExceptionA.class, TestExceptionB.class }, skipOn = { TestExceptionA.class, TestExceptionB.class })
    private void dummyTestFallback3() {
        // A customised Fallback
    }

    @SuppressWarnings("serial")
    private static class TestExceptionA extends Exception {
    }

    @SuppressWarnings("serial")
    private static class TestExceptionB extends Exception {
    }

    public String dummyFallbackMethod() {
        return "A Fallback message";
    }

    private static class DummyInvocationContext implements InvocationContext {

        Method method;
        Object target;

        public DummyInvocationContext(Method testMethod, Object testObject) {
            this.method = testMethod;
            this.target = testObject;
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Object getTarget() {
            return target;
        }

        @Override
        public Constructor<?> getConstructor() {
            return null;
        }

        @Override
        public Map<String, Object> getContextData() {
            return null;
        }

        @Override
        public Object[] getParameters() {
            return null;
        }

        @Override
        public Object getTimer() {
            return null;
        }

        @Override
        public Object proceed() throws Exception {
            return null;
        }

        @Override
        public void setParameters(Object[] arg0) {
        }

    }

}
