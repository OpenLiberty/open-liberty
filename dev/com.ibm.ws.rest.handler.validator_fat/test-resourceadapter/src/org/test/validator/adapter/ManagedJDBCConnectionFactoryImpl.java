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

import java.io.PrintWriter;
import java.sql.Connection;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.CommException;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.ConnectionDefinition;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.resource.spi.ResourceAllocationException;
import javax.security.auth.Subject;
import javax.sql.DataSource;

/**
 * Test managed connection factory for JDBC connections that don't connect to anything.
 */
@ConnectionDefinition(connectionFactory = DataSource.class,
                      connectionFactoryImpl = DataSourceImpl.class,
                      connection = Connection.class,
                      connectionImpl = JDBCConnectionImpl.class)
public class ManagedJDBCConnectionFactoryImpl extends ManagedConnectionFactoryImpl {
    private static final long serialVersionUID = 1L;

    @Override
    public Object createConnectionFactory() throws ResourceException {
        return createConnectionFactory(null);
    }

    @Override
    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        return new DataSourceImpl(cm, this);
    }
}
