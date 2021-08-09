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
public class Properties_db2_jcc extends DataSourceProperties {
    private String blockingReadConnectionTimeout;
    private String clientAccountingInformation;
    private String clientApplicationInformation;
    private String clientRerouteAlternatePortNumber;
    private String clientRerouteAlternateServerName;
    private String clientRerouteServerListJNDIName;
    private String clientUser;
    private String clientWorkstation;
    private String currentFunctionPath;
    private String currentLockTimeout;
    private String currentPackagePath;
    private String currentPackageSet;
    private String currentSQLID;
    private String currentSchema;
    private String cursorSensitivity;
    private String deferPrepares;
    private String driverType;
    private String enableClientAffinitiesList;
    private String enableNamedParameterMarkers;
    private String enableSeamlessFailover;
    private String enableSysplexWLB;
    private String fetchSize;
    private String fullyMaterializeInputStreams;
    private String fullyMaterializeLobData;
    private String keepAliveTimeout;
    private String keepDynamic;
    private String kerberosServerPrincipal;
    private String maxRetriesForClientReroute;
    private String queryCloseImplicit;
    private String queryDataSize;
    private String readOnly;
    private String resultSetHoldability;
    private String resultSetHoldabilityForCatalogQueries;
    private String retrieveMessagesFromServerOnGetMessage;
    private String retryIntervalForClientReroute;
    private String securityMechanism;
    private String sendDataAsIs;
    private String sqljEnableClassLoaderSpecificProfiles;
    private String sslConnection;
    private String streamBufferSize;
    private String sysSchema;
    private String traceDirectory;
    private String traceFile;
    private String traceFileAppend;
    private String traceLevel;
    private String useCachedCursor;
    private String useJDBC4ColumnNameAndLabelSemantics;
    private String useTransactionRedirect;
    private String xaNetworkOptimization;

    @Override
    public String getElementName() {
        return DB2_JCC;
    }

    @XmlAttribute(name = "blockingReadConnectionTimeout")
    public void setBlockingReadConnectionTimeout(String blockingReadConnectionTimeout) {
        this.blockingReadConnectionTimeout = blockingReadConnectionTimeout;
    }

    public String getBlockingReadConnectionTimeout() {
        return this.blockingReadConnectionTimeout;
    }

    @XmlAttribute(name = "clientAccountingInformation")
    public void setClientAccountingInformation(String clientAccountingInformation) {
        this.clientAccountingInformation = clientAccountingInformation;
    }

    public String getClientAccountingInformation() {
        return this.clientAccountingInformation;
    }

    @XmlAttribute(name = "clientApplicationInformation")
    public void setClientApplicationInformation(String clientApplicationInformation) {
        this.clientApplicationInformation = clientApplicationInformation;
    }

    public String getClientApplicationInformation() {
        return this.clientApplicationInformation;
    }

    @XmlAttribute(name = "clientRerouteAlternatePortNumber")
    public void setClientRerouteAlternatePortNumber(String clientRerouteAlternatePortNumber) {
        this.clientRerouteAlternatePortNumber = clientRerouteAlternatePortNumber;
    }

    public String getClientRerouteAlternatePortNumber() {
        return this.clientRerouteAlternatePortNumber;
    }

    @XmlAttribute(name = "clientRerouteAlternateServerName")
    public void setClientRerouteAlternateServerName(String clientRerouteAlternateServerName) {
        this.clientRerouteAlternateServerName = clientRerouteAlternateServerName;
    }

    public String getClientRerouteAlternateServerName() {
        return this.clientRerouteAlternateServerName;
    }

    @XmlAttribute(name = "clientRerouteServerListJNDIName")
    public void setClientRerouteServerListJNDIName(String clientRerouteServerListJNDIName) {
        this.clientRerouteServerListJNDIName = clientRerouteServerListJNDIName;
    }

    public String getClientRerouteServerListJNDIName() {
        return this.clientRerouteServerListJNDIName;
    }

    @XmlAttribute(name = "clientUser")
    public void setClientUser(String clientUser) {
        this.clientUser = clientUser;
    }

    public String getClientUser() {
        return this.clientUser;
    }

    @XmlAttribute(name = "clientWorkstation")
    public void setClientWorkstation(String clientWorkstation) {
        this.clientWorkstation = clientWorkstation;
    }

    public String getClientWorkstation() {
        return this.clientWorkstation;
    }

