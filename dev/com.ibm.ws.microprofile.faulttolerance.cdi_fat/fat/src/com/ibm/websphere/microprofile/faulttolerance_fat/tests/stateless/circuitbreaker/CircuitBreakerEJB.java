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

import javax.ejb.Stateless;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.TestException;

@Stateless
public class CircuitBreakerEJB {

    public enum Response {
        RETURN,
        THROW
    };

    @CircuitBreaker(requestVolumeThreshold = 4, delay = 500)
    public void test(Response response) throws TestException, FaultToleranceException {
        if (response == Response.THROW) {
            throw new TestException();
        }
    }

}
