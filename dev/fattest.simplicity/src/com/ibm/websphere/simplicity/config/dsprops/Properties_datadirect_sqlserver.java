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
public class Properties_datadirect_sqlserver extends DataSourceProperties {
    private String JDBCBehavior;
    private String XATransactionGroup;
    private String XMLDescribeType;
    private String accountingInfo;
    private String alternateServers;
    private String alwaysReportTriggerResults;
    private String applicationName;
    private String authenticationMethod;
    private String bulkLoadBatchSize;
    private String bulkLoadOptions;
    private String clientHostName;
    private String clientUser;
    private String codePageOverride;
    private String connectionRetryCount;
    private String connectionRetryDelay;
    private String convertNull;
    private String dateTimeInputParameterType;
    private String dateTimeOutputParameterType;
    private String describeInputParameters;
    private String describeOutputParameters;
    private String enableBulkLoad;
    private String enableCancelTimeout;
    private String encryptionMethod;
    private String failoverGranularity;
    private String failoverMode;
    private String failoverPreconnect;
    private String hostNameInCertificate;
    private String initializationString;
    private String insensitiveResultSetBufferSize;
    private String javaDoubleToString;
    private String loadBalancing;
    private String longDataCacheSize;
    private String netAddress;
    private String packetSize;
    private String queryTimeout;
    private String resultsetMetaDataOptions;
    private String selectMethod;
    private String snapshotSerializable;
    private String spyAttributes;
    private String stringInputParameterType;
    private String stringOutputParameterType;
    private String suppressConnectionWarnings;
    private String transactionMode;
    private String truncateFractionalSeconds;
    private String trustStore;
    private String trustStorePassword;
    private String useServerSideUpdatableCursors;
    private String validateServerCertificate;

    @Override
    public String getElementName() {
        return DATADIRECT_SQLSERVER;
    }

    @XmlAttribute(name = "JDBCBehavior")
    public void setJDBCBehavior(String JDBCBehavior) {
        this.JDBCBehavior = JDBCBehavior;
    }

    public String getJDBCBehavior() {
        return this.JDBCBehavior;
    }

    @XmlAttribute(name = "XATransactionGroup")
    public void setXATransactionGroup(String XATransactionGroup) {
        this.XATransactionGroup = XATransactionGroup;
    }

    public String getXATransactionGroup() {
        return this.XATransactionGroup;
    }

    @XmlAttribute(name = "XMLDescribeType")
    public void setXMLDescribeType(String XMLDescribeType) {
        this.XMLDescribeType = XMLDescribeType;
    }

    public String getXMLDescribeType() {
        return this.XMLDescribeType;
    }

    @XmlAttribute(name = "accountingInfo")
    public void setAccountingInfo(String accountingInfo) {
        this.accountingInfo = accountingInfo;
    }

    public String getAccountingInfo() {
        return this.accountingInfo;
    }

    @XmlAttribute(name = "alternateServers")
    public void setAlternateServers(String alternateServers) {
        this.alternateServers = alternateServers;
    }

    public String getAlternateServers() {
        return this.alternateServers;
    }

    @XmlAttribute(name = "alwaysReportTriggerResults")
    public void setAlwaysReportTriggerResults(String alwaysReportTriggerResults) {
        this.alwaysReportTriggerResults = alwaysReportTriggerResults;
    }

    public String getAlwaysReportTriggerResults() {
        return this.alwaysReportTriggerResults;
    }

    @XmlAttribute(name = "applicationName")
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationName() {
        return this.applicationName;
    }

