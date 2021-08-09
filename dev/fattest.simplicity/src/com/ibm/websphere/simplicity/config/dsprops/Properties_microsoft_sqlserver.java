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
public class Properties_microsoft_sqlserver extends DataSourceProperties {
    private String URL;
    private String applicationIntent;
    private String applicationName;
    private String authenticationScheme;
    private String encrypt;
    private String failoverPartner;
    private String hostNameInCertificate;
    private String instanceName;
    private String integratedSecurity;
    private String lastUpdateCount;
    private String lockTimeout;
    private String multiSubnetFailover;
    private String packetSize;
    private String responseBuffering;
    private String selectMethod;
    private String sendStringParametersAsUnicode;
    private String sendTimeAsDatetime;
    private String trustServerCertificate;
    private String trustStore;
    private String trustStorePassword;
    private String workstationID;
    private String xopenStates;

    @Override
    public String getElementName() {
        return MICROSOFT_SQLSERVER;
    }

    @XmlAttribute(name = "URL")
    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getURL() {
        return this.URL;
    }

    @XmlAttribute(name = "applicationIntent")
    public void setApplicationIntent(String applicationIntent) {
        this.applicationIntent = applicationIntent;
    }

    public String getApplicationIntent() {
        return this.applicationIntent;
    }

    @XmlAttribute(name = "applicationName")
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationName() {
        return this.applicationName;
    }

    @XmlAttribute(name = "authenticationScheme")
    public void setAuthenticationScheme(String authenticationScheme) {
        this.authenticationScheme = authenticationScheme;
    }

    public String getAuthenticationScheme() {
        return this.authenticationScheme;
    }

    @XmlAttribute(name = "encrypt")
    public void setEncrypt(String encrypt) {
        this.encrypt = encrypt;
    }

    public String getEncrypt() {
        return this.encrypt;
    }

    @XmlAttribute(name = "failoverPartner")
    public void setFailoverPartner(String failoverPartner) {
        this.failoverPartner = failoverPartner;
    }

    public String getFailoverPartner() {
        return this.failoverPartner;
    }

    @XmlAttribute(name = "hostNameInCertificate")
    public void setHostNameInCertificate(String hostNameInCertificate) {
        this.hostNameInCertificate = hostNameInCertificate;
    }

    public String getHostNameInCertificate() {
        return this.hostNameInCertificate;
    }

    @XmlAttribute(name = "instanceName")
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getInstanceName() {
        return this.instanceName;
    }

    @XmlAttribute(name = "integratedSecurity")
    public void setIntegratedSecurity(String integratedSecurity) {
        this.integratedSecurity = integratedSecurity;
    }

    public String getIntegratedSecurity() {
        return this.integratedSecurity;
    }

    @XmlAttribute(name = "lastUpdateCount")
    public void setLastUpdateCount(String lastUpdateCount) {
        this.lastUpdateCount = lastUpdateCount;
    }

    public String getLastUpdateCount() {
        return this.lastUpdateCount;
    }

    @XmlAttribute(name = "lockTimeout")
    public void setLockTimeout(String lockTimeout) {
        this.lockTimeout = lockTimeout;
    }

    public String getLockTimeout() {
        return this.lockTimeout;
    }

    @XmlAttribute(name = "multiSubnetFailover")
    public void setMultiSubnetFailover(String multiSubnetFailover) {
        this.multiSubnetFailover = multiSubnetFailover;
    }

    public String getMultiSubnetFailover() {
        return this.multiSubnetFailover;
    }

    @XmlAttribute(name = "packetSize")
    public void setPacketSize(String packetSize) {
        this.packetSize = packetSize;
    }

    public String getPacketSize() {
        return this.packetSize;
    }

    @XmlAttribute(name = "responseBuffering")
    public void setResponseBuffering(String responseBuffering) {
        this.responseBuffering = responseBuffering;
    }

    public String getResponseBuffering() {
        return this.responseBuffering;
    }

    @XmlAttribute(name = "selectMethod")
    public void setSelectMethod(String selectMethod) {
        this.selectMethod = selectMethod;
    }

    public String getSelectMethod() {
        return this.selectMethod;
    }

    @XmlAttribute(name = "sendStringParametersAsUnicode")
    public void setSendStringParametersAsUnicode(String sendStringParametersAsUnicode) {
        this.sendStringParametersAsUnicode = sendStringParametersAsUnicode;
    }

    public String getSendStringParametersAsUnicode() {
        return this.sendStringParametersAsUnicode;
    }

