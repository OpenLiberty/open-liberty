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

import java.io.PrintWriter;
import java.util.Set;

import javax.resource.NotSupportedException;
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
    private Boolean enableBetaContent;

    @ConfigProperty
    private Character escapeChar;

    @ConfigProperty(defaultValue = "localhost")
    private String hostName;

    @ConfigProperty
    private String password;

    @ConfigProperty
    private Integer portNumber = 7654;

    @ConfigProperty
    private String userName;

    @Override
    public Object createConnectionFactory() throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        return new ConnectionFactoryImpl(cm, this);
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        return new ManagedConnectionImpl(this);
    }

    public Boolean getEnableBetaContent() {
        return enableBetaContent;
    }

    public Character getEscapeChar() {
        return escapeChar;
    }

    public String getHostName() {
        return hostName;
    }

    @Override
    public PrintWriter getLogWriter() {
        return null;
    }

    public String getPassword() {
        return password;
    }

    public Integer getPortNumber() {
        return portNumber;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    public String getUserName() {
        return userName;
    }

    @Override
    public ManagedConnection matchManagedConnections(@SuppressWarnings("rawtypes") Set connections, Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        return null;
    }

    public void setEnableBetaContent(Boolean enableBetaContent) {
        this.enableBetaContent = enableBetaContent;
    }

    public void setEscapeChar(Character escapeChar) {
        this.escapeChar = escapeChar;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    @Override
    public void setLogWriter(PrintWriter logWriter) {
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPortNumber(Integer portNumber) {
        this.portNumber = portNumber;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.adapter = (ResourceAdapterImpl) adapter;
    }

    public void setUserName(String user) {
        this.userName = user;
    }
}
