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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;
import com.ibm.ws.microprofile.faulttolerance_fat.util.Connection;

/**
 * Servlet implementation class Test
 */
@RequestScoped
@Retry(maxRetries = 2)
public class FallbackBean {

    private int connectCountA = 0;
    private int connectCountB = 0;
    private int connectCountC = 0;
    private int connectCountD = 0;

    @Fallback(MyFallbackHandler.class)
    public Connection connectA() throws ConnectException {
        throw new ConnectException("FallbackBean.connectA: " + (++connectCountA));
    }

    @Fallback(MyFallbackHandler.class)
    public Connection connectB(String param) throws ConnectException {
        throw new ConnectException("FallbackBean.connectB: " + (++connectCountB));
    }

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

    public Connection fallback(ExecutionContext executionContext) {
        return new Connection() {

            @Override
            public String getData() {
                return "Fallback Connection: " + executionContext.getMethod().getName();
            }
        };
    }

    public Future<Connection> fallbackAsync() {
        return CompletableFuture.completedFuture(new Connection() {
            @Override
            public String getData() {
                return "fallbackAsync";
            }
        });
    }

    public int getConnectCountA() {
        return connectCountA;
    }

    public int getConnectCountB() {
        return connectCountB;
    }

    public int getConnectCountC() {
        return connectCountC;
    }

    public int getConnectCountD() {
        return connectCountD;
    }

}
