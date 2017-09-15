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

import java.util.concurrent.Future;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;

import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;

@RequestScoped
public class RetryBeanC {

    private int connectCount = 0;

    // Should always abort as ConnectException.class is always thrown
    @Retry(maxRetries = 3, abortOn = { IllegalArgumentException.class, ConnectException.class })
    public void connectC() throws ConnectException {
        throw new ConnectException("RetryBeanC Connect: " + (++connectCount));
    }

    // AbortOn is overridden in config to [IllegalArgumentException, ConnectionException] so this should always abort
    @Retry(maxRetries = 3)
    public void connectC2() throws ConnectException {
        throw new ConnectException("RetryBeanC Connect: " + (++connectCount));
    }

    @Asynchronous
    @Retry(maxRetries = 3, abortOn = ConnectException.class)
    public Future<Void> connectCAsync() throws ConnectException {
        throw new ConnectException("RetryBeanC Connect: " + (++connectCount));
    }

    @Retry(maxRetries = -1, maxDuration = 0, jitter = 0, delay = 0)
    public void connectCForever() throws ConnectException {
        connectCount++;
        if (connectCount < 5) {
            throw new ConnectException("RetryBeanC Connect: " + connectCount);
        }
    }

    @Retry(maxRetries = 5, maxDuration = 0)
    public void connectCDurationZero() throws ConnectException {
        connectCount++;
        throw new ConnectException("RetryBeanC Connect: " + connectCount);
    }

    public int getConnectCount() {
        return connectCount;
    }

}
