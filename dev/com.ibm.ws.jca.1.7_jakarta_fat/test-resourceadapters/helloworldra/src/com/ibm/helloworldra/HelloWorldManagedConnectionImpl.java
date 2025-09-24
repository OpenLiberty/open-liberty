/*******************************************************************************
 * Copyright (c) 2002,2021 IBM Corporation and others.
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

package com.ibm.helloworldra;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;

import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LocalTransaction;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionMetaData;

public class HelloWorldManagedConnectionImpl implements ManagedConnection {

    private static final String TRANSACTIONS_NOT_SUPPORTED_ERROR = "Transactions not supported";

    private HelloWorldConnectionImpl connection;
    private Vector listeners = new Vector();
    private PrintWriter out;

    /**
     * Constructor for HelloWorldManagedConnectionImpl
     */
    public HelloWorldManagedConnectionImpl() {

        super();
    }

    public void close() {

        Enumeration list = listeners.elements();
        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(connection);
        while (list.hasMoreElements()) {
            ((ConnectionEventListener) list.nextElement()).connectionClosed(event);
        }
    }

    /**
     * @see ManagedConnection#getConnection(Subject, ConnectionRequestInfo)
     */
    @Override
    public Object getConnection(
                                Subject subject,
                                ConnectionRequestInfo cxRequestInfo) throws ResourceException {

        connection = new HelloWorldConnectionImpl(this);
        return connection;
    }

    /**
     * @see ManagedConnection#destroy()
     */
    @Override
    public void destroy() throws ResourceException {

        // Connection Manager never invokes getConnection when using this ManagedConnection for parked handles
        if (connection != null)
            connection.invalidate();
        connection = null;
        listeners = null;
    }

    /**
     * @see ManagedConnection#cleanup()
     */
    @Override
    public void cleanup() throws ResourceException {

        // Connection Manager never invokes getConnection when using this ManagedConnection for parked handles
        if (connection != null)
            connection.invalidate();
    }

    /**
     * @see ManagedConnection#associateConnection(Object)
     */
    @Override
    public void associateConnection(Object connection) throws ResourceException {
    }

    /**
     * @see ManagedConnection#addConnectionEventListener(ConnectionEventListener)
     */
    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {

        listeners.add(listener);
    }

    /**
     * @see ManagedConnection#removeConnectionEventListener(ConnectionEventListener)
     */
    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {

        listeners.remove(listener);
    }

    /**
     * @see ManagedConnection#getXAResource()
     */
    @Override
    public XAResource getXAResource() throws ResourceException {

        throw new NotSupportedException(TRANSACTIONS_NOT_SUPPORTED_ERROR);
    }

    /**
     * @see ManagedConnection#getLocalTransaction()
     */
    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {

        throw new NotSupportedException(TRANSACTIONS_NOT_SUPPORTED_ERROR);
    }

    /**
     * @see ManagedConnection#getMetaData()
     */
    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {

        return new HelloWorldManagedConnectionMetaDataImpl(connection.getMetaData());
    }

    /**
     * @see ManagedConnection#setLogWriter(PrintWriter)
     */
    @Override
    public void setLogWriter(PrintWriter out) throws ResourceException {

        this.out = out;
    }

    /**
     * @see ManagedConnection#getLogWriter()
     */
    @Override
    public PrintWriter getLogWriter() throws ResourceException {

        return out;
    }

}