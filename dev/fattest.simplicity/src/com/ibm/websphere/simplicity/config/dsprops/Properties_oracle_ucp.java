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
package com.ibm.websphere.simplicity.config.dsprops;

import javax.xml.bind.annotation.XmlAttribute;

import com.ibm.websphere.simplicity.config.DataSourceProperties;

/**
 * Lists data source properties specific to this driver.
 */
public class Properties_oracle_ucp extends DataSourceProperties {
    private String ONSConfiguration;
    private String SQLForValidateConnection;
    private String URL;
    private String abandonedConnectionTimeout;
    private String connectionFactoryClassName;
    private String connectionFactoryProperties;
    private String connectionHarvestMaxCount;
    private String connectionHarvestTriggerCount;
    private String connectionPoolName;
    private String connectionProperties;
    private String connectionWaitTimeout;
    private String fastConnectionFailoverEnabled;
    private String inactiveConnectionTimeout;
    private String initialPoolSize;
    private String maxConnectionReuseCount;
    private String maxConnectionReuseTime;
    private String maxIdleTime;
    private String maxPoolSize;
    private String maxStatements;
    private String minPoolSize;
    private String networkProtocol;
    private String propertyCycle;
    private String roleName;
    private String timeToLiveConnectionTimeout;
    private String timeoutCheckInterval;
    private String validateConnectionOnBorrow;

    @Override
    public String getElementName() {
        return ORACLE_UCP;
    }

    @XmlAttribute(name = "ONSConfiguration")
    public void setONSConfiguration(String ONSConfiguration) {
        this.ONSConfiguration = ONSConfiguration;
    }

    public String getONSConfiguration() {
        return this.ONSConfiguration;
    }

    @XmlAttribute(name = "SQLForValidateConnection")
    public void setSQLForValidateConnection(String SQLForValidateConnection) {
        this.SQLForValidateConnection = SQLForValidateConnection;
    }

    public String getSQLForValidateConnection() {
        return this.SQLForValidateConnection;
    }

    @XmlAttribute(name = "URL")
    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getURL() {
        return this.URL;
    }

    @XmlAttribute(name = "abandonedConnectionTimeout")
    public void setAbandonedConnectionTimeout(String abandonedConnectionTimeout) {
        this.abandonedConnectionTimeout = abandonedConnectionTimeout;
    }

    public String getAbandonedConnectionTimeout() {
        return this.abandonedConnectionTimeout;
    }

    @XmlAttribute(name = "connectionFactoryClassName")
    public void setConnectionFactoryClassName(String connectionFactoryClassName) {
        this.connectionFactoryClassName = connectionFactoryClassName;
    }

    public String getConnectionFactoryClassName() {
        return this.connectionFactoryClassName;
    }

    @XmlAttribute(name = "connectionFactoryProperties")
    public void setConnectionFactoryProperties(String connectionFactoryProperties) {
        this.connectionFactoryProperties = connectionFactoryProperties;
    }

    public String getConnectionFactoryProperties() {
        return this.connectionFactoryProperties;
    }

    @XmlAttribute(name = "connectionHarvestMaxCount")
    public void setConnectionHarvestMaxCount(String connectionHarvestMaxCount) {
        this.connectionHarvestMaxCount = connectionHarvestMaxCount;
    }

    public String getConnectionHarvestMaxCount() {
        return this.connectionHarvestMaxCount;
    }

    @XmlAttribute(name = "connectionHarvestTriggerCount")
    public void setConnectionHarvestTriggerCount(String connectionHarvestTriggerCount) {
        this.connectionHarvestTriggerCount = connectionHarvestTriggerCount;
    }

    public String getConnectionHarvestTriggerCount() {
        return this.connectionHarvestTriggerCount;
    }

    @XmlAttribute(name = "connectionPoolName")
    public void setConnectionPoolName(String connectionPoolName) {
        this.connectionPoolName = connectionPoolName;
    }

    public String getConnectionPoolName() {
        return this.connectionPoolName;
    }

    @XmlAttribute(name = "connectionProperties")
    public void setConnectionProperties(String connectionProperties) {
        this.connectionProperties = connectionProperties;
    }

    public String getConnectionProperties() {
        return this.connectionProperties;
    }