    @XmlAttribute(name = "currentFunctionPath")
    public void setCurrentFunctionPath(String currentFunctionPath) {
        this.currentFunctionPath = currentFunctionPath;
    }

    public String getCurrentFunctionPath() {
        return this.currentFunctionPath;
    }

    @XmlAttribute(name = "currentLockTimeout")
    public void setCurrentLockTimeout(String currentLockTimeout) {
        this.currentLockTimeout = currentLockTimeout;
    }

    public String getCurrentLockTimeout() {
        return this.currentLockTimeout;
    }

    @XmlAttribute(name = "currentPackagePath")
    public void setCurrentPackagePath(String currentPackagePath) {
        this.currentPackagePath = currentPackagePath;
    }

    public String getCurrentPackagePath() {
        return this.currentPackagePath;
    }

    @XmlAttribute(name = "currentPackageSet")
    public void setCurrentPackageSet(String currentPackageSet) {
        this.currentPackageSet = currentPackageSet;
    }

    public String getCurrentPackageSet() {
        return this.currentPackageSet;
    }

    @XmlAttribute(name = "currentSQLID")
    public void setCurrentSQLID(String currentSQLID) {
        this.currentSQLID = currentSQLID;
    }

    public String getCurrentSQLID() {
        return this.currentSQLID;
    }

    @XmlAttribute(name = "currentSchema")
    public void setCurrentSchema(String currentSchema) {
        this.currentSchema = currentSchema;
    }

    public String getCurrentSchema() {
        return this.currentSchema;
    }

    @XmlAttribute(name = "cursorSensitivity")
    public void setCursorSensitivity(String cursorSensitivity) {
        this.cursorSensitivity = cursorSensitivity;
    }

    public String getCursorSensitivity() {
        return this.cursorSensitivity;
    }

    @XmlAttribute(name = "deferPrepares")
    public void setDeferPrepares(String deferPrepares) {
        this.deferPrepares = deferPrepares;
    }

    public String getDeferPrepares() {
        return this.deferPrepares;
    }

    @XmlAttribute(name = "driverType")
    public void setDriverType(String driverType) {
        this.driverType = driverType;
    }

    public String getDriverType() {
        return this.driverType;
    }

    @XmlAttribute(name = "enableClientAffinitiesList")
    public void setEnableClientAffinitiesList(String enableClientAffinitiesList) {
        this.enableClientAffinitiesList = enableClientAffinitiesList;
    }

    public String getEnableClientAffinitiesList() {
        return this.enableClientAffinitiesList;
    }

    @XmlAttribute(name = "enableNamedParameterMarkers")
    public void setEnableNamedParameterMarkers(String enableNamedParameterMarkers) {
        this.enableNamedParameterMarkers = enableNamedParameterMarkers;
    }

    public String getEnableNamedParameterMarkers() {
        return this.enableNamedParameterMarkers;
    }

    @XmlAttribute(name = "enableSeamlessFailover")
    public void setEnableSeamlessFailover(String enableSeamlessFailover) {
        this.enableSeamlessFailover = enableSeamlessFailover;
    }

    public String getEnableSeamlessFailover() {
        return this.enableSeamlessFailover;
    }

    @XmlAttribute(name = "enableSysplexWLB")
    public void setEnableSysplexWLB(String enableSysplexWLB) {
        this.enableSysplexWLB = enableSysplexWLB;
    }

    public String getEnableSysplexWLB() {
        return this.enableSysplexWLB;
    }

    @XmlAttribute(name = "fetchSize")
    public void setFetchSize(String fetchSize) {
        this.fetchSize = fetchSize;
    }

    public String getFetchSize() {
        return this.fetchSize;
    }

    @XmlAttribute(name = "fullyMaterializeInputStreams")
    public void setFullyMaterializeInputStreams(String fullyMaterializeInputStreams) {
        this.fullyMaterializeInputStreams = fullyMaterializeInputStreams;
    }

    public String getFullyMaterializeInputStreams() {
        return this.fullyMaterializeInputStreams;
    }

    @XmlAttribute(name = "fullyMaterializeLobData")
    public void setFullyMaterializeLobData(String fullyMaterializeLobData) {
        this.fullyMaterializeLobData = fullyMaterializeLobData;
    }

    public String getFullyMaterializeLobData() {
        return this.fullyMaterializeLobData;
    }

    @XmlAttribute(name = "keepAliveTimeout")
    public void setKeepAliveTimeout(String keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }

