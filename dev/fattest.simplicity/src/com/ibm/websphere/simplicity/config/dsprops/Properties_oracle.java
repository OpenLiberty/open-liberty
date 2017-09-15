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
public class Properties_oracle extends DataSourceProperties {
    private String ONSConfiguration;
    private String TNSEntryName;
    private String URL;
    private String connectionProperties;
    private String driverType;
    private String networkProtocol;
    private String serviceName;

    @Override
    public String getElementName() {
        return ORACLE_JDBC;
    }

    @XmlAttribute(name = "ONSConfiguration")
    public void setONSConfiguration(String ONSConfiguration) {
        this.ONSConfiguration = ONSConfiguration;
    }

    public String getONSConfiguration() {
        return this.ONSConfiguration;
    }

    @XmlAttribute(name = "TNSEntryName")
    public void setTNSEntryName(String TNSEntryName) {
        this.TNSEntryName = TNSEntryName;
    }

    public String getTNSEntryName() {
        return this.TNSEntryName;
    }

    @XmlAttribute(name = "URL")
    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getURL() {
        return this.URL;
    }

    @XmlAttribute(name = "connectionProperties")
    public void setConnectionProperties(String connectionProperties) {
        this.connectionProperties = connectionProperties;
    }

    public String getConnectionProperties() {
        return this.connectionProperties;
    }

    @XmlAttribute(name = "driverType")
    public void setDriverType(String driverType) {
        this.driverType = driverType;
    }

    public String getDriverType() {
        return this.driverType;
    }

    @XmlAttribute(name = "networkProtocol")
    public void setNetworkProtocol(String networkProtocol) {
        this.networkProtocol = networkProtocol;
    }

    public String getNetworkProtocol() {
        return this.networkProtocol;
    }

    @XmlAttribute(name = "serviceName")
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return this.serviceName;
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
        if (TNSEntryName != null)
            buf.append("TNSEntryName=\"" + TNSEntryName + "\" ");
        if (URL != null)
            buf.append("URL=\"" + URL + "\" ");
        if (connectionProperties != null)
            buf.append("connectionProperties=\"" + connectionProperties + "\" ");
        if (super.getDatabaseName() != null)
            buf.append("databaseName=\"" + super.getDatabaseName() + "\" ");
        if (driverType != null)
            buf.append("driverType=\"" + driverType + "\" ");
        if (super.getLoginTimeout() != null)
            buf.append("loginTimeout=\"" + super.getLoginTimeout() + "\" ");
        if (networkProtocol != null)
            buf.append("networkProtocol=\"" + networkProtocol + "\" ");
        if (super.getPassword() != null)
            buf.append("password=\"" + super.getPassword() + "\" ");
        if (super.getPortNumber() != null)
            buf.append("portNumber=\"" + super.getPortNumber() + "\" ");
        if (super.getServerName() != null)
            buf.append("serverName=\"" + super.getServerName() + "\" ");
        if (serviceName != null)
            buf.append("serviceName=\"" + serviceName + "\" ");
        if (super.getUser() != null)
            buf.append("user=\"" + super.getUser() + "\" ");
        buf.append("}");
        return buf.toString();
    }

}