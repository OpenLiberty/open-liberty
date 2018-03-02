/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance_fat.tx.beans;

import java.util.Set;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;
import com.ibm.ws.microprofile.faulttolerance_fat.util.Connection;
import com.ibm.ws.microprofile.faulttolerance_fat.util.DisconnectException;
import com.ibm.wsspi.uow.UOWManagerFactory;

/**
 * Servlet implementation class Test
 */
@RequestScoped
@Retry(maxRetries = 2)
public class RetryBeanB extends RetryBeanA {

    private int connectCount = 0;
    private int disconnectCount = 0;

    /*
     * The @FTTransactional annotation on this method is a naive imitation of the
     * standard @Transactional. It provides rudimentary transactionality through an
     * interceptor which has a lower priority than the fault tolerance interceptor.
     * All retries of this method will therefore occur within their own transaction.
     */
    @Retry
    @FTTransactional
    public Connection connectB(Set<Long> txns) throws ConnectException {
        final long tx = UOWManagerFactory.getUOWManager().getLocalUOWId();
        txns.add(tx);
        System.out.println("connectB called under tx: " + tx);
        throw new ConnectException("RetryBeanB Connect: " + (++connectCount));
    }

    public void disconnectB() throws DisconnectException {
        String message = "RetryBeanB Disconnect: " + (++disconnectCount);
        System.out.println(message);
        throw new DisconnectException(message);
    }
}
