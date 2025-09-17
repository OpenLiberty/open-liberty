/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.async;

import static com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.ExecutionAssert.assertCompletes;
import static com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.ExecutionAssert.assertThrows;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.BarrierFactory;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.BarrierFactory.Barrier;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.TestException;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@SuppressWarnings("serial")
@WebServlet("/ftasyncejb")
// Disabled because the CDI-EJB integration for interceptors does not work when an interceptor returns and then calls proceed on another thread
// Lots of cases to test here (e.g. transactions) if we want to make this work.
@Mode(TestMode.EXPERIMENTAL)
public class FTAsyncEJBTestServlet extends FATServlet {

    @Inject
    private FTAsyncEJB bean;

    @Test
    public void testAsync() {
        try (BarrierFactory bf = new BarrierFactory()) {
            AtomicInteger counter = new AtomicInteger(0);

            Barrier b1 = bf.create();
            Future<?> f1 = bean.test(counter, b1);
            b1.assertAwaited();

            assertEquals(1, counter.get());

            Barrier b2 = bf.create();
            Future<?> f2 = bean.test(counter, b2);
            b2.assertAwaited();

            assertEquals(2, counter.get());

            b1.complete();
            assertCompletes(f1);

            b2.complete();
            assertCompletes(f2);
        }
    }

    @Test
    public void testAsyncRetry() throws TestException {
        AtomicInteger failureCoundown1 = new AtomicInteger(3);
        assertCompletes(bean.testRetry(failureCoundown1));
        assertEquals(-1, failureCoundown1.get());

        AtomicInteger failureCoundown2 = new AtomicInteger(5);
        assertThrows(TestException.class, bean.testRetry(failureCoundown2));
        assertEquals(1, failureCoundown2.get());
    }

}
