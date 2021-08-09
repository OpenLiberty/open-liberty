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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.ibm.ws.collector.Client;
import com.ibm.ws.collector.clientpool.test.ClientPoolTest;
import com.ibm.wsspi.ssl.SSLSupport;

/**
 *
 */
public class DummyClient implements Client {
    private static final Class c = ClientPoolTest.class;
    private static final Logger logger = Logger.getLogger(c.getSimpleName());

    private String hostName;
    private int port;
    private final String sslConfig;
    private final SSLSupport sslSupport;

    private volatile boolean connectionInitialized = false;

    private final CountDownLatch lock = new CountDownLatch(1);

    private static final long waitBeforeConnectMilliSecs = 500;
    private static final long waitBeforeSendMilliSecs = 500;
    private static final long waitBeforeCloseMilliSecs = 500;

    private static int counter = 0;
    private int id = 0;

    /**
     *
     */
    public DummyClient(String sslConfig, SSLSupport sslSupport) {
        this.id = counter++;
        this.sslConfig = sslConfig;
        this.sslSupport = sslSupport;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.collector.Client#connect(java.lang.String, int)
     */
    @Override
    public void connect(String hostName, int port) {
        this.connectionInitialized = false;

        this.hostName = hostName;
        this.port = port;

        logger.info("Client[" + id + "] Initializing connection...");

        try {
            lock.await(waitBeforeConnectMilliSecs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.throwing(c.getSimpleName(), "connect", e);
        }

        this.connectionInitialized = true;
        logger.info("Client[" + id + "] Connection initialized.");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.collector.Client#sendData(java.util.List)
     */
    @Override
    public void sendData(List<Object> dataObjects) {
        if (connectionInitialized) {
            logger.info("Client[" + id + "] Sending dataObjects...");

            try {
                lock.await(waitBeforeSendMilliSecs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                logger.throwing(c.getSimpleName(), "sendData", e);
            }

            logger.info("Client[" + id + "] Sent dataObjects.");
        } else {
            throw new RuntimeException("Client[" + id + "] Connection not Initialized.");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.collector.Client#close()
     */
    @Override
    public void close() {
        logger.info("Client[" + id + "] closing connection...");
        this.hostName = null;
        this.port = 0;
        this.connectionInitialized = false;

        try {
            lock.await(waitBeforeCloseMilliSecs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.throwing(c.getSimpleName(), "close", e);
        }

        logger.info("Client[" + id + "] closed connection.");
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "DummyClient "
               + "[id=" + id
               + ", hostName=" + hostName
               + ", port=" + port
               + ", sslConfig=" + sslConfig
               + ", sslSupport=" + sslSupport
               + ", connectionInitialized=" + connectionInitialized + "]";
    }

}
