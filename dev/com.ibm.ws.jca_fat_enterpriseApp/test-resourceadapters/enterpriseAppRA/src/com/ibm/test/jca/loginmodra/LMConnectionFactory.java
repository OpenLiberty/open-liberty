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

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;

public class LMConnectionFactory implements ConnectionFactory {
    private static final long serialVersionUID = 1;

    private final ConnectionManager cm;
    private final ManagedConnectionFactory mcf;
    private Reference ref;

    LMConnectionFactory(ConnectionManager cm, ManagedConnectionFactory mcf) {
        this.cm = cm;
        this.mcf = mcf;
    }

    @Override
    public Connection getConnection() throws ResourceException {
        return (Connection) cm.allocateConnection(mcf, null);
    }

    @Override
    public Connection getConnection(ConnectionSpec conSpec) throws ResourceException {
        if (conSpec == null)
            return getConnection();
        else
            throw new NotSupportedException();
    }

    @Override
    public ResourceAdapterMetaData getMetaData() throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public RecordFactory getRecordFactory() throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public Reference getReference() throws NamingException {
        return ref;
    }

    @Override
    public void setReference(Reference ref) {
        this.ref = ref;
    }
}
