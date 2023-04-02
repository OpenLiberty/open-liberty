/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package com.ibm.example.jca.anno;

import com.ibm.example.jca.anno.ConnectionSpecImpl.ConnectionRequestInfoImpl;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionMetaData;
import jakarta.resource.cci.Interaction;
import jakarta.resource.cci.LocalTransaction;
import jakarta.resource.cci.ResultSetInfo;
import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionEventListener;

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
