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
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.retry;

import javax.ejb.Stateless;

import org.eclipse.microprofile.faulttolerance.Retry;

import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.TestException;

@Stateless
public class RetryOnEJB {

    public static final int MAX = 5;
    private static int counter = 0;

    //A simple test that makes sure the counter works.
    @Retry(maxRetries = 10)
    public int retryEventuallyPass() throws TestException {
        if (counter < MAX) {
            counter++;
            throw new TestException();
        }
        return counter;
    }

    public static final int NEVER_PASS_MAX = 10;
    private static int neverPassCounter = 0;

    public static int getNeverPassCounter() {
        return neverPassCounter;
    }

    @Retry(maxRetries = NEVER_PASS_MAX)
    public int retryNeverPass() throws TestException {
        neverPassCounter++;
        throw new TestException();
    }

}
