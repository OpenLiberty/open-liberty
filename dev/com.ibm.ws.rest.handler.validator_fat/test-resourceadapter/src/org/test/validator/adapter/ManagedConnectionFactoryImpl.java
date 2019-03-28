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
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
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

/**
 * Test managed connection factory that doesn't connect to anything.
 */
@ConnectionDefinition(connectionFactory = ConnectionFactory.class,
                      connectionFactoryImpl = ConnectionFactoryImpl.class,
                      connection = Connection.class,
                      connectionImpl = ConnectionImpl.class)
public class ManagedConnectionFactoryImpl implements ManagedConnectionFactory, ResourceAdapterAssociation {
    private static final long serialVersionUID = 1L;

    ResourceAdapterImpl adapter;

    @ConfigProperty
    private String hostName = "localhost";

    @ConfigProperty
    private Integer portNumber = 4321;

    @Override
    public Object createConnectionFactory() throws ResourceException {
        return createConnectionFactory(null);
    }

    @Override
    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        return new ConnectionFactoryImpl(cm, this);
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        // Various sorts of exception paths for the validator to test

        if (portNumber < 0) {
            InvalidPropertyException x = new InvalidPropertyException("portNumber");
            x.initCause(new IllegalArgumentException("Negative port numbers are not allowed."));
            throw x;
        }

        if (portNumber < 1024)
            throw new IllegalArgumentException(Integer.toString(portNumber));

        if (portNumber % 10 == 0)
            throw new ResourceAllocationException("Port " + portNumber + " is currently in use.");

        if (!hostName.equals("localhost") && !hostName.endsWith(".openliberty.io"))
            throw new CommException("Unable to connect to " + hostName);

        return new ManagedConnectionImpl();
    }

    public String getHostName() {
        return hostName;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return null;
    }

    public Integer getPortNumber() {
        return portNumber;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    @Override
    public ManagedConnection matchManagedConnections(@SuppressWarnings("rawtypes") Set connections, Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        for (Object mc : connections)
            if (mc instanceof ManagedConnectionImpl)
                return (ManagedConnection) mc;
        return null;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    @Override
    public void setLogWriter(PrintWriter logWriter) throws ResourceException {}

    public void setPortNumber(Integer portNumber) {
        this.portNumber = portNumber;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.adapter = (ResourceAdapterImpl) adapter;
    }
}
