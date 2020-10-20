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

package com.ibm.tra.ann;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.ConnectionDefinition;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;

@SuppressWarnings("serial")
@ConnectionDefinition(connectionFactory = ConfigPropertyConnectionFactoryIntf.class,
                      connectionFactoryImpl = ConfigPropertyConnectionFactory.class,
                      connection = javax.resource.cci.Connection.class,
                      connectionImpl = ConfigPropertyConnection.class)
public class ConfigPropertyManagedConnectionFactory implements ManagedConnectionFactory, Serializable {

    private String serverName = null;

    private Integer portNumber = null;

    @ConfigProperty(type = java.lang.String.class, defaultValue = "CDTest")
    private String user1 = null;

    @ConfigProperty(type = java.lang.String.class, defaultValue = "CDPass", ignore = true, confidential = true)
    private String password1 = null;

    @ConfigProperty(type = java.lang.String.class, defaultValue = "CDConf", ignore = true, confidential = true)
    private String conf1 = null;

    @Override
    public Object createConnectionFactory() throws ResourceException {
        return null;
    }

    @Override
    public Object createConnectionFactory(ConnectionManager arg0) throws ResourceException {
        return null;
    }

    @Override
    public ManagedConnection createManagedConnection(Subject arg0, ConnectionRequestInfo arg1) throws ResourceException {
        return null;
    }

    public String getConf1() {
        return conf1;
    }

    public void setConf1(String conf) {
        this.conf1 = conf;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return null;
    }

    @Override
    public ManagedConnection matchManagedConnections(Set arg0, Subject arg1, ConnectionRequestInfo arg2) throws ResourceException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter arg0) throws ResourceException {}

    public String getPassword1() {
        return password1;
    }

    public void setPassword1(String password) {
        this.password1 = password;
    }

    public Integer getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(Integer portNumber) {
        this.portNumber = portNumber;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getUser1() {
        return user1;
    }

    public void setUser1(String user) {
        this.user1 = user;
    }

}