    @XmlAttribute(name = "connectionWaitTimeout")
    public void setConnectionWaitTimeout(String connectionWaitTimeout) {
        this.connectionWaitTimeout = connectionWaitTimeout;
    }

    public String getConnectionWaitTimeout() {
        return this.connectionWaitTimeout;
    }

    @XmlAttribute(name = "fastConnectionFailoverEnabled")
    public void setFastConnectionFailoverEnabled(String fastConnectionFailoverEnabled) {
        this.fastConnectionFailoverEnabled = fastConnectionFailoverEnabled;
    }

    public String getFastConnectionFailoverEnabled() {
        return this.fastConnectionFailoverEnabled;
    }

    @XmlAttribute(name = "inactiveConnectionTimeout")
    public void setInactiveConnectionTimeout(String inactiveConnectionTimeout) {
        this.inactiveConnectionTimeout = inactiveConnectionTimeout;
    }

    public String getInactiveConnectionTimeout() {
        return this.inactiveConnectionTimeout;
    }

    @XmlAttribute(name = "initialPoolSize")
    public void setInitialPoolSize(String initialPoolSize) {
        this.initialPoolSize = initialPoolSize;
    }

    public String getInitialPoolSize() {
        return this.initialPoolSize;
    }

    @XmlAttribute(name = "maxConnectionReuseCount")
    public void setMaxConnectionReuseCount(String maxConnectionReuseCount) {
        this.maxConnectionReuseCount = maxConnectionReuseCount;
    }

    public String getMaxConnectionReuseCount() {
        return this.maxConnectionReuseCount;
    }

    @XmlAttribute(name = "maxConnectionReuseTime")
    public void setMaxConnectionReuseTime(String maxConnectionReuseTime) {
        this.maxConnectionReuseTime = maxConnectionReuseTime;
    }

    public String getMaxConnectionReuseTime() {
        return this.maxConnectionReuseTime;
    }

