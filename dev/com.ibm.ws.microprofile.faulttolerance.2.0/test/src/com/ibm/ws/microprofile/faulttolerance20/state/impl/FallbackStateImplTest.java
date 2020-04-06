/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.state.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance.impl.policy.FallbackPolicyImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance.utils.DummyMetricRecorder;
import com.ibm.ws.microprofile.faulttolerance20.impl.MethodResult;

@SuppressWarnings("restriction") // Unit test accesses non-exported *PolicyImpl classes
public class FallbackStateImplTest {

    private static MetricRecorder dummyMetrics = DummyMetricRecorder.get();

    @Test
    public <R> void testFallback() throws InterruptedException, InstantiationException, IllegalAccessException {
        FallbackPolicyImpl policy = new FallbackPolicyImpl();
        FallbackStateImpl state = new FallbackStateImpl(policy, dummyMetrics);

        MethodResult<R> resultWithException = MethodResult.failure(new RuntimeException());
        assertTrue("Fallback should be applied if an exception is thrown",
                   state.shouldApplyFallback(resultWithException));

        MethodResult<R> resultNoException = MethodResult.success(null);
        assertFalse("Fallback should NOT be applied if an exception is NOT thrown",
                    state.shouldApplyFallback(resultNoException));
    }

    @SuppressWarnings("unchecked")
    @Test
    public <R> void testFallbackApplyOn() throws InterruptedException, InstantiationException, IllegalAccessException {
        FallbackPolicyImpl policy = new FallbackPolicyImpl();
        policy.setApplyOn(TestExceptionA.class);
        FallbackStateImpl state = new FallbackStateImpl(policy, dummyMetrics);

        MethodResult<R> resultExceptionA = MethodResult.failure(new TestExceptionA());
        assertTrue("Fallback should be applied since the exception thrown is found in the list of applyOn() exceptions",
                   state.shouldApplyFallback(resultExceptionA));

        MethodResult<R> resultExceptionAextended = MethodResult.failure(new TestExceptionAextended());
        assertTrue("Fallback should be applied since the exception thrown is an instance of an exception in the list of applyOn() exceptions",
                   state.shouldApplyFallback(resultExceptionAextended));

        MethodResult<R> resultExceptionB = MethodResult.failure(new TestExceptionB());
        assertFalse("Fallback should NOT be applied since the exception thrown is NOT an instance of an exception in the list of applyOn() exceptions",
                    state.shouldApplyFallback(resultExceptionB));

    }

    @SuppressWarnings("unchecked")
    @Test
    public <R> void testFallbackSkipOn() throws InterruptedException, InstantiationException, IllegalAccessException {
        FallbackPolicyImpl policy = new FallbackPolicyImpl();
        policy.setSkipOn(TestExceptionA.class);
        FallbackStateImpl state = new FallbackStateImpl(policy, dummyMetrics);

        MethodResult<R> resultExceptionA = MethodResult.failure(new TestExceptionA());
        assertFalse("Fallback should NOT be applied since the exception thrown is found in the list of skipOn() exceptions",
                    state.shouldApplyFallback(resultExceptionA));

        MethodResult<R> resultExceptionAextended = MethodResult.failure(new TestExceptionAextended());
        assertFalse("Fallback should NOT be applied since the exception thrown is an instance of an exception in the list of skipOn() exceptions",
                    state.shouldApplyFallback(resultExceptionAextended));

        MethodResult<R> resultExceptionB = MethodResult.failure(new TestExceptionB());
        assertTrue("Fallback should be applied since the exception thrown is NOT an instance of an exception in the list of skipOn() exceptions",
                   state.shouldApplyFallback(resultExceptionB));
    }

    @SuppressWarnings("unchecked")
    @Test
    public <R> void testFallbackApplyAndSkipOn() throws InterruptedException, InstantiationException, IllegalAccessException {
        FallbackPolicyImpl policy = new FallbackPolicyImpl();
        policy.setApplyOn(TestExceptionA.class, TestExceptionBextended.class);
        policy.setSkipOn(TestExceptionB.class, TestExceptionAextended.class);
        FallbackStateImpl state = new FallbackStateImpl(policy, dummyMetrics);

        // Summary:
        // * NOTE: from the Fallback.java specification, skipOn() takes priority over applyOn() *
        // TestExceptionA          should apply Fallback      since it is in applyOn() and not in (or an instance of an exception in) skipOn()
        // TestExceptionAextended  should NOT apply Fallback  since it is in skipOn()
        // TestExceptionB          should NOT apply Fallback  since it is in skipOn()
        // TestExceptionBextended  should NOT apply Fallback  since it is in an instance of an exception in skipOn()

        MethodResult<R> resultExceptionA = MethodResult.failure(new TestExceptionA());
        assertTrue("ExceptionA ⊆ applyOn and ⊄ skipOn => Fallback should be applied",
                   state.shouldApplyFallback(resultExceptionA));

        MethodResult<R> resultExceptionAextended = MethodResult.failure(new TestExceptionAextended());
        assertFalse("ExceptionAextended ⊆ skipOn => Fallback should NOT be applied",
                    state.shouldApplyFallback(resultExceptionAextended));

        MethodResult<R> resultExceptionB = MethodResult.failure(new TestExceptionB());
        assertFalse("ExceptionB ⊆ skipOn => Fallback should NOT be applied",
                    state.shouldApplyFallback(resultExceptionB));

        MethodResult<R> resultExceptionBextended = MethodResult.failure(new TestExceptionBextended());
        assertFalse("ExceptionBextended ⊆ skipOn => Fallback should NOT be applied",
                    state.shouldApplyFallback(resultExceptionBextended));

    }

    static class TestExceptionA extends Exception {
    }

    static class TestExceptionB extends Exception {
    }

    static class TestExceptionAextended extends TestExceptionA {
    }

    static class TestExceptionBextended extends TestExceptionB {
    }

}
