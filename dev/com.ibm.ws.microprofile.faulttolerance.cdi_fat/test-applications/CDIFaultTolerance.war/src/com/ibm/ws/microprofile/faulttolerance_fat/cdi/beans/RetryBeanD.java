/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;

@RequestScoped
/**
 * Set the maxRetries to 2 - which would lead to test failure - but this bean's config
 * will be overridden to 4 in microprofile-config.properties so that the connectDMaxRetries2 method will
 * be executed the number of times expected by the test.
 */
@Retry(maxRetries = 2)
public class RetryBeanD extends RetryBeanA {

    private int connectCount = 0;

    // Inherit class Retry Policy
    public void connectDMaxRetries2() throws ConnectException {
        connectCount++;
        throw new ConnectException("RetryBeanD Connect: " + connectCount);
    }

    public int getConnectCount() {
        return connectCount;
    }
}