    public String getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    @XmlAttribute(name = "keepDynamic")
    public void setKeepDynamic(String keepDynamic) {
        this.keepDynamic = keepDynamic;
    }

    public String getKeepDynamic() {
        return this.keepDynamic;
    }

    @XmlAttribute(name = "kerberosServerPrincipal")
    public void setKerberosServerPrincipal(String kerberosServerPrincipal) {
        this.kerberosServerPrincipal = kerberosServerPrincipal;
    }

    public String getKerberosServerPrincipal() {
        return this.kerberosServerPrincipal;
    }

    @XmlAttribute(name = "maxRetriesForClientReroute")
    public void setMaxRetriesForClientReroute(String maxRetriesForClientReroute) {
        this.maxRetriesForClientReroute = maxRetriesForClientReroute;
    }

    public String getMaxRetriesForClientReroute() {
        return this.maxRetriesForClientReroute;
    }

    @XmlAttribute(name = "queryCloseImplicit")
    public void setQueryCloseImplicit(String queryCloseImplicit) {
        this.queryCloseImplicit = queryCloseImplicit;
    }

    public String getQueryCloseImplicit() {
        return this.queryCloseImplicit;
    }

    @XmlAttribute(name = "queryDataSize")
    public void setQueryDataSize(String queryDataSize) {
        this.queryDataSize = queryDataSize;
    }

    public String getQueryDataSize() {
        return this.queryDataSize;
    }

    @XmlAttribute(name = "readOnly")
    public void setReadOnly(String readOnly) {
        this.readOnly = readOnly;
    }

    public String getReadOnly() {
        return this.readOnly;
    }

    @XmlAttribute(name = "resultSetHoldability")
    public void setResultSetHoldability(String resultSetHoldability) {
        this.resultSetHoldability = resultSetHoldability;
    }

    public String getResultSetHoldability() {
        return this.resultSetHoldability;
    }

    @XmlAttribute(name = "resultSetHoldabilityForCatalogQueries")
    public void setResultSetHoldabilityForCatalogQueries(String resultSetHoldabilityForCatalogQueries) {
        this.resultSetHoldabilityForCatalogQueries = resultSetHoldabilityForCatalogQueries;
    }

    public String getResultSetHoldabilityForCatalogQueries() {
        return this.resultSetHoldabilityForCatalogQueries;
    }

    @XmlAttribute(name = "retrieveMessagesFromServerOnGetMessage")
    public void setRetrieveMessagesFromServerOnGetMessage(String retrieveMessagesFromServerOnGetMessage) {
        this.retrieveMessagesFromServerOnGetMessage = retrieveMessagesFromServerOnGetMessage;
    }

    public String getRetrieveMessagesFromServerOnGetMessage() {
        return this.retrieveMessagesFromServerOnGetMessage;
    }

    @XmlAttribute(name = "retryIntervalForClientReroute")
    public void setRetryIntervalForClientReroute(String retryIntervalForClientReroute) {
        this.retryIntervalForClientReroute = retryIntervalForClientReroute;
    }

    public String getRetryIntervalForClientReroute() {
        return this.retryIntervalForClientReroute;
    }

    @XmlAttribute(name = "securityMechanism")
    public void setSecurityMechanism(String securityMechanism) {
        this.securityMechanism = securityMechanism;
    }

    public String getSecurityMechanism() {
        return this.securityMechanism;
    }

    @XmlAttribute(name = "sendDataAsIs")
    public void setSendDataAsIs(String sendDataAsIs) {
        this.sendDataAsIs = sendDataAsIs;
    }

    public String getSendDataAsIs() {
        return this.sendDataAsIs;
    }

    @XmlAttribute(name = "sqljEnableClassLoaderSpecificProfiles")
    public void setSqljEnableClassLoaderSpecificProfiles(String sqljEnableClassLoaderSpecificProfiles) {
        this.sqljEnableClassLoaderSpecificProfiles = sqljEnableClassLoaderSpecificProfiles;
    }

    public String getSqljEnableClassLoaderSpecificProfiles() {
        return this.sqljEnableClassLoaderSpecificProfiles;
    }

    @XmlAttribute(name = "sslConnection")
    public void setSslConnection(String sslConnection) {
        this.sslConnection = sslConnection;
    }

    public String getSslConnection() {
        return this.sslConnection;
    }

    @XmlAttribute(name = "streamBufferSize")
    public void setStreamBufferSize(String streamBufferSize) {
        this.streamBufferSize = streamBufferSize;
    }

