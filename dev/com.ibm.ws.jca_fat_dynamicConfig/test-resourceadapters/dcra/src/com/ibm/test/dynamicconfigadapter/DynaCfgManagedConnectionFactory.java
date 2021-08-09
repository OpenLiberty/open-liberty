/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.test.dynamicconfigadapter;

import java.io.PrintWriter;
import java.util.Set;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.resource.spi.TransactionSupport;
import javax.security.auth.Subject;

public class DynaCfgManagedConnectionFactory implements ManagedConnectionFactory, ResourceAdapterAssociation, TransactionSupport {
    private static final long serialVersionUID = -2703356891914213274L;

    private int loginTimeout;
    private ResourceAdapter adapter;

    @Override
    public Object createConnectionFactory() throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        return new DynaCfgDataSource(cm, this);
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        return new DynaCfgManagedConnection(this, (DynaCfgConnectionRequestInfo) cri, subject);
    }

    public int getLoginTimeout() {
        return loginTimeout;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return null;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    /**
     * @see javax.resource.spi.TransactionSupport#getTransactionSupport()
     */
    @Override
    public TransactionSupportLevel getTransactionSupport() {
        return TransactionSupportLevel.NoTransaction;
    }

    @Override
    public ManagedConnection matchManagedConnections(@SuppressWarnings("rawtypes") Set set, Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        return null;
    }

    public void setLoginTimeout(int loginTimeout) {
        this.loginTimeout = loginTimeout;
    }

    @Override
    public void setLogWriter(PrintWriter writer) throws ResourceException {
    }

    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.adapter = adapter;
    }
}
