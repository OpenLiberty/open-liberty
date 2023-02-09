/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.workcontext.jca;

import java.io.PrintWriter;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import com.ibm.workcontext.jca.ConnectionSpecImpl.ConnectionRequestInfoImpl;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LocalTransaction;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionMetaData;

/**
 * Example managed connection.
 */
public class ManagedConnectionImpl implements ManagedConnection {
    final ConcurrentLinkedQueue<ConnectionEventListener> listeners = new ConcurrentLinkedQueue<ConnectionEventListener>();

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void associateConnection(Object handle) throws ResourceException {
        ((ConnectionImpl) handle).mc = this;
    }

    @Override
    public void cleanup() throws ResourceException {
    }

    @Override
    public void destroy() throws ResourceException {
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        return new ConnectionImpl(this, (ConnectionRequestInfoImpl) cri);
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return null;
    }

    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public XAResource getXAResource() throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void setLogWriter(PrintWriter logWriter) throws ResourceException {
    }
}
