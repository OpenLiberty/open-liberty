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
package org.test.config.adapter;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;

public class ConnectionFactoryImpl implements ConnectionFactory {
    private static final long serialVersionUID = 1L;

    @Override
    public Connection getConnection() throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public Connection getConnection(ConnectionSpec conSpec) throws ResourceException {
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
        return null;
    }

    @Override
    public void setReference(Reference ref) {
    }
}
