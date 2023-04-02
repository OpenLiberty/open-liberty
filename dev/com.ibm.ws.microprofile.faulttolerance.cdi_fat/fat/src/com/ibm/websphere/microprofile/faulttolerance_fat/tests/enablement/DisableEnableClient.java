/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.enablement;

import org.eclipse.microprofile.faulttolerance.Retry;

import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;

// Important: no annotation on the class
// bean-discovery-mode=all will make this a dependent bean
public class DisableEnableClient {

    private int failWithOneRetryCounter = 0;
    private int failWithOneRetryAgainCounter = 0;

    /**
     * If Retry is enabled, this method will run twice, otherwise it will run once
     */
    @Retry(maxRetries = 1)
    public void failWithOneRetry() throws ConnectException {
        failWithOneRetryCounter++;
        throw new ConnectException("Test Exception");
    }

    public int getFailWithOneRetryCounter() {
        return failWithOneRetryCounter;
    }

    /**
     * If Retry is enabled, this method will run twice, otherwise it will run once
     */
    @Retry(maxRetries = 1)
    public void failWithOneRetryAgain() throws ConnectException {
        failWithOneRetryAgainCounter++;
        throw new ConnectException("Test Exception");
    }

    public int getFailWithOneRetryAgainCounter() {
        return failWithOneRetryAgainCounter;
    }

}
