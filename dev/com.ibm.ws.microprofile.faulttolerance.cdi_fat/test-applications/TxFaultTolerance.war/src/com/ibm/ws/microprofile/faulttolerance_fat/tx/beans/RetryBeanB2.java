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
package com.ibm.ws.microprofile.faulttolerance_fat.tx.beans;

import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.transaction.Transactional;

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
public class RetryBeanB2 extends RetryBeanA {

    private int connectCount = 0;
    private int disconnectCount = 0;

    /*
     * This method has the standard @Transactional annotation
     * The interceptors for @Transactional are specified to have a priority which means
     * they will fire before the fault tolerance interceptors. All retries of this method
     * will therefore occur within the same transaction.
     */
    @Retry
    @Transactional
    public Connection connectB(Set<Long> txns) throws ConnectException {
        final long tx = UOWManagerFactory.getUOWManager().getLocalUOWId();
        txns.add(tx);
        System.out.println("connectB called under tx: " + tx);
        throw new ConnectException("RetryBeanB2 Connect: " + (++connectCount));
    }

    public void disconnectB() throws DisconnectException {
        String message = "RetryBeanB2 Disconnect: " + (++disconnectCount);
        System.out.println(message);
        throw new DisconnectException(message);
    }
}
