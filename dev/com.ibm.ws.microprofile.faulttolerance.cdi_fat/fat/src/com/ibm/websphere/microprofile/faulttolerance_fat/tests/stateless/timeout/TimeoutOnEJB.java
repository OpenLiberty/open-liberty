/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.timeout;

import javax.ejb.Stateless;

import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

@Stateless
public class TimeoutOnEJB {

    private static final int TIMEOUT_DURATION = 1000;

    //A simple test that makes sure the counter works.
    @Timeout(TIMEOUT_DURATION)
    public void testMethodThatTimesOut(ResultsRecord record) throws TimeoutException {
        record.testMethodCalled = true;
        try {
            Thread.sleep(TIMEOUT_DURATION * 2);
        } catch (InterruptedException e) {
            record.testMethodRecievedInteruptException = true;
            return;
        }

        record.testMethodContinuedPastInterruptException = true;
    }

    @Timeout(TIMEOUT_DURATION * 1000)
    public void testMethodThatWontTimeOut() throws TimeoutException {
    }

}
