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
public class Properties_informix_jcc extends DataSourceProperties {
    private String DBANSIWARN;
    private String DBDATE;
    private String DBPATH;
    private String DBSPACETEMP;
    private String DBTEMP;
    private String DBUPSPACE;
    private String DELIMIDENT;
    private String IFX_DIRECTIVES;
    private String IFX_EXTDIRECTIVES;
    private String IFX_UPDDESC;
    private String IFX_XASTDCOMPLIANCE_XAEND;
    private String INFORMIXOPCACHE;
    private String INFORMIXSTACKSIZE;
    private String NODEFDAC;
    private String OPTCOMPIND;
    private String OPTOFC;
    private String PDQPRIORITY;
    private String PSORT_DBTEMP;
    private String PSORT_NPROCS;
    private String STMT_CACHE;
    private String currentLockTimeout;
    private String deferPrepares;
    private String driverType;
    private String enableNamedParameterMarkers;
    private String enableSeamlessFailover;
    private String enableSysplexWLB;
    private String fetchSize;
    private String fullyMaterializeLobData;
    private String keepDynamic;
    private String progressiveStreaming;
    private String queryDataSize;
    private String resultSetHoldability;
    private String resultSetHoldabilityForCatalogQueries;
    private String retrieveMessagesFromServerOnGetMessage;
    private String securityMechanism;
    private String traceDirectory;
    private String traceFile;
    private String traceFileAppend;
    private String traceLevel;
    private String useJDBC4ColumnNameAndLabelSemantics;

    @Override
    public String getElementName() {
        return INFORMIX_JCC;
    }

    @XmlAttribute(name = "DBANSIWARN")
    public void setDBANSIWARN(String DBANSIWARN) {
        this.DBANSIWARN = DBANSIWARN;
    }

    public String getDBANSIWARN() {
        return this.DBANSIWARN;
    }

    @XmlAttribute(name = "DBDATE")
    public void setDBDATE(String DBDATE) {
        this.DBDATE = DBDATE;
    }

    public String getDBDATE() {
        return this.DBDATE;
    }

    @XmlAttribute(name = "DBPATH")
    public void setDBPATH(String DBPATH) {
        this.DBPATH = DBPATH;
    }

    public String getDBPATH() {
        return this.DBPATH;
    }

    @XmlAttribute(name = "DBSPACETEMP")
    public void setDBSPACETEMP(String DBSPACETEMP) {
        this.DBSPACETEMP = DBSPACETEMP;
    }

    public String getDBSPACETEMP() {
        return this.DBSPACETEMP;
    }

    @XmlAttribute(name = "DBTEMP")
    public void setDBTEMP(String DBTEMP) {
        this.DBTEMP = DBTEMP;
    }

    public String getDBTEMP() {
        return this.DBTEMP;
    }

    @XmlAttribute(name = "DBUPSPACE")
    public void setDBUPSPACE(String DBUPSPACE) {
        this.DBUPSPACE = DBUPSPACE;
    }

    public String getDBUPSPACE() {
        return this.DBUPSPACE;
    }

    @XmlAttribute(name = "DELIMIDENT")
    public void setDELIMIDENT(String DELIMIDENT) {
        this.DELIMIDENT = DELIMIDENT;
    }

    public String getDELIMIDENT() {
        return this.DELIMIDENT;
    }

    @XmlAttribute(name = "IFX_DIRECTIVES")
    public void setIFX_DIRECTIVES(String IFX_DIRECTIVES) {
        this.IFX_DIRECTIVES = IFX_DIRECTIVES;
    }

    public String getIFX_DIRECTIVES() {
        return this.IFX_DIRECTIVES;
    }

    @XmlAttribute(name = "IFX_EXTDIRECTIVES")
    public void setIFX_EXTDIRECTIVES(String IFX_EXTDIRECTIVES) {
        this.IFX_EXTDIRECTIVES = IFX_EXTDIRECTIVES;
    }

