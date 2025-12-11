/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.circuitbreaker;

import static com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.ExecutionAssert.assertReturns;
import static com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.ExecutionAssert.assertThrows;
import static com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.ExecutionAssert.assertThrowsEjbWrapped;
import static com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.circuitbreaker.CircuitBreakerEJB.Response.RETURN;
import static com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.circuitbreaker.CircuitBreakerEJB.Response.THROW;
import static org.junit.Assert.fail;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.Test;

import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.TestException;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;
import componenttest.rules.repeater.MicroProfileActions;

@SuppressWarnings("serial")
@WebServlet("/circuitbreakerejb")
public class CircuitBreakerEJBTestServlet extends FATServlet {

    @Inject
    private CircuitBreakerEJB bean;

    //On EE8_MP20 when the circuit is open, the second attempt throws TestException instead of the expected CircuitBreakerOpenException.
    @SkipForRepeat(MicroProfileActions.MP20_ID)
    @Test
    // EJB will create FFDC for non-application exceptions
    @ExpectedFFDC("org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException")
    public void testEjbCircuitBreaker() {
        assertReturns(() -> bean.test(RETURN));
        assertReturns(() -> bean.test(RETURN));
        assertReturns(() -> bean.test(RETURN));
        assertReturns(() -> bean.test(RETURN));
        assertThrows(TestException.class, () -> bean.test(THROW));
        assertThrows(TestException.class, () -> bean.test(THROW));
        // 50% failure rate in last four requests -> circuit opens
        assertThrowsEjbWrapped(CircuitBreakerOpenException.class, () -> bean.test(RETURN));
        assertThrowsEjbWrapped(CircuitBreakerOpenException.class, () -> bean.test(THROW));
        // Wait for circuit to half-open
        sleep(1000);
        // Circuit half-open
        assertReturns(() -> bean.test(RETURN));
        // Circuit closed
        assertReturns(() -> bean.test(RETURN));
        assertThrows(TestException.class, () -> bean.test(THROW));
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            fail("Test interrupted");
        }
    }

}