    @XmlAttribute(name = "authenticationMethod")
    public void setAuthenticationMethod(String authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    public String getAuthenticationMethod() {
        return this.authenticationMethod;
    }

    @XmlAttribute(name = "bulkLoadBatchSize")
    public void setBulkLoadBatchSize(String bulkLoadBatchSize) {
        this.bulkLoadBatchSize = bulkLoadBatchSize;
    }

    public String getBulkLoadBatchSize() {
        return this.bulkLoadBatchSize;
    }

    @XmlAttribute(name = "bulkLoadOptions")
    public void setBulkLoadOptions(String bulkLoadOptions) {
        this.bulkLoadOptions = bulkLoadOptions;
    }

    public String getBulkLoadOptions() {
        return this.bulkLoadOptions;
    }

    @XmlAttribute(name = "clientHostName")
    public void setClientHostName(String clientHostName) {
        this.clientHostName = clientHostName;
    }

    public String getClientHostName() {
        return this.clientHostName;
    }

    @XmlAttribute(name = "clientUser")
    public void setClientUser(String clientUser) {
        this.clientUser = clientUser;
    }

    public String getClientUser() {
        return this.clientUser;
    }

    @XmlAttribute(name = "codePageOverride")
    public void setCodePageOverride(String codePageOverride) {
        this.codePageOverride = codePageOverride;
    }

    public String getCodePageOverride() {
        return this.codePageOverride;
    }

    @XmlAttribute(name = "connectionRetryCount")
    public void setConnectionRetryCount(String connectionRetryCount) {
        this.connectionRetryCount = connectionRetryCount;
    }

    public String getConnectionRetryCount() {
        return this.connectionRetryCount;
    }

    @XmlAttribute(name = "connectionRetryDelay")
    public void setConnectionRetryDelay(String connectionRetryDelay) {
        this.connectionRetryDelay = connectionRetryDelay;
    }

    public String getConnectionRetryDelay() {
        return this.connectionRetryDelay;
    }

    @XmlAttribute(name = "convertNull")
    public void setConvertNull(String convertNull) {
        this.convertNull = convertNull;
    }

    public String getConvertNull() {
        return this.convertNull;
    }

    @XmlAttribute(name = "dateTimeInputParameterType")
    public void setDateTimeInputParameterType(String dateTimeInputParameterType) {
        this.dateTimeInputParameterType = dateTimeInputParameterType;
    }

    public String getDateTimeInputParameterType() {
        return this.dateTimeInputParameterType;
    }

    @XmlAttribute(name = "dateTimeOutputParameterType")
    public void setDateTimeOutputParameterType(String dateTimeOutputParameterType) {
        this.dateTimeOutputParameterType = dateTimeOutputParameterType;
    }

    public String getDateTimeOutputParameterType() {
        return this.dateTimeOutputParameterType;
    }

    @XmlAttribute(name = "describeInputParameters")
    public void setDescribeInputParameters(String describeInputParameters) {
        this.describeInputParameters = describeInputParameters;
    }

    public String getDescribeInputParameters() {
        return this.describeInputParameters;
    }

    @XmlAttribute(name = "describeOutputParameters")
    public void setDescribeOutputParameters(String describeOutputParameters) {
        this.describeOutputParameters = describeOutputParameters;
    }

    public String getDescribeOutputParameters() {
        return this.describeOutputParameters;
    }

    @XmlAttribute(name = "enableBulkLoad")
    public void setEnableBulkLoad(String enableBulkLoad) {
        this.enableBulkLoad = enableBulkLoad;
    }

    public String getEnableBulkLoad() {
        return this.enableBulkLoad;
    }

    @XmlAttribute(name = "enableCancelTimeout")
    public void setEnableCancelTimeout(String enableCancelTimeout) {
        this.enableCancelTimeout = enableCancelTimeout;
    }

    public String getEnableCancelTimeout() {
        return this.enableCancelTimeout;
    }

    @XmlAttribute(name = "encryptionMethod")
    public void setEncryptionMethod(String encryptionMethod) {
        this.encryptionMethod = encryptionMethod;
    }

    public String getEncryptionMethod() {
        return this.encryptionMethod;
    }

    @XmlAttribute(name = "failoverGranularity")
    public void setFailoverGranularity(String failoverGranularity) {
        this.failoverGranularity = failoverGranularity;
    }

    public String getFailoverGranularity() {
        return this.failoverGranularity;
    }

    @XmlAttribute(name = "failoverMode")
    public void setFailoverMode(String failoverMode) {
        this.failoverMode = failoverMode;
    }

    public String getFailoverMode() {
        return this.failoverMode;
    }

    @XmlAttribute(name = "failoverPreconnect")
    public void setFailoverPreconnect(String failoverPreconnect) {
        this.failoverPreconnect = failoverPreconnect;
    }

    public String getFailoverPreconnect() {
        return this.failoverPreconnect;
    }

    @XmlAttribute(name = "hostNameInCertificate")
    public void setHostNameInCertificate(String hostNameInCertificate) {
        this.hostNameInCertificate = hostNameInCertificate;
    }

    public String getHostNameInCertificate() {
        return this.hostNameInCertificate;
    }

    @XmlAttribute(name = "initializationString")
    public void setInitializationString(String initializationString) {
        this.initializationString = initializationString;
    }

    public String getInitializationString() {
        return this.initializationString;
    }

    @XmlAttribute(name = "insensitiveResultSetBufferSize")
    public void setInsensitiveResultSetBufferSize(String insensitiveResultSetBufferSize) {
        this.insensitiveResultSetBufferSize = insensitiveResultSetBufferSize;
    }

    public String getInsensitiveResultSetBufferSize() {
        return this.insensitiveResultSetBufferSize;
    }

    @XmlAttribute(name = "javaDoubleToString")
    public void setJavaDoubleToString(String javaDoubleToString) {
        this.javaDoubleToString = javaDoubleToString;
    }

    public String getJavaDoubleToString() {
        return this.javaDoubleToString;
    }

    @XmlAttribute(name = "loadBalancing")
    public void setLoadBalancing(String loadBalancing) {
        this.loadBalancing = loadBalancing;
    }

    public String getLoadBalancing() {
        return this.loadBalancing;
    }

    @XmlAttribute(name = "longDataCacheSize")
    public void setStringDataCacheSize(String longDataCacheSize) {
        this.longDataCacheSize = longDataCacheSize;
    }

    public String getStringDataCacheSize() {
        return this.longDataCacheSize;
    }

    @XmlAttribute(name = "netAddress")
    public void setNetAddress(String netAddress) {
        this.netAddress = netAddress;
    }

    public String getNetAddress() {
        return this.netAddress;
    }

    @XmlAttribute(name = "packetSize")
    public void setPacketSize(String packetSize) {
        this.packetSize = packetSize;
    }

    public String getPacketSize() {
        return this.packetSize;
    }

    @XmlAttribute(name = "queryTimeout")
    public void setQueryTimeout(String queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    public String getQueryTimeout() {
        return this.queryTimeout;
    }

    @XmlAttribute(name = "resultsetMetaDataOptions")
    public void setResultsetMetaDataOptions(String resultsetMetaDataOptions) {
        this.resultsetMetaDataOptions = resultsetMetaDataOptions;
    }

    public String getResultsetMetaDataOptions() {
        return this.resultsetMetaDataOptions;
    }

    @XmlAttribute(name = "selectMethod")
    public void setSelectMethod(String selectMethod) {
        this.selectMethod = selectMethod;
    }

    public String getSelectMethod() {
        return this.selectMethod;
    }

    @XmlAttribute(name = "snapshotSerializable")
    public void setSnapshotSerializable(String snapshotSerializable) {
        this.snapshotSerializable = snapshotSerializable;
    }

    public String getSnapshotSerializable() {
        return this.snapshotSerializable;
    }

    @XmlAttribute(name = "spyAttributes")
    public void setSpyAttributes(String spyAttributes) {
        this.spyAttributes = spyAttributes;
    }

    public String getSpyAttributes() {
        return this.spyAttributes;
    }

    @XmlAttribute(name = "stringInputParameterType")
    public void setStringInputParameterType(String stringInputParameterType) {
        this.stringInputParameterType = stringInputParameterType;
    }

    public String getStringInputParameterType() {
        return this.stringInputParameterType;
    }

    @XmlAttribute(name = "stringOutputParameterType")
    public void setStringOutputParameterType(String stringOutputParameterType) {
        this.stringOutputParameterType = stringOutputParameterType;
    }

    public String getStringOutputParameterType() {
        return this.stringOutputParameterType;
    }

    @XmlAttribute(name = "suppressConnectionWarnings")
    public void setSuppressConnectionWarnings(String suppressConnectionWarnings) {
        this.suppressConnectionWarnings = suppressConnectionWarnings;
    }

    public String getSuppressConnectionWarnings() {
        return this.suppressConnectionWarnings;
    }

    @XmlAttribute(name = "transactionMode")
    public void setTransactionMode(String transactionMode) {
        this.transactionMode = transactionMode;
    }

    public String getTransactionMode() {
        return this.transactionMode;
    }

    @XmlAttribute(name = "truncateFractionalSeconds")
    public void setTruncateFractionalSeconds(String truncateFractionalSeconds) {
        this.truncateFractionalSeconds = truncateFractionalSeconds;
    }

    public String getTruncateFractionalSeconds() {
        return this.truncateFractionalSeconds;
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

    @XmlAttribute(name = "useServerSideUpdatableCursors")
    public void setUseServerSideUpdatableCursors(String useServerSideUpdatableCursors) {
        this.useServerSideUpdatableCursors = useServerSideUpdatableCursors;
    }

    public String getUseServerSideUpdatableCursors() {
        return this.useServerSideUpdatableCursors;
    }

    @XmlAttribute(name = "validateServerCertificate")
    public void setValidateServerCertificate(String validateServerCertificate) {
        this.validateServerCertificate = validateServerCertificate;
    }

    public String getValidateServerCertificate() {
        return this.validateServerCertificate;
    }

    /**
     * Returns a String listing the properties and their values used on this
     * data source.
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("{");
        if (JDBCBehavior != null)
            buf.append("JDBCBehavior=\"" + JDBCBehavior + "\" ");
        if (XATransactionGroup != null)
            buf.append("XATransactionGroup=\"" + XATransactionGroup + "\" ");
        if (XMLDescribeType != null)
            buf.append("XMLDescribeType=\"" + XMLDescribeType + "\" ");
        if (accountingInfo != null)
            buf.append("accountingInfo=\"" + accountingInfo + "\" ");
        if (alternateServers != null)
            buf.append("alternateServers=\"" + alternateServers + "\" ");
        if (alwaysReportTriggerResults != null)
            buf.append("alwaysReportTriggerResults=\"" + alwaysReportTriggerResults + "\" ");
        if (applicationName != null)
            buf.append("applicationName=\"" + applicationName + "\" ");
        if (authenticationMethod != null)
            buf.append("authenticationMethod=\"" + authenticationMethod + "\" ");
        if (bulkLoadBatchSize != null)
            buf.append("bulkLoadBatchSize=\"" + bulkLoadBatchSize + "\" ");
        if (bulkLoadOptions != null)
            buf.append("bulkLoadOptions=\"" + bulkLoadOptions + "\" ");
        if (clientHostName != null)
            buf.append("clientHostName=\"" + clientHostName + "\" ");
        if (clientUser != null)
            buf.append("clientUser=\"" + clientUser + "\" ");
        if (codePageOverride != null)
            buf.append("codePageOverride=\"" + codePageOverride + "\" ");
        if (connectionRetryCount != null)
            buf.append("connectionRetryCount=\"" + connectionRetryCount + "\" ");
        if (connectionRetryDelay != null)
            buf.append("connectionRetryDelay=\"" + connectionRetryDelay + "\" ");
        if (convertNull != null)
            buf.append("convertNull=\"" + convertNull + "\" ");
        if (super.getDatabaseName() != null)
            buf.append("databaseName=\"" + super.getDatabaseName() + "\" ");
        if (dateTimeInputParameterType != null)
            buf.append("dateTimeInputParameterType=\"" + dateTimeInputParameterType + "\" ");
        if (dateTimeOutputParameterType != null)
            buf.append("dateTimeOutputParameterType=\"" + dateTimeOutputParameterType + "\" ");
        if (describeInputParameters != null)
            buf.append("describeInputParameters=\"" + describeInputParameters + "\" ");
        if (describeOutputParameters != null)
            buf.append("describeOutputParameters=\"" + describeOutputParameters + "\" ");
        if (enableBulkLoad != null)
            buf.append("enableBulkLoad=\"" + enableBulkLoad + "\" ");
        if (enableCancelTimeout != null)
            buf.append("enableCancelTimeout=\"" + enableCancelTimeout + "\" ");
        if (encryptionMethod != null)
            buf.append("encryptionMethod=\"" + encryptionMethod + "\" ");
        if (failoverGranularity != null)
            buf.append("failoverGranularity=\"" + failoverGranularity + "\" ");
        if (failoverMode != null)
            buf.append("failoverMode=\"" + failoverMode + "\" ");
        if (failoverPreconnect != null)
            buf.append("failoverPreconnect=\"" + failoverPreconnect + "\" ");
        if (hostNameInCertificate != null)
            buf.append("hostNameInCertificate=\"" + hostNameInCertificate + "\" ");
        if (initializationString != null)
            buf.append("initializationString=\"" + initializationString + "\" ");
        if (insensitiveResultSetBufferSize != null)
            buf.append("insensitiveResultSetBufferSize=\"" + insensitiveResultSetBufferSize + "\" ");
        if (javaDoubleToString != null)
            buf.append("javaDoubleToString=\"" + javaDoubleToString + "\" ");
        if (loadBalancing != null)
            buf.append("loadBalancing=\"" + loadBalancing + "\" ");
        if (super.getLoginTimeout() != null)
            buf.append("loginTimeout=\"" + super.getLoginTimeout() + "\" ");
        if (longDataCacheSize != null)
            buf.append("longDataCacheSize=\"" + longDataCacheSize + "\" ");
        if (netAddress != null)
            buf.append("netAddress=\"" + netAddress + "\" ");
        if (packetSize != null)
            buf.append("packetSize=\"" + packetSize + "\" ");
        if (super.getPassword() != null)
            buf.append("password=\"" + super.getPassword() + "\" ");
        if (super.getPortNumber() != null)
            buf.append("portNumber=\"" + super.getPortNumber() + "\" ");
        if (queryTimeout != null)
            buf.append("queryTimeout=\"" + queryTimeout + "\" ");
        if (resultsetMetaDataOptions != null)
            buf.append("resultsetMetaDataOptions=\"" + resultsetMetaDataOptions + "\" ");
        if (selectMethod != null)
            buf.append("selectMethod=\"" + selectMethod + "\" ");
        if (super.getServerName() != null)
            buf.append("serverName=\"" + super.getServerName() + "\" ");
        if (snapshotSerializable != null)
            buf.append("snapshotSerializable=\"" + snapshotSerializable + "\" ");
        if (spyAttributes != null)
            buf.append("spyAttributes=\"" + spyAttributes + "\" ");
        if (stringInputParameterType != null)
            buf.append("stringInputParameterType=\"" + stringInputParameterType + "\" ");
        if (stringOutputParameterType != null)
            buf.append("stringOutputParameterType=\"" + stringOutputParameterType + "\" ");
        if (suppressConnectionWarnings != null)
            buf.append("suppressConnectionWarnings=\"" + suppressConnectionWarnings + "\" ");
        if (transactionMode != null)
            buf.append("transactionMode=\"" + transactionMode + "\" ");
        if (truncateFractionalSeconds != null)
            buf.append("truncateFractionalSeconds=\"" + truncateFractionalSeconds + "\" ");
        if (trustStore != null)
            buf.append("trustStore=\"" + trustStore + "\" ");
        if (trustStorePassword != null)
            buf.append("trustStorePassword=\"" + trustStorePassword + "\" ");
        if (useServerSideUpdatableCursors != null)
            buf.append("useServerSideUpdatableCursors=\"" + useServerSideUpdatableCursors + "\" ");
        if (super.getUser() != null)
            buf.append("user=\"" + super.getUser() + "\" ");
        if (validateServerCertificate != null)
            buf.append("validateServerCertificate=\"" + validateServerCertificate + "\" ");
        buf.append("}");
        return buf.toString();
    }

}