    public String getIFX_EXTDIRECTIVES() {
        return this.IFX_EXTDIRECTIVES;
    }

    @XmlAttribute(name = "IFX_UPDDESC")
    public void setIFX_UPDDESC(String IFX_UPDDESC) {
        this.IFX_UPDDESC = IFX_UPDDESC;
    }

    public String getIFX_UPDDESC() {
        return this.IFX_UPDDESC;
    }

    @XmlAttribute(name = "IFX_XASTDCOMPLIANCE_XAEND")
    public void setIFX_XASTDCOMPLIANCE_XAEND(String IFX_XASTDCOMPLIANCE_XAEND) {
        this.IFX_XASTDCOMPLIANCE_XAEND = IFX_XASTDCOMPLIANCE_XAEND;
    }

    public String getIFX_XASTDCOMPLIANCE_XAEND() {
        return this.IFX_XASTDCOMPLIANCE_XAEND;
    }

    @XmlAttribute(name = "INFORMIXOPCACHE")
    public void setINFORMIXOPCACHE(String INFORMIXOPCACHE) {
        this.INFORMIXOPCACHE = INFORMIXOPCACHE;
    }

    public String getINFORMIXOPCACHE() {
        return this.INFORMIXOPCACHE;
    }

    @XmlAttribute(name = "INFORMIXSTACKSIZE")
    public void setINFORMIXSTACKSIZE(String INFORMIXSTACKSIZE) {
        this.INFORMIXSTACKSIZE = INFORMIXSTACKSIZE;
    }

    public String getINFORMIXSTACKSIZE() {
        return this.INFORMIXSTACKSIZE;
    }

    @XmlAttribute(name = "NODEFDAC")
    public void setNODEFDAC(String NODEFDAC) {
        this.NODEFDAC = NODEFDAC;
    }

    public String getNODEFDAC() {
        return this.NODEFDAC;
    }

    @XmlAttribute(name = "OPTCOMPIND")
    public void setOPTCOMPIND(String OPTCOMPIND) {
        this.OPTCOMPIND = OPTCOMPIND;
    }

    public String getOPTCOMPIND() {
        return this.OPTCOMPIND;
    }

    @XmlAttribute(name = "OPTOFC")
    public void setOPTOFC(String OPTOFC) {
        this.OPTOFC = OPTOFC;
    }

    public String getOPTOFC() {
        return this.OPTOFC;
    }

    @XmlAttribute(name = "PDQPRIORITY")
    public void setPDQPRIORITY(String PDQPRIORITY) {
        this.PDQPRIORITY = PDQPRIORITY;
    }

    public String getPDQPRIORITY() {
        return this.PDQPRIORITY;
    }

    @XmlAttribute(name = "PSORT_DBTEMP")
    public void setPSORT_DBTEMP(String PSORT_DBTEMP) {
        this.PSORT_DBTEMP = PSORT_DBTEMP;
    }

    public String getPSORT_DBTEMP() {
        return this.PSORT_DBTEMP;
    }

    @XmlAttribute(name = "PSORT_NPROCS")
    public void setPSORT_NPROCS(String PSORT_NPROCS) {
        this.PSORT_NPROCS = PSORT_NPROCS;
    }

    public String getPSORT_NPROCS() {
        return this.PSORT_NPROCS;
    }

    @XmlAttribute(name = "STMT_CACHE")
    public void setSTMT_CACHE(String STMT_CACHE) {
        this.STMT_CACHE = STMT_CACHE;
    }

    public String getSTMT_CACHE() {
        return this.STMT_CACHE;
    }

    @XmlAttribute(name = "currentLockTimeout")
    public void setCurrentLockTimeout(String currentLockTimeout) {
        this.currentLockTimeout = currentLockTimeout;
    }