    public String getStreamBufferSize() {
        return this.streamBufferSize;
    }

    @XmlAttribute(name = "sysSchema")
    public void setSysSchema(String sysSchema) {
        this.sysSchema = sysSchema;
    }

    public String getSysSchema() {
        return this.sysSchema;
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

    @XmlAttribute(name = "useCachedCursor")
    public void setUseCachedCursor(String useCachedCursor) {
        this.useCachedCursor = useCachedCursor;
    }

    public String getUseCachedCursor() {
        return this.useCachedCursor;
    }

    @XmlAttribute(name = "useJDBC4ColumnNameAndLabelSemantics")
    public void setUseJDBC4ColumnNameAndLabelSemantics(String useJDBC4ColumnNameAndLabelSemantics) {
        this.useJDBC4ColumnNameAndLabelSemantics = useJDBC4ColumnNameAndLabelSemantics;
    }

    public String getUseJDBC4ColumnNameAndLabelSemantics() {
        return this.useJDBC4ColumnNameAndLabelSemantics;
    }

    @XmlAttribute(name = "useTransactionRedirect")
    public void setUseTransactionRedirect(String useTransactionRedirect) {
        this.useTransactionRedirect = useTransactionRedirect;
    }

    public String getUseTransactionRedirect() {
        return this.useTransactionRedirect;
    }

    @XmlAttribute(name = "xaNetworkOptimization")
    public void setXaNetworkOptimization(String xaNetworkOptimization) {
        this.xaNetworkOptimization = xaNetworkOptimization;
    }

    public String getXaNetworkOptimization() {
        return this.xaNetworkOptimization;
    }

    /**
     * Returns a String listing the properties and their values used on this
     * data source.
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("{");
        if (blockingReadConnectionTimeout != null)
            buf.append("blockingReadConnectionTimeout=\"" + blockingReadConnectionTimeout + "\" ");
        if (clientAccountingInformation != null)
            buf.append("clientAccountingInformation=\"" + clientAccountingInformation + "\" ");
        if (clientApplicationInformation != null)
            buf.append("clientApplicationInformation=\"" + clientApplicationInformation + "\" ");
        if (clientRerouteAlternatePortNumber != null)
            buf.append("clientRerouteAlternatePortNumber=\"" + clientRerouteAlternatePortNumber + "\" ");
        if (clientRerouteAlternateServerName != null)
            buf.append("clientRerouteAlternateServerName=\"" + clientRerouteAlternateServerName + "\" ");
        if (clientRerouteServerListJNDIName != null)
            buf.append("clientRerouteServerListJNDIName=\"" + clientRerouteServerListJNDIName + "\" ");
        if (clientUser != null)
            buf.append("clientUser=\"" + clientUser + "\" ");
        if (clientWorkstation != null)
            buf.append("clientWorkstation=\"" + clientWorkstation + "\" ");
        if (currentFunctionPath != null)
            buf.append("currentFunctionPath=\"" + currentFunctionPath + "\" ");
        if (currentLockTimeout != null)
            buf.append("currentLockTimeout=\"" + currentLockTimeout + "\" ");
        if (currentPackagePath != null)
            buf.append("currentPackagePath=\"" + currentPackagePath + "\" ");
        if (currentPackageSet != null)
            buf.append("currentPackageSet=\"" + currentPackageSet + "\" ");
        if (currentSQLID != null)
            buf.append("currentSQLID=\"" + currentSQLID + "\" ");
        if (currentSchema != null)
            buf.append("currentSchema=\"" + currentSchema + "\" ");
        if (cursorSensitivity != null)
            buf.append("cursorSensitivity=\"" + cursorSensitivity + "\" ");
        if (super.getDatabaseName() != null)
            buf.append("databaseName=\"" + super.getDatabaseName() + "\" ");
        if (deferPrepares != null)
            buf.append("deferPrepares=\"" + deferPrepares + "\" ");
        if (driverType != null)
            buf.append("driverType=\"" + driverType + "\" ");
        if (enableClientAffinitiesList != null)
            buf.append("enableClientAffinitiesList=\"" + enableClientAffinitiesList + "\" ");
        if (enableNamedParameterMarkers != null)
            buf.append("enableNamedParameterMarkers=\"" + enableNamedParameterMarkers + "\" ");
        if (enableSeamlessFailover != null)
            buf.append("enableSeamlessFailover=\"" + enableSeamlessFailover + "\" ");
        if (enableSysplexWLB != null)
            buf.append("enableSysplexWLB=\"" + enableSysplexWLB + "\" ");
        if (fetchSize != null)
            buf.append("fetchSize=\"" + fetchSize + "\" ");
        if (fullyMaterializeInputStreams != null)
            buf.append("fullyMaterializeInputStreams=\"" + fullyMaterializeInputStreams + "\" ");
        if (fullyMaterializeLobData != null)
            buf.append("fullyMaterializeLobData=\"" + fullyMaterializeLobData + "\" ");
        if (keepAliveTimeout != null)
            buf.append("keepAliveTimeout=\"" + keepAliveTimeout + "\" ");
        if (keepDynamic != null)
            buf.append("keepDynamic=\"" + keepDynamic + "\" ");
        if (kerberosServerPrincipal != null)
            buf.append("kerberosServerPrincipal=\"" + kerberosServerPrincipal + "\" ");
        if (super.getLoginTimeout() != null)
            buf.append("loginTimeout=\"" + super.getLoginTimeout() + "\" ");
        if (maxRetriesForClientReroute != null)
            buf.append("maxRetriesForClientReroute=\"" + maxRetriesForClientReroute + "\" ");
        if (super.getPassword() != null)
            buf.append("password=\"" + super.getPassword() + "\" ");
        if (super.getPortNumber() != null)
            buf.append("portNumber=\"" + super.getPortNumber() + "\" ");
        if (queryCloseImplicit != null)
            buf.append("queryCloseImplicit=\"" + queryCloseImplicit + "\" ");
        if (queryDataSize != null)
            buf.append("queryDataSize=\"" + queryDataSize + "\" ");
        if (readOnly != null)
            buf.append("readOnly=\"" + readOnly + "\" ");
        if (resultSetHoldability != null)
            buf.append("resultSetHoldability=\"" + resultSetHoldability + "\" ");
        if (resultSetHoldabilityForCatalogQueries != null)
            buf.append("resultSetHoldabilityForCatalogQueries=\"" + resultSetHoldabilityForCatalogQueries + "\" ");
        if (retrieveMessagesFromServerOnGetMessage != null)
            buf.append("retrieveMessagesFromServerOnGetMessage=\"" + retrieveMessagesFromServerOnGetMessage + "\" ");
        if (retryIntervalForClientReroute != null)
            buf.append("retryIntervalForClientReroute=\"" + retryIntervalForClientReroute + "\" ");
        if (securityMechanism != null)
            buf.append("securityMechanism=\"" + securityMechanism + "\" ");
        if (sendDataAsIs != null)
            buf.append("sendDataAsIs=\"" + sendDataAsIs + "\" ");
        if (super.getServerName() != null)
            buf.append("serverName=\"" + super.getServerName() + "\" ");
        if (sqljEnableClassLoaderSpecificProfiles != null)
            buf.append("sqljEnableClassLoaderSpecificProfiles=\"" + sqljEnableClassLoaderSpecificProfiles + "\" ");
        if (sslConnection != null)
            buf.append("sslConnection=\"" + sslConnection + "\" ");
        if (streamBufferSize != null)
            buf.append("streamBufferSize=\"" + streamBufferSize + "\" ");
        if (sysSchema != null)
            buf.append("sysSchema=\"" + sysSchema + "\" ");
        if (traceDirectory != null)
            buf.append("traceDirectory=\"" + traceDirectory + "\" ");
        if (traceFile != null)
            buf.append("traceFile=\"" + traceFile + "\" ");
        if (traceFileAppend != null)
            buf.append("traceFileAppend=\"" + traceFileAppend + "\" ");
        if (traceLevel != null)
            buf.append("traceLevel=\"" + traceLevel + "\" ");
        if (useCachedCursor != null)
            buf.append("useCachedCursor=\"" + useCachedCursor + "\" ");
        if (useJDBC4ColumnNameAndLabelSemantics != null)
            buf.append("useJDBC4ColumnNameAndLabelSemantics=\"" + useJDBC4ColumnNameAndLabelSemantics + "\" ");
        if (useTransactionRedirect != null)
            buf.append("useTransactionRedirect=\"" + useTransactionRedirect + "\" ");
        if (super.getUser() != null)
            buf.append("user=\"" + super.getUser() + "\" ");
        if (xaNetworkOptimization != null)
            buf.append("xaNetworkOptimization=\"" + xaNetworkOptimization + "\" ");
        buf.append("}");
        return buf.toString();
    }

}