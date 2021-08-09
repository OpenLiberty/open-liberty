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
import com.ibm.ws.microprofile.faulttolerance_fat.util.Connection;
import com.ibm.ws.microprofile.faulttolerance_fat.util.DisconnectException;

/**
 * Servlet implementation class Test
 */
@RequestScoped
@Retry(maxRetries = 2)
public class RetryBeanB extends RetryBeanA {

    private int connectCount = 0;
    private int disconnectCount = 0;

    @Retry
    public Connection connectB() throws ConnectException {
        throw new ConnectException("RetryBeanB Connect: " + (++connectCount));
    }

    public void disconnectB() throws DisconnectException {
        String message = "RetryBeanB Disconnect: " + (++disconnectCount);
        System.out.println(message);
        throw new DisconnectException(message);
    }
}
