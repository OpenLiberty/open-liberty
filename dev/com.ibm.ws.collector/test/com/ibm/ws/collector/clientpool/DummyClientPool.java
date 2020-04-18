/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.collector.clientpool;

import java.lang.reflect.Field;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.collector.Client;
import com.ibm.ws.collector.ClientPool;
import com.ibm.wsspi.ssl.SSLSupport;

/**
 *
 */
public class DummyClientPool extends ClientPool {

    private LinkedBlockingDeque<Client> clientsRef = null;;

    /**
     * @param sslConfig
     * @param sslSupport
     * @param numClients
     * @throws SSLException
     */
    public DummyClientPool(String sslConfig, SSLSupport sslSupport, int numClients) throws SSLException {
        super(sslConfig, sslSupport, numClients);
        setClientsFieldRef();
    }

    /**
     * Get reference to private field 'clients' in ClientPool
     */
    private void setClientsFieldRef() {
        try {
            Field f = ClientPool.class.getDeclaredField("clients");
            f.setAccessible(true);
            clientsRef = (LinkedBlockingDeque<Client>) f.get(this);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.collector.ClientPool#createClient(java.lang.String, com.ibm.wsspi.ssl.SSLSupport)
     */
    @Override
    public Client createClient(String sslConfig, SSLSupport sslSupport) throws SSLException {
        return new DummyClient(sslConfig, sslSupport);
    }

    /**
     * Return current size of clients available to checkout.
     *
     * @return
     */
    public int getClientPoolQueueSize() {
        if (clientsRef != null) {
            return clientsRef.size();
        } else {
            return -1;
        }
    }

    public void incrementNumClientsClosed(int count) {
        try {
            Field f = ClientPool.class.getDeclaredField("numClientsClosed");
            f.setAccessible(true);
            AtomicInteger numClientsClosed = (AtomicInteger) f.get(this);
            for (int i = 0; i < count; i++) {
                numClientsClosed.incrementAndGet();
            }
            System.out.println("_incrementNumClientsClosed : done : " + numClientsClosed);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
