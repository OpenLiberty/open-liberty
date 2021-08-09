/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.bval.jca.adapter;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.ResultSetInfo;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;

import com.ibm.bval.jca.adapter.ConnectionSpecImpl.ConnectionRequestInfoImpl;

/**
 * Example connection.
 */
public class ConnectionImpl implements Connection {
    ConnectionFactoryImpl cf;
    ConnectionRequestInfoImpl cri;
    ManagedConnectionImpl mc;

    ConnectionImpl(ManagedConnectionImpl mc, ConnectionRequestInfoImpl cri) {
        this.cri = cri;
        this.mc = mc;
    }

    @Override
    public void close() throws ResourceException {
        if (cri == null)
            throw new ResourceException("already closed");
        cri = null;

        if (mc != null) {
            ConnectionEvent event = new ConnectionEvent(mc, ConnectionEvent.CONNECTION_CLOSED);
            event.setConnectionHandle(this);
            for (ConnectionEventListener listener : mc.listeners)
                listener.connectionClosed(event);
            mc = null;
        }
    }

    @Override
    public Interaction createInteraction() throws ResourceException {
        if (cri == null)
            throw new ResourceException("connection is closed");
        else
            return new InteractionImpl(this);
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public ConnectionMetaData getMetaData() throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public ResultSetInfo getResultSetInfo() throws ResourceException {
        throw new NotSupportedException();
    }
}