    @XmlAttribute(name = "sendTimeAsDatetime")
    public void setSendTimeAsDatetime(String sendTimeAsDatetime) {
        this.sendTimeAsDatetime = sendTimeAsDatetime;
    }

    public String getSendTimeAsDatetime() {
        return this.sendTimeAsDatetime;
    }

    @XmlAttribute(name = "trustServerCertificate")
    public void setTrustServerCertificate(String trustServerCertificate) {
        this.trustServerCertificate = trustServerCertificate;
    }

    public String getTrustServerCertificate() {
        return this.trustServerCertificate;
    }

    @XmlAttribute(name = "trustStore")
    public void setTrustStore(String trustStore) {
        this.trustStore = trustStore;
    }

    public String getTrustStore() {
        return this.trustStore;
    }

    @XmlAttribute(name = "trustStorePassword")
    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public String getTrustStorePassword() {
        return this.trustStorePassword;
    }

    @XmlAttribute(name = "workstationID")
    public void setWorkstationID(String workstationID) {
        this.workstationID = workstationID;
    }

    public String getWorkstationID() {
        return this.workstationID;
    }

    @XmlAttribute(name = "xopenStates")
    public void setXopenStates(String xopenStates) {
        this.xopenStates = xopenStates;
    }

    public String getXopenStates() {
        return this.xopenStates;
    }

    /**
     * Returns a String listing the properties and their values used on this
     * data source.
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("{");
        if (URL != null)
            buf.append("URL=\"" + URL + "\" ");
        if (applicationIntent != null)
            buf.append("applicationIntent=\"" + applicationIntent + "\" ");
        if (applicationName != null)
            buf.append("applicationName=\"" + applicationName + "\" ");
        if (authenticationScheme != null)
            buf.append("authenticationScheme=\"" + authenticationScheme + "\" ");
        if (super.getDatabaseName() != null)
            buf.append("databaseName=\"" + super.getDatabaseName() + "\" ");
        if (encrypt != null)
            buf.append("encrypt=\"" + encrypt + "\" ");
        if (failoverPartner != null)
            buf.append("failoverPartner=\"" + failoverPartner + "\" ");
        if (hostNameInCertificate != null)
            buf.append("hostNameInCertificate=\"" + hostNameInCertificate + "\" ");
        if (instanceName != null)
            buf.append("instanceName=\"" + instanceName + "\" ");
        if (integratedSecurity != null)
            buf.append("integratedSecurity=\"" + integratedSecurity + "\" ");
        if (lastUpdateCount != null)
            buf.append("lastUpdateCount=\"" + lastUpdateCount + "\" ");
        if (lockTimeout != null)
            buf.append("lockTimeout=\"" + lockTimeout + "\" ");
        if (super.getLoginTimeout() != null)
            buf.append("loginTimeout=\"" + super.getLoginTimeout() + "\" ");
        if (multiSubnetFailover != null)
            buf.append("multiSubnetFailover=\"" + multiSubnetFailover + "\" ");
        if (packetSize != null)
            buf.append("packetSize=\"" + packetSize + "\" ");
        if (super.getPassword() != null)
            buf.append("password=\"" + super.getPassword() + "\" ");
        if (super.getPortNumber() != null)
            buf.append("portNumber=\"" + super.getPortNumber() + "\" ");
        if (responseBuffering != null)
            buf.append("responseBuffering=\"" + responseBuffering + "\" ");
        if (selectMethod != null)
            buf.append("selectMethod=\"" + selectMethod + "\" ");
        if (sendStringParametersAsUnicode != null)
            buf.append("sendStringParametersAsUnicode=\"" + sendStringParametersAsUnicode + "\" ");
        if (sendTimeAsDatetime != null)
            buf.append("sendTimeAsDatetime=\"" + sendTimeAsDatetime + "\" ");
        if (super.getServerName() != null)
            buf.append("serverName=\"" + super.getServerName() + "\" ");
        if (trustServerCertificate != null)
            buf.append("trustServerCertificate=\"" + trustServerCertificate + "\" ");
        if (trustStore != null)
            buf.append("trustStore=\"" + trustStore + "\" ");
        if (trustStorePassword != null)
            buf.append("trustStorePassword=\"" + trustStorePassword + "\" ");
        if (super.getUser() != null)
            buf.append("user=\"" + super.getUser() + "\" ");
        if (workstationID != null)
            buf.append("workstationID=\"" + workstationID + "\" ");
        if (xopenStates != null)
            buf.append("xopenStates=\"" + xopenStates + "\" ");
        buf.append("}");
        return buf.toString();
    }

}