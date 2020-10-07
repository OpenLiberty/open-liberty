/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.test.jca.loginmodra;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.ResultSetInfo;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;

public class LMConnection implements Connection, ConnectionMetaData {
    LMManagedConnection mc;

    LMConnection(LMManagedConnection mc) {
        this.mc = mc;
    }

    @Override
    public void close() throws ResourceException {
        if (mc == null) {
            throw new ResourceException("Connection was already closed");
        } else {
            ConnectionEvent event = new ConnectionEvent(mc, ConnectionEvent.CONNECTION_CLOSED);
            event.setConnectionHandle(this);
            for (ConnectionEventListener listener : mc.listeners)
                listener.connectionClosed(event);
            mc = null;
        }
    }

    @Override
    public Interaction createInteraction() throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public String getEISProductName() {
        return "LoginModEIS";
    }

    @Override
    public String getEISProductVersion() {
        return "96.247.265";
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public ConnectionMetaData getMetaData() throws ResourceException {
        return this;
    }

    @Override
    public ResultSetInfo getResultSetInfo() throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public String getUserName() {
        return mc.userPwd;
    }
}
