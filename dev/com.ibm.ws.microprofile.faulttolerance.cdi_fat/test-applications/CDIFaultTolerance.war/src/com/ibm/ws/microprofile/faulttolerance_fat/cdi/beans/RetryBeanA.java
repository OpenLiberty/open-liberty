/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans;

import org.eclipse.microprofile.faulttolerance.Retry;

import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;
import com.ibm.ws.microprofile.faulttolerance_fat.util.Connection;
import com.ibm.ws.microprofile.faulttolerance_fat.util.DisconnectException;

public class RetryBeanA {

    private int connectCount = 0;
    private int disconnectCount = 0;

    public Connection connectA() throws ConnectException {
        throw new ConnectException("RetryBeanA Connect: " + (++connectCount));
    }

    @Retry(maxRetries = 4)
    public void disconnectA() throws DisconnectException {
        throw new DisconnectException("RetryBeanA Disconnect: " + (++disconnectCount));
    }
}