    public String getCurrentLockTimeout() {
        return this.currentLockTimeout;
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

    @XmlAttribute(name = "fullyMaterializeLobData")
    public void setFullyMaterializeLobData(String fullyMaterializeLobData) {
        this.fullyMaterializeLobData = fullyMaterializeLobData;
    }

    public String getFullyMaterializeLobData() {
        return this.fullyMaterializeLobData;
    }

    @XmlAttribute(name = "keepDynamic")
    public void setKeepDynamic(String keepDynamic) {
        this.keepDynamic = keepDynamic;
    }

    public String getKeepDynamic() {
        return this.keepDynamic;
    }

    @XmlAttribute(name = "progressiveStreaming")
    public void setProgressiveStreaming(String progressiveStreaming) {
        this.progressiveStreaming = progressiveStreaming;
    }

    public String getProgressiveStreaming() {
        return this.progressiveStreaming;
    }

    @XmlAttribute(name = "queryDataSize")
    public void setQueryDataSize(String queryDataSize) {
        this.queryDataSize = queryDataSize;
    }

    public String getQueryDataSize() {
        return this.queryDataSize;
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

    @XmlAttribute(name = "securityMechanism")
    public void setSecurityMechanism(String securityMechanism) {
        this.securityMechanism = securityMechanism;
    }

    public String getSecurityMechanism() {
        return this.securityMechanism;
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

    @XmlAttribute(name = "useJDBC4ColumnNameAndLabelSemantics")
    public void setUseJDBC4ColumnNameAndLabelSemantics(String useJDBC4ColumnNameAndLabelSemantics) {
        this.useJDBC4ColumnNameAndLabelSemantics = useJDBC4ColumnNameAndLabelSemantics;
    }

    public String getUseJDBC4ColumnNameAndLabelSemantics() {
        return this.useJDBC4ColumnNameAndLabelSemantics;
    }

    /**
     * Returns a String listing the properties and their values used on this
     * data source.
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("{");
        if (DBANSIWARN != null)
            buf.append("DBANSIWARN=\"" + DBANSIWARN + "\" ");
        if (DBDATE != null)
            buf.append("DBDATE=\"" + DBDATE + "\" ");
        if (DBPATH != null)
            buf.append("DBPATH=\"" + DBPATH + "\" ");
        if (DBSPACETEMP != null)
            buf.append("DBSPACETEMP=\"" + DBSPACETEMP + "\" ");
        if (DBTEMP != null)
            buf.append("DBTEMP=\"" + DBTEMP + "\" ");
        if (DBUPSPACE != null)
            buf.append("DBUPSPACE=\"" + DBUPSPACE + "\" ");
        if (DELIMIDENT != null)
            buf.append("DELIMIDENT=\"" + DELIMIDENT + "\" ");
        if (IFX_DIRECTIVES != null)
            buf.append("IFX_DIRECTIVES=\"" + IFX_DIRECTIVES + "\" ");
        if (IFX_EXTDIRECTIVES != null)
            buf.append("IFX_EXTDIRECTIVES=\"" + IFX_EXTDIRECTIVES + "\" ");
        if (IFX_UPDDESC != null)
            buf.append("IFX_UPDDESC=\"" + IFX_UPDDESC + "\" ");
        if (IFX_XASTDCOMPLIANCE_XAEND != null)
            buf.append("IFX_XASTDCOMPLIANCE_XAEND=\"" + IFX_XASTDCOMPLIANCE_XAEND + "\" ");
        if (INFORMIXOPCACHE != null)
            buf.append("INFORMIXOPCACHE=\"" + INFORMIXOPCACHE + "\" ");
        if (INFORMIXSTACKSIZE != null)
            buf.append("INFORMIXSTACKSIZE=\"" + INFORMIXSTACKSIZE + "\" ");
        if (NODEFDAC != null)
            buf.append("NODEFDAC=\"" + NODEFDAC + "\" ");
        if (OPTCOMPIND != null)
            buf.append("OPTCOMPIND=\"" + OPTCOMPIND + "\" ");
        if (OPTOFC != null)
            buf.append("OPTOFC=\"" + OPTOFC + "\" ");
        if (PDQPRIORITY != null)
            buf.append("PDQPRIORITY=\"" + PDQPRIORITY + "\" ");
        if (PSORT_DBTEMP != null)
            buf.append("PSORT_DBTEMP=\"" + PSORT_DBTEMP + "\" ");
        if (PSORT_NPROCS != null)
            buf.append("PSORT_NPROCS=\"" + PSORT_NPROCS + "\" ");
        if (STMT_CACHE != null)
            buf.append("STMT_CACHE=\"" + STMT_CACHE + "\" ");
        if (currentLockTimeout != null)
            buf.append("currentLockTimeout=\"" + currentLockTimeout + "\" ");
        if (super.getDatabaseName() != null)
            buf.append("databaseName=\"" + super.getDatabaseName() + "\" ");
        if (deferPrepares != null)
            buf.append("deferPrepares=\"" + deferPrepares + "\" ");
        if (driverType != null)
            buf.append("driverType=\"" + driverType + "\" ");
        if (enableNamedParameterMarkers != null)
            buf.append("enableNamedParameterMarkers=\"" + enableNamedParameterMarkers + "\" ");
        if (enableSeamlessFailover != null)
            buf.append("enableSeamlessFailover=\"" + enableSeamlessFailover + "\" ");
        if (enableSysplexWLB != null)
            buf.append("enableSysplexWLB=\"" + enableSysplexWLB + "\" ");
        if (fetchSize != null)
            buf.append("fetchSize=\"" + fetchSize + "\" ");
        if (fullyMaterializeLobData != null)
            buf.append("fullyMaterializeLobData=\"" + fullyMaterializeLobData + "\" ");
        if (keepDynamic != null)
            buf.append("keepDynamic=\"" + keepDynamic + "\" ");
        if (super.getLoginTimeout() != null)
            buf.append("loginTimeout=\"" + super.getLoginTimeout() + "\" ");
        if (super.getPassword() != null)
            buf.append("password=\"" + super.getPassword() + "\" ");
        if (super.getPortNumber() != null)
            buf.append("portNumber=\"" + super.getPortNumber() + "\" ");
        if (progressiveStreaming != null)
            buf.append("progressiveStreaming=\"" + progressiveStreaming + "\" ");
        if (queryDataSize != null)
            buf.append("queryDataSize=\"" + queryDataSize + "\" ");
        if (resultSetHoldability != null)
            buf.append("resultSetHoldability=\"" + resultSetHoldability + "\" ");
        if (resultSetHoldabilityForCatalogQueries != null)
            buf.append("resultSetHoldabilityForCatalogQueries=\"" + resultSetHoldabilityForCatalogQueries + "\" ");
        if (retrieveMessagesFromServerOnGetMessage != null)
            buf.append("retrieveMessagesFromServerOnGetMessage=\"" + retrieveMessagesFromServerOnGetMessage + "\" ");
        if (securityMechanism != null)
            buf.append("securityMechanism=\"" + securityMechanism + "\" ");
        if (super.getServerName() != null)
            buf.append("serverName=\"" + super.getServerName() + "\" ");
        if (traceDirectory != null)
            buf.append("traceDirectory=\"" + traceDirectory + "\" ");
        if (traceFile != null)
            buf.append("traceFile=\"" + traceFile + "\" ");
        if (traceFileAppend != null)
            buf.append("traceFileAppend=\"" + traceFileAppend + "\" ");
        if (traceLevel != null)
            buf.append("traceLevel=\"" + traceLevel + "\" ");
        if (useJDBC4ColumnNameAndLabelSemantics != null)
            buf.append("useJDBC4ColumnNameAndLabelSemantics=\"" + useJDBC4ColumnNameAndLabelSemantics + "\" ");
        if (super.getUser() != null)
            buf.append("user=\"" + super.getUser() + "\" ");
        buf.append("}");
        return buf.toString();
    }

}