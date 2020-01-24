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

import java.io.PrintWriter;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.SecurityException;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

public class LMManagedConnection implements ManagedConnection {
    final ConcurrentLinkedQueue<ConnectionEventListener> listeners = new ConcurrentLinkedQueue<ConnectionEventListener>();
    final String userPwd;

    LMManagedConnection(String userPwd) {
        this.userPwd = userPwd;
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void associateConnection(Object handle) throws ResourceException {
        ((LMConnection) handle).mc = this;
    }

    @Override
    public void cleanup() throws ResourceException {
    }

    @Override
    public void destroy() throws ResourceException {
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        String u = LMManagedConnectionFactory.getUserAndPassword(subject);
        if (!userPwd.equals(u))
            throw new SecurityException("User/password " + u + " does not match " + userPwd);
        return new LMConnection(this);
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
