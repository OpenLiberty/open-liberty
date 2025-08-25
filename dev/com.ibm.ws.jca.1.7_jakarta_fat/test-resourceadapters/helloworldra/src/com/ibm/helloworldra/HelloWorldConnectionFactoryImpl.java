/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
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

package com.ibm.helloworldra;

import javax.naming.NamingException;
import javax.naming.Reference;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionFactory;
import jakarta.resource.cci.ConnectionSpec;
import jakarta.resource.cci.RecordFactory;
import jakarta.resource.cci.ResourceAdapterMetaData;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ManagedConnectionFactory;

public class HelloWorldConnectionFactoryImpl implements ConnectionFactory {

    private Reference reference;
    private ConnectionManager cm;
    private ManagedConnectionFactory mcf;

    /**
     * Constructor for HelloWorldConnectionFactoryImpl
     */
    public HelloWorldConnectionFactoryImpl(
                                           ManagedConnectionFactory mcf,
                                           ConnectionManager cm) {

        super();
        this.mcf = mcf;
        this.cm = cm;
    }

    /**
     * @see ConnectionFactory#getConnection()
     */
    @Override
    public Connection getConnection() throws ResourceException {

        return (Connection) cm.allocateConnection(mcf, null);
    }

    /**
     * @see ConnectionFactory#getConnection(ConnectionSpec)
     */
    @Override
    public Connection getConnection(ConnectionSpec connectionSpec) throws ResourceException {

        return getConnection();
    }

    /**
     * @see ConnectionFactory#getRecordFactory()
     */
    @Override
    public RecordFactory getRecordFactory() throws ResourceException {

        return new HelloWorldRecordFactoryImpl();
    }

    /**
     * @see ConnectionFactory#getMetaData()
     */
    @Override
    public ResourceAdapterMetaData getMetaData() throws ResourceException {

        return new HelloWorldResourceAdapterMetaDataImpl();
    }

    /**
     * @see Referenceable#setReference(Reference)
     */
    @Override
    public void setReference(Reference reference) {

        this.reference = reference;
    }

    /**
     * @see Referenceable#getReference()
     */
    @Override
    public Reference getReference() throws NamingException {

        return reference;
    }

}