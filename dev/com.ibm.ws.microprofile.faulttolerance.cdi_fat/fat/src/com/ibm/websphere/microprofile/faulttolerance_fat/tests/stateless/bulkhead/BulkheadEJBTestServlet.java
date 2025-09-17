/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.bulkhead;

import static com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.ExecutionAssert.assertCompletes;
import static com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.ExecutionAssert.assertThrowsEjbWrapped;
import static componenttest.rules.repeater.MicroProfileActions.MP20_ID;

import java.util.concurrent.Future;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.Test;

import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.BarrierFactory;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.BarrierFactory.Barrier;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/bulkheadejb")
public class BulkheadEJBTestServlet extends FATServlet {

    @Inject
    private BulkheadEJB bean;

    @Resource
    private ManagedExecutorService executor;

    @Test
    // EJB will FFDC on non-application exceptions
    @ExpectedFFDC("org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException")
    // Pre mpFaultTolerance-3.0 (MP-4.0), bulkhead state is per-bean instance
    // Stateless EJBs are pooled and calls are serlialized, effectively giving each call its own bulkhead,
    // which means the bulkhead is technically working correctly but is completely useless.
    @SkipForRepeat(MP20_ID)
    public void testBulkhead() {
        try (BarrierFactory bf = new BarrierFactory()) {
            Barrier b1 = bf.create();
            Future<?> f1 = executor.submit(() -> bean.test(b1));
            b1.assertAwaited();

            Barrier b2 = bf.create();
            Future<?> f2 = executor.submit(() -> bean.test(b2));
            b2.assertAwaited();

            Barrier b3 = bf.create();
            Future<?> f3 = executor.submit(() -> bean.test(b3));
            b3.assertAwaited();

            // Bulkhead is now full

            Barrier b4 = bf.create();
            Future<?> f4 = executor.submit(() -> bean.test(b4));
            assertThrowsEjbWrapped(BulkheadException.class, f4);

            // Free up one space

            b1.complete();
            assertCompletes(f1);

            Barrier b5 = bf.create();
            Future<?> f5 = executor.submit(() -> bean.test(b5));

            // Allow everything to complete and check it was successful
            b2.complete();
            assertCompletes(f2);
            b3.complete();
            assertCompletes(f3);
            b5.complete();
            assertCompletes(f5);
        }
    }
}
