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

import java.util.concurrent.Future;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;
import com.ibm.ws.microprofile.faulttolerance_fat.util.Connection;

/**
 * Servlet implementation class Test
 */
@RequestScoped
@Retry(maxRetries = 2)
public class FallbackBean extends ParentFallbackBean {

    private int connectCountC = 0;
    private int connectCountD = 0;

    @Asynchronous
    @Fallback(AsyncFallbackHandler.class)
    public Future<Connection> connectC() throws ConnectException {
        throw new ConnectException("FallbackBean.connectC: " + (++connectCountC));
    }

    @Asynchronous
    @Fallback(fallbackMethod = "fallbackAsync")
    public Future<Connection> connectD() throws ConnectException {
        throw new ConnectException("FallbackBean.connectD: " + (++connectCountD));
    }

    public int getConnectCountC() {
        return connectCountC;
    }

    public int getConnectCountD() {
        return connectCountD;
    }

}
