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

import org.eclipse.microprofile.faulttolerance.Fallback;

import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;
import com.ibm.ws.microprofile.faulttolerance_fat.util.Connection;

@RequestScoped
public class FallbackBeanWithoutRetry {

    private int connectCountA = 0;

    @Fallback(MyFallbackHandler.class)
    public Connection connectA() throws ConnectException {
        throw new ConnectException("FallbackBean.connectA: " + (++connectCountA));
    }

    // Overridden as MyFallbackHandler2 in config
    @Fallback(MyFallbackHandler.class)
    public Connection connectB() throws ConnectException {
        throw new ConnectException("FallbackBean.connectB");
    }

    // Overridden as connectFallback2 in config
    @Fallback(fallbackMethod = "connectFallback")
    public Connection connectC() throws ConnectException {
        throw new ConnectException("FallbackBean.connectC");
    }

    public Connection connectFallback() {
        return new Connection() {
            @Override
            public String getData() {
                return "connectFallback";
            }
        };
    }

    public Connection connectFallback2() {
        return new Connection() {
            @Override
            public String getData() {
                return "connectFallback2";
            }
        };
    }

    public int getConnectCountA() {
        return connectCountA;
    }

}
