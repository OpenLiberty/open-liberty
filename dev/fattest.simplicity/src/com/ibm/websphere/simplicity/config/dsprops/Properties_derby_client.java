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
public class Properties_derby_client extends DataSourceProperties {
    private String connectionAttributes;
    private String createDatabase;
    private String retrieveMessageText;
    private String securityMechanism;
    private String shutdownDatabase;
    private String ssl;
    private String traceDirectory;
    private String traceFile;
    private String traceFileAppend;
    private String traceLevel;

    @Override
    public String getElementName() {
        return DERBY_CLIENT;
    }

    @XmlAttribute(name = "connectionAttributes")
    public void setConnectionAttributes(String connectionAttributes) {
        this.connectionAttributes = connectionAttributes;
    }

    public String getConnectionAttributes() {
        return this.connectionAttributes;
    }

    @XmlAttribute(name = "createDatabase")
    public void setCreateDatabase(String createDatabase) {
        this.createDatabase = createDatabase;
    }

    public String getCreateDatabase() {
        return this.createDatabase;
    }

    @XmlAttribute(name = "retrieveMessageText")
    public void setRetrieveMessageText(String retrieveMessageText) {
        this.retrieveMessageText = retrieveMessageText;
    }

    public String getRetrieveMessageText() {
        return this.retrieveMessageText;
    }

    @XmlAttribute(name = "securityMechanism")
    public void setSecurityMechanism(String securityMechanism) {
        this.securityMechanism = securityMechanism;
    }

    public String getSecurityMechanism() {
        return this.securityMechanism;
    }

    @XmlAttribute(name = "shutdownDatabase")
    public void setShutdownDatabase(String shutdownDatabase) {
        this.shutdownDatabase = shutdownDatabase;
    }

    public String getShutdownDatabase() {
        return this.shutdownDatabase;
    }

    @XmlAttribute(name = "ssl")
    public void setSsl(String ssl) {
        this.ssl = ssl;
    }

    public String getSsl() {
        return this.ssl;
    }

    @XmlAttribute(name = "traceDirectory")
    public void setTraceDirectory(String traceDirectory) {
        this.traceDirectory = traceDirectory;
    }

    public String getTraceDirectory() {
        return this.traceDirectory;
    }

    @XmlAttribute(name = "traceFile")
    public void setTraceFile(String traceFile) {
        this.traceFile = traceFile;
    }

    public String getTraceFile() {
        return this.traceFile;
    }

    @XmlAttribute(name = "traceFileAppend")
    public void setTraceFileAppend(String traceFileAppend) {
        this.traceFileAppend = traceFileAppend;
    }

    public String getTraceFileAppend() {
        return this.traceFileAppend;
    }

    @XmlAttribute(name = "traceLevel")
    public void setTraceLevel(String traceLevel) {
        this.traceLevel = traceLevel;
    }

    public String getTraceLevel() {
        return this.traceLevel;
    }

    /**
     * Returns a String listing the properties and their values used on this
     * data source.
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("{");
        if (connectionAttributes != null)
            buf.append("connectionAttributes=\"" + connectionAttributes + "\" ");
        if (createDatabase != null)
            buf.append("createDatabase=\"" + createDatabase + "\" ");
        if (super.getDatabaseName() != null)
            buf.append("databaseName=\"" + super.getDatabaseName() + "\" ");
        if (super.getLoginTimeout() != null)
            buf.append("loginTimeout=\"" + super.getLoginTimeout() + "\" ");
        if (super.getPassword() != null)
            buf.append("password=\"" + super.getPassword() + "\" ");
        if (super.getPortNumber() != null)
            buf.append("portNumber=\"" + super.getPortNumber() + "\" ");
        if (retrieveMessageText != null)
            buf.append("retrieveMessageText=\"" + retrieveMessageText + "\" ");
        if (securityMechanism != null)
            buf.append("securityMechanism=\"" + securityMechanism + "\" ");
        if (super.getServerName() != null)
            buf.append("serverName=\"" + super.getServerName() + "\" ");
        if (shutdownDatabase != null)
            buf.append("shutdownDatabase=\"" + shutdownDatabase + "\" ");
        if (ssl != null)
            buf.append("ssl=\"" + ssl + "\" ");
        if (traceDirectory != null)
            buf.append("traceDirectory=\"" + traceDirectory + "\" ");
        if (traceFile != null)
            buf.append("traceFile=\"" + traceFile + "\" ");
        if (traceFileAppend != null)
            buf.append("traceFileAppend=\"" + traceFileAppend + "\" ");
        if (traceLevel != null)
            buf.append("traceLevel=\"" + traceLevel + "\" ");
        if (super.getUser() != null)
            buf.append("user=\"" + super.getUser() + "\" ");
        buf.append("}");
        return buf.toString();
    }

}