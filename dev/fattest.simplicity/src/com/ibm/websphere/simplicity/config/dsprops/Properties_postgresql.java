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
package com.ibm.websphere.simplicity.config.dsprops;

import javax.xml.bind.annotation.XmlAttribute;

import com.ibm.websphere.simplicity.config.DataSourceProperties;

/**
 * Lists data source properties specific to this driver.
 */
public class Properties_postgresql extends DataSourceProperties {
    private String URL;
    private String applicationName;
    private String cancelSignalTimeout;
    private String connectionTimeout;
    private String currentSchema;
    private String defaultRowFetchSize;
    private String preparedStatementCacheQueries;
    private String readOnly;
    private String socketTimeout;
    private String ssl;
    private String sslCert;
    private String sslMode;
    private String sslPassword;
    private String sslRootCert;
    private String sslFactory;
    private String targetServerType;
    private String tcpKeepAlive;

    @Override
    public String getElementName() {
        return POSTGRESQL;
    }

    public String getURL() {
        return URL;
    }

    @XmlAttribute(name = "URL")
    public void setURL(String uRL) {
        URL = uRL;
    }

    public String getApplicationName() {
        return applicationName;
    }

    @XmlAttribute(name = "applicationName")
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getCancelSignalTimeout() {
        return cancelSignalTimeout;
    }

    @XmlAttribute(name = "cancelSignalTimeout")
    public void setCancelSignalTimeout(String cancelSignalTimeout) {
        this.cancelSignalTimeout = cancelSignalTimeout;
    }

    public String getConnectionTimeout() {
        return connectionTimeout;
    }

    @XmlAttribute(name = "connectionTimeout")
    public void setConnectionTimeout(String connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public String getCurrentSchema() {
        return currentSchema;
    }

    @XmlAttribute(name = "currentSchema")
    public void setCurrentSchema(String currentSchema) {
        this.currentSchema = currentSchema;
    }

    public String getDefaultRowFetchSize() {
        return defaultRowFetchSize;
    }

    @XmlAttribute(name = "defaultRowFetchSize")
    public void setDefaultRowFetchSize(String defaultRowFetchSize) {
        this.defaultRowFetchSize = defaultRowFetchSize;
    }

    public String getPreparedStatementCacheQueries() {
        return preparedStatementCacheQueries;
    }

    @XmlAttribute(name = "preparedStatementCacheQueries")
    public void setPreparedStatementCacheQueries(String preparedStatementCacheQueries) {
        this.preparedStatementCacheQueries = preparedStatementCacheQueries;
    }

    public String getReadOnly() {
        return readOnly;
    }

    @XmlAttribute(name = "readOnly")
    public void setReadOnly(String readOnly) {
        this.readOnly = readOnly;
    }

    public String getSocketTimeout() {
        return socketTimeout;
    }

    @XmlAttribute(name = "socketTimeout")
    public void setSocketTimeout(String socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public String getSsl() {
        return ssl;
    }

    @XmlAttribute(name = "ssl")
    public void setSsl(String ssl) {
        this.ssl = ssl;
    }

    public String getSslCert() {
        return sslCert;
    }

    @XmlAttribute(name = "sslCert")
    public void setSslCert(String sslCert) {
        this.sslCert = sslCert;
    }

    public String getSslMode() {
        return sslMode;
    }

    @XmlAttribute(name = "sslMode")
    public void setSslMode(String sslMode) {
        this.sslMode = sslMode;
    }

    public String getSslPassword() {
        return sslPassword;
    }

    @XmlAttribute(name = "sslPassword")
    public void setSslPassword(String sslPassword) {
        this.sslPassword = sslPassword;
    }

    public String getSslRootCert() {
        return sslRootCert;
    }

    @XmlAttribute(name = "sslRootCert")
    public void setSslRootCert(String sslRootCert) {
        this.sslRootCert = sslRootCert;
    }

    public String getSslFactory() {
        return sslFactory;
    }

    @XmlAttribute(name = "sslFactory")
    public void setSslFactory(String sslFactory) {
        this.sslFactory = sslFactory;
    }

    public String getTargetServerType() {
        return targetServerType;
    }

    @XmlAttribute(name = "targetServerType")
    public void setTargetServerType(String targetServerType) {
        this.targetServerType = targetServerType;
    }

    public String getTcpKeepAlive() {
        return tcpKeepAlive;
    }

    @XmlAttribute(name = "tcpKeepAlive")
    public void setTcpKeepAlive(String tcpKeepAlive) {
        this.tcpKeepAlive = tcpKeepAlive;
    }

    /**
     * Returns a String listing the properties and their values used on this
     * data source.
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("{");
        if (super.getDatabaseName() != null)
            buf.append("databaseName=\"" + super.getDatabaseName() + "\" ");
        if (super.getPortNumber() != null)
            buf.append("portNumber=\"" + super.getPortNumber() + "\" ");
        if (super.getServerName() != null)
            buf.append("serverName=\"" + super.getServerName() + "\" ");
        if (URL != null)
            buf.append("URL=\"" + URL + "\" ");
        if (applicationName != null)
            buf.append("applicationName=\"" + applicationName + "\" ");
        if (cancelSignalTimeout != null)
            buf.append("cancelSignalTimeout=\"" + cancelSignalTimeout + "\" ");
        if (connectionTimeout != null)
            buf.append("connectionTimeout=\"" + connectionTimeout + "\" ");
        if (currentSchema != null)
            buf.append("currentSchema=\"" + currentSchema + "\" ");
        if (defaultRowFetchSize != null)
            buf.append("defaultRowFetchSize=\"" + defaultRowFetchSize + "\" ");
        if (super.getLoginTimeout() != null)
            buf.append("loginTimeout=\"" + super.getLoginTimeout() + "\" ");
        if (super.getPassword() != null)
            buf.append("password=\"" + super.getPassword() + "\" ");
        if (preparedStatementCacheQueries != null)
            buf.append("preparedStatementCacheQueries=\"" + preparedStatementCacheQueries + "\" ");
        if (readOnly != null)
            buf.append("readOnly=\"" + readOnly + "\" ");
        if (socketTimeout != null)
            buf.append("socketTimeout=\"" + socketTimeout + "\" ");
        if (ssl != null)
            buf.append("ssl=\"" + ssl + "\" ");
        if (sslCert != null)
            buf.append("sslCert=\"" + sslCert + "\" ");
        if (sslMode != null)
            buf.append("sslMode=\"" + sslMode + "\" ");
        if (sslPassword != null)
            buf.append("sslPassword=\"" + sslPassword + "\" ");
        if (sslRootCert != null)
            buf.append("sslRootCert=\"" + sslRootCert + "\" ");
        if (sslFactory != null)
            buf.append("sslFactory=\"" + sslFactory + "\" ");
        if (targetServerType != null)
            buf.append("targetServerType=\"" + targetServerType + "\" ");
        if (tcpKeepAlive != null)
            buf.append("tcpKeepAlive=\"" + tcpKeepAlive + "\" ");
        if (super.getUser() != null)
            buf.append("user=\"" + super.getUser() + "\" ");
        buf.append("}");
        return buf.toString();
    }

}