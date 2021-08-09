/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.test.validator.adapter;

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

import org.test.validator.adapter.ConnectionSpecImpl.ConnectionRequestInfoImpl;

public class ConnectionFactoryImpl implements ConnectionFactory {
    private static final long serialVersionUID = 847022212144243370L;

    private final ConnectionManager cm;
    final ManagedConnectionFactoryImpl mcf;
    private Reference ref;

    ConnectionFactoryImpl(ConnectionManager cm, ManagedConnectionFactoryImpl mcf) {
        this.cm = cm;
        this.mcf = mcf;
    }

    @Override
    public Connection getConnection() throws ResourceException {
        return getConnection(new ConnectionSpecImpl());
    }

    @Override
    public Connection getConnection(ConnectionSpec conSpec) throws ResourceException {
        ConnectionRequestInfoImpl cri = ((ConnectionSpecImpl) conSpec).createConnectionRequestInfo();
        return (Connection) cm.allocateConnection(mcf, cri);
    }

    @Override
    public ResourceAdapterMetaData getMetaData() throws ResourceException {
        return new ResourceAdapterMetaDataImpl();
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
