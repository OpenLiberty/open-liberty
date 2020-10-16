/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.helloworldra;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;

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