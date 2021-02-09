/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.helloworldra;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

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

        connection.invalidate();
        connection = null;
        listeners = null;
    }

    /**
     * @see ManagedConnection#cleanup()
     */
    @Override
    public void cleanup() throws ResourceException {

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