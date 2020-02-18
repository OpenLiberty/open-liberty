/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

public class DataSourceProperties extends ConfigElement {

    public static final String DB2_I_NATIVE = "properties.db2.i.native";
    public static final String DB2_I_TOOLBOX = "properties.db2.i.toolbox";
    public static final String DB2_JCC = "properties.db2.jcc";
    public static final String DERBY_EMBEDDED = "properties.derby.embedded";
    public static final String DERBY_CLIENT = "properties.derby.client";
    public static final String GENERIC = "properties";
    public static final String INFORMIX_JCC = "properties.informix.jcc";
    public static final String INFORMIX_JDBC = "properties.informix";
    public static final String ORACLE_JDBC = "properties.oracle";
    public static final String ORACLE_UCP = "properties.oracle.ucp";
    public static final String POSTGRESQL = "properties.postgresql";
    public static final String DATADIRECT_SQLSERVER = "properties.datadirect.sqlserver";
    public static final String MICROSOFT_SQLSERVER = "properties.microsoft.sqlserver";
    public static final String SYBASE = "properties.sybase";

    private String databaseName;
    private String loginTimeout;
    private String password;
    private String portNumber;
    private String serverName;
    private String user;
    private String badVendorProperty;

    public String getElementName() {
        return GENERIC;
    }

    @XmlAttribute(name = "databaseName")
    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getDatabaseName() {
        return this.databaseName;
    }

    @XmlAttribute(name = "loginTimeout")
    public void setLoginTimeout(String loginTimeout) {
        this.loginTimeout = loginTimeout;
    }

    public String getLoginTimeout() {
        return this.loginTimeout;
    }

    @XmlAttribute(name = "password")
    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return this.password;
    }

    @XmlAttribute(name = "portNumber")
    public void setPortNumber(String portNumber) {
        this.portNumber = portNumber;
    }

    public String getPortNumber() {
        return this.portNumber;
    }

    @XmlAttribute(name = "serverName")
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerName() {
        return this.serverName;
    }

    @XmlAttribute(name = "user")
    public void setUser(String user) {
        this.user = user;
    }

    public String getUser() {
        return this.user;
    }

    public String getBadVendorProperty() {
        return badVendorProperty;
    }

    @XmlAttribute(name = "badVendorProperty")
    public void setBadVendorProperty(String badVendorProperty) {
        this.badVendorProperty = badVendorProperty;
    }
}