    @XmlAttribute(name = "maxIdleTime")
    public void setMaxIdleTime(String maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    public String getMaxIdleTime() {
        return this.maxIdleTime;
    }

    @XmlAttribute(name = "maxPoolSize")
    public void setMaxPoolSize(String maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public String getMaxPoolSize() {
        return this.maxPoolSize;
    }

    @XmlAttribute(name = "maxStatements")
    public void setMaxStatements(String maxStatements) {
        this.maxStatements = maxStatements;
    }

    public String getMaxStatements() {
        return this.maxStatements;
    }

    @XmlAttribute(name = "minPoolSize")
    public void setMinPoolSize(String minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public String getMinPoolSize() {
        return this.minPoolSize;
    }

    @XmlAttribute(name = "networkProtocol")
    public void setNetworkProtocol(String networkProtocol) {
        this.networkProtocol = networkProtocol;
    }

    public String getNetworkProtocol() {
        return this.networkProtocol;
    }

    @XmlAttribute(name = "propertyCycle")
    public void setPropertyCycle(String propertyCycle) {
        this.propertyCycle = propertyCycle;
    }

    public String getPropertyCycle() {
        return this.propertyCycle;
    }

    @XmlAttribute(name = "roleName")
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleName() {
        return this.roleName;
    }

    @XmlAttribute(name = "timeToLiveConnectionTimeout")
    public void setTimeToLiveConnectionTimeout(String timeToLiveConnectionTimeout) {
        this.timeToLiveConnectionTimeout = timeToLiveConnectionTimeout;
    }

    public String getTimeToLiveConnectionTimeout() {
        return this.timeToLiveConnectionTimeout;
    }

    @XmlAttribute(name = "timeoutCheckInterval")
    public void setTimeoutCheckInterval(String timeoutCheckInterval) {
        this.timeoutCheckInterval = timeoutCheckInterval;
    }

    public String getTimeoutCheckInterval() {
        return this.timeoutCheckInterval;
    }

    @XmlAttribute(name = "validateConnectionOnBorrow")
    public void setValidateConnectionOnBorrow(String validateConnectionOnBorrow) {
        this.validateConnectionOnBorrow = validateConnectionOnBorrow;
    }

    public String getValidateConnectionOnBorrow() {
        return this.validateConnectionOnBorrow;
    }

    /**
     * Returns a String listing the properties and their values used on this
     * data source.
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("{");
        if (ONSConfiguration != null)
            buf.append("ONSConfiguration=\"" + ONSConfiguration + "\" ");
        if (SQLForValidateConnection != null)
            buf.append("SQLForValidateConnection=\"" + SQLForValidateConnection + "\" ");
        if (URL != null)
            buf.append("URL=\"" + URL + "\" ");
        if (abandonedConnectionTimeout != null)
            buf.append("abandonedConnectionTimeout=\"" + abandonedConnectionTimeout + "\" ");
        if (connectionFactoryClassName != null)
            buf.append("connectionFactoryClassName=\"" + connectionFactoryClassName + "\" ");
        if (connectionFactoryProperties != null)
            buf.append("connectionFactoryProperties=\"" + connectionFactoryProperties + "\" ");
        if (connectionHarvestMaxCount != null)
            buf.append("connectionHarvestMaxCount=\"" + connectionHarvestMaxCount + "\" ");
        if (connectionHarvestTriggerCount != null)
            buf.append("connectionHarvestTriggerCount=\"" + connectionHarvestTriggerCount + "\" ");
        if (connectionPoolName != null)
            buf.append("connectionPoolName=\"" + connectionPoolName + "\" ");
        if (connectionProperties != null)
            buf.append("connectionProperties=\"" + connectionProperties + "\" ");
        if (connectionWaitTimeout != null)
            buf.append("connectionWaitTimeout=\"" + connectionWaitTimeout + "\" ");
        if (super.getDatabaseName() != null)
            buf.append("databaseName=\"" + super.getDatabaseName() + "\" ");
        if (fastConnectionFailoverEnabled != null)
            buf.append("fastConnectionFailoverEnabled=\"" + fastConnectionFailoverEnabled + "\" ");
        if (inactiveConnectionTimeout != null)
            buf.append("inactiveConnectionTimeout=\"" + inactiveConnectionTimeout + "\" ");
        if (initialPoolSize != null)
            buf.append("initialPoolSize=\"" + initialPoolSize + "\" ");
        if (super.getLoginTimeout() != null)
            buf.append("loginTimeout=\"" + super.getLoginTimeout() + "\" ");
        if (maxConnectionReuseCount != null)
            buf.append("maxConnectionReuseCount=\"" + maxConnectionReuseCount + "\" ");
        if (maxConnectionReuseTime != null)
            buf.append("maxConnectionReuseTime=\"" + maxConnectionReuseTime + "\" ");
        if (maxIdleTime != null)
            buf.append("maxIdleTime=\"" + maxIdleTime + "\" ");
        if (maxPoolSize != null)
            buf.append("maxPoolSize=\"" + maxPoolSize + "\" ");
        if (maxStatements != null)
            buf.append("maxStatements=\"" + maxStatements + "\" ");
        if (minPoolSize != null)
            buf.append("minPoolSize=\"" + minPoolSize + "\" ");
        if (networkProtocol != null)
            buf.append("networkProtocol=\"" + networkProtocol + "\" ");
        if (super.getPassword() != null)
            buf.append("password=\"" + super.getPassword() + "\" ");
        if (super.getPortNumber() != null)
            buf.append("portNumber=\"" + super.getPortNumber() + "\" ");
        if (propertyCycle != null)
            buf.append("propertyCycle=\"" + propertyCycle + "\" ");
        if (roleName != null)
            buf.append("roleName=\"" + roleName + "\" ");
        if (super.getServerName() != null)
            buf.append("serverName=\"" + super.getServerName() + "\" ");
        if (timeToLiveConnectionTimeout != null)
            buf.append("timeToLiveConnectionTimeout=\"" + timeToLiveConnectionTimeout + "\" ");
        if (timeoutCheckInterval != null)
            buf.append("timeoutCheckInterval=\"" + timeoutCheckInterval + "\" ");
        if (super.getUser() != null)
            buf.append("user=\"" + super.getUser() + "\" ");
        if (validateConnectionOnBorrow != null)
            buf.append("validateConnectionOnBorrow=\"" + validateConnectionOnBorrow + "\" ");
        buf.append("}");
        return buf.toString();
    }

}