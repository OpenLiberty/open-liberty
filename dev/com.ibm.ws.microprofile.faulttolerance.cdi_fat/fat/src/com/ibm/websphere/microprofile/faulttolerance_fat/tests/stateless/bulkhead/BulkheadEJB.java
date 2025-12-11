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

import javax.ejb.Stateless;

import org.eclipse.microprofile.faulttolerance.Bulkhead;

import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.BarrierFactory.Barrier;

@Stateless
public class BulkheadEJB {

    @Bulkhead(3)
    public void test(Barrier barrier) {
        System.out.println("BulkheadEJB begin waiting: " + this);
        barrier.await();
        System.out.println("BulkheadEJB completing: " + this);
    }
}
