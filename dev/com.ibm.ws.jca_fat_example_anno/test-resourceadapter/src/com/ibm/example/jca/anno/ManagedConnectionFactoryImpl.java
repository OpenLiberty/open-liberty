/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.example.jca.anno;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.ConnectionDefinition;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.security.auth.Subject;

/**
 * Example managed connection factory.
 */
@ConnectionDefinition(connectionFactory = ConnectionFactory.class,
                      connectionFactoryImpl = ConnectionFactoryImpl.class,
                      connection = Connection.class,
                      connectionImpl = ConnectionImpl.class)
public class ManagedConnectionFactoryImpl implements ManagedConnectionFactory, ResourceAdapterAssociation {
    private static final long serialVersionUID = -4670808886282561945L;

    /**
     * This is the in-memory data store
     */
    final static ConcurrentHashMap<String, ConcurrentLinkedQueue<Map<?, ?>>> tables = new ConcurrentHashMap<String, ConcurrentLinkedQueue<Map<?, ?>>>();

    ResourceAdapterImpl adapter;

    @ConfigProperty
    private String tableName = "DEFAULT";

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
        return new ManagedConnectionImpl();
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return null;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public ManagedConnection matchManagedConnections(@SuppressWarnings("rawtypes") Set connections, Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        for (Object mc : connections)
            if (mc instanceof ManagedConnectionImpl)
                return (ManagedConnection) mc;
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter logWriter) throws ResourceException {}

    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.adapter = (ResourceAdapterImpl) adapter;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
        tables.putIfAbsent(tableName, new ConcurrentLinkedQueue<Map<?, ?>>());
    }
}
