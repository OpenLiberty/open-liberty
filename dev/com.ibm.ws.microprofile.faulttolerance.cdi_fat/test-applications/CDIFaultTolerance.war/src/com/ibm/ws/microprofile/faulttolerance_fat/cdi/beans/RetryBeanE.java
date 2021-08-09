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
public class RetryBeanE {

    private int connectCount = 0;

    /**
     * Max retries is set to 6 at the class level in config, which should affect this method
     */
    @Retry(maxRetries = 2)
    public void connect() throws ConnectException {
        connectCount++;
        throw new ConnectException("RetryBeanE Connect: " + connectCount);
    }

    public int getConnectCount() {
        return connectCount;
    }
}
