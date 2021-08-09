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
public class Properties_sybase extends DataSourceProperties {
    private String connectionProperties;
    private String networkProtocol;
    private String resourceManagerName;
    private String SERVER_INITIATED_TRANSACTIONS;
    private String version;

    @Override
    public String getElementName() {
        return SYBASE;
    }

    @XmlAttribute(name = "connectionProperties")
    public void setConnectionProperties(String connectionProperties) {
        this.connectionProperties = connectionProperties;
    }

    public String getConnectionProperties() {
        return this.connectionProperties;
    }

    @XmlAttribute(name = "networkProtocol")
    public void setNetworkProtocol(String networkProtocol) {
        this.networkProtocol = networkProtocol;
    }

    public String getNetworkProtocol() {
        return this.networkProtocol;
    }

    @XmlAttribute(name = "resourceManagerName")
    public void setResourceManagerName(String resourceManagerName) {
        this.resourceManagerName = resourceManagerName;
    }

    public String getResourceManagerName() {
        return this.resourceManagerName;
    }

    @XmlAttribute(name = "SERVER_INITIATED_TRANSACTIONS")
    public void setSERVER_INITIATED_TRANSACTIONS(String SERVER_INITIATED_TRANSACTIONS) {
        this.SERVER_INITIATED_TRANSACTIONS = SERVER_INITIATED_TRANSACTIONS;
    }

    public String getSERVER_INITIATED_TRANSACTIONS() {
        return this.SERVER_INITIATED_TRANSACTIONS;
    }

    @XmlAttribute(name = "version")
    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return this.version;
    }

    /**
     * Returns a String listing the properties and their values used on this
     * data source.
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("{");
        if (connectionProperties != null)
            buf.append("connectionProperties=\"" + connectionProperties + "\" ");
        if (super.getDatabaseName() != null)
            buf.append("databaseName=\"" + super.getDatabaseName() + "\" ");
        if (super.getLoginTimeout() != null)
            buf.append("loginTimeout=\"" + super.getLoginTimeout() + "\" ");
        if (networkProtocol != null)
            buf.append("networkProtocol=\"" + networkProtocol + "\" ");
        if (super.getPassword() != null)
            buf.append("password=\"" + super.getPassword() + "\" ");
        if (super.getPortNumber() != null)
            buf.append("portNumber=\"" + super.getPortNumber() + "\" ");
        if (resourceManagerName != null)
            buf.append("resourceManagerName=\"" + resourceManagerName + "\" ");
        if (SERVER_INITIATED_TRANSACTIONS != null)
            buf.append("SERVER_INITIATED_TRANSACTIONS=\"" + SERVER_INITIATED_TRANSACTIONS + "\" ");
        if (super.getServerName() != null)
            buf.append("serverName=\"" + super.getServerName() + "\" ");
        if (super.getUser() != null)
            buf.append("user=\"" + super.getUser() + "\" ");
        if (version != null)
            buf.append("version=\"" + version + "\" ");
        buf.append("}");
        return buf.toString();
    }

}