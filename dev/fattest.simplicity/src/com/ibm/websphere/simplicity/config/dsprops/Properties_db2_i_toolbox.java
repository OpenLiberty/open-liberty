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
public class Properties_db2_i_toolbox extends DataSourceProperties {
    private String access;
    private String behaviorOverride;
    private String bidiImplicitReordering;
    private String bidiNumericOrdering;
    private String bidiStringType;
    private String bigDecimal;
    private String blockCriteria;
    private String blockSize;
    private String cursorHold;
    private String cursorSensitivity;
    private String dataCompression;
    private String dataTruncation;
    private String dateFormat;
    private String dateSeparator;
    private String decimalSeparator;
    private String driver;
    private String dsPackage;
    private String errors;
    private String extendedDynamic;
    private String extendedMetaData;
    private String fullOpen;
    private String holdInputLocators;
    private String holdStatements;
    private String isolationLevelSwitchingSupport;
    private String keepAlive;
    private String lazyClose;
    private String libraries;
    private String lobThreshold;
    private String maximumPrecision;
    private String maximumScale;
    private String metaDataSource;
    private String minimumDivideScale;
    private String naming;
    private String packageAdd;
    private String packageCCSID;
    private String packageCache;
    private String packageCriteria;
    private String packageError;
    private String packageLibrary;
    private String prefetch;
    private String prompt;
    private String proxyServer;
    private String qaqqiniLibrary;
    private String queryOptimizeGoal;
    private String receiveBufferSize;
    private String remarks;
    private String rollbackCursorHold;
    private String savePasswordWhenSerialized;
    private String secondaryUrl;
    private String secure;
    private String sendBufferSize;
    private String serverTraceCategories;
    private String soLinger;
    private String soTimeout;
    private String sort;
    private String sortLanguage;
    private String sortTable;
    private String sortWeight;
    private String tcpNoDelay;
    private String threadUsed;
    private String timeFormat;
    private String timeSeparator;
    private String toolboxTrace;
    private String trace;
    private String translateBinary;
    private String translateBoolean;
    private String translateHex;
    private String trueAutoCommit;
    private String xaLooselyCoupledSupport;

    @Override
    public String getElementName() {
        return DB2_I_TOOLBOX;
    }

    @XmlAttribute(name = "access")
    public void setAccess(String access) {
        this.access = access;
    }

    public String getAccess() {
        return this.access;
    }

    @XmlAttribute(name = "behaviorOverride")
    public void setBehaviorOverride(String behaviorOverride) {
        this.behaviorOverride = behaviorOverride;
    }

    public String getBehaviorOverride() {
        return this.behaviorOverride;
    }

    @XmlAttribute(name = "bidiImplicitReordering")
    public void setBidiImplicitReordering(String bidiImplicitReordering) {
        this.bidiImplicitReordering = bidiImplicitReordering;
    }

    public String getBidiImplicitReordering() {
        return this.bidiImplicitReordering;
    }

    @XmlAttribute(name = "bidiNumericOrdering")
    public void setBidiNumericOrdering(String bidiNumericOrdering) {
        this.bidiNumericOrdering = bidiNumericOrdering;
    }

    public String getBidiNumericOrdering() {
        return this.bidiNumericOrdering;
    }

    @XmlAttribute(name = "bidiStringType")
    public void setBidiStringType(String bidiStringType) {
        this.bidiStringType = bidiStringType;
    }

    public String getBidiStringType() {
        return this.bidiStringType;
    }

    @XmlAttribute(name = "bigDecimal")
    public void setBigDecimal(String bigDecimal) {
        this.bigDecimal = bigDecimal;
    }

    public String getBigDecimal() {
        return this.bigDecimal;
    }

    @XmlAttribute(name = "blockCriteria")
    public void setBlockCriteria(String blockCriteria) {
        this.blockCriteria = blockCriteria;
    }

    public String getBlockCriteria() {
        return this.blockCriteria;
    }

    @XmlAttribute(name = "blockSize")
    public void setBlockSize(String blockSize) {
        this.blockSize = blockSize;
    }

    public String getBlockSize() {
        return this.blockSize;
    }

    @XmlAttribute(name = "cursorHold")
    public void setCursorHold(String cursorHold) {
        this.cursorHold = cursorHold;
    }

    public String getCursorHold() {
        return this.cursorHold;
    }

    @XmlAttribute(name = "cursorSensitivity")
    public void setCursorSensitivity(String cursorSensitivity) {
        this.cursorSensitivity = cursorSensitivity;
    }

    public String getCursorSensitivity() {
        return this.cursorSensitivity;
    }

    @XmlAttribute(name = "dataCompression")
    public void setDataCompression(String dataCompression) {
        this.dataCompression = dataCompression;
    }

    public String getDataCompression() {
        return this.dataCompression;
    }

    @XmlAttribute(name = "dataTruncation")
    public void setDataTruncation(String dataTruncation) {
        this.dataTruncation = dataTruncation;
    }

    public String getDataTruncation() {
        return this.dataTruncation;
    }

    @XmlAttribute(name = "dateFormat")
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getDateFormat() {
        return this.dateFormat;
    }

    @XmlAttribute(name = "dateSeparator")
    public void setDateSeparator(String dateSeparator) {
        this.dateSeparator = dateSeparator;
    }

    public String getDateSeparator() {
        return this.dateSeparator;
    }

    @XmlAttribute(name = "decimalSeparator")
    public void setDecimalSeparator(String decimalSeparator) {
        this.decimalSeparator = decimalSeparator;
    }

    public String getDecimalSeparator() {
        return this.decimalSeparator;
    }

    @XmlAttribute(name = "driver")
    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getDriver() {
        return this.driver;
    }

    @XmlAttribute(name = "dsPackage")
    public void setDsPackage(String dsPackage) {
        this.dsPackage = dsPackage;
    }

    public String getDsPackage() {
        return this.dsPackage;
    }

    @XmlAttribute(name = "errors")
    public void setErrors(String errors) {
        this.errors = errors;
    }

    public String getErrors() {
        return this.errors;
    }

    @XmlAttribute(name = "extendedDynamic")
    public void setExtendedDynamic(String extendedDynamic) {
        this.extendedDynamic = extendedDynamic;
    }

    public String getExtendedDynamic() {
        return this.extendedDynamic;
    }

    @XmlAttribute(name = "extendedMetaData")
    public void setExtendedMetaData(String extendedMetaData) {
        this.extendedMetaData = extendedMetaData;
    }

    public String getExtendedMetaData() {
        return this.extendedMetaData;
    }

    @XmlAttribute(name = "fullOpen")
    public void setFullOpen(String fullOpen) {
        this.fullOpen = fullOpen;
    }

    public String getFullOpen() {
        return this.fullOpen;
    }

    @XmlAttribute(name = "holdInputLocators")
    public void setHoldInputLocators(String holdInputLocators) {
        this.holdInputLocators = holdInputLocators;
    }

    public String getHoldInputLocators() {
        return this.holdInputLocators;
    }

    @XmlAttribute(name = "holdStatements")
    public void setHoldStatements(String holdStatements) {
        this.holdStatements = holdStatements;
    }

    public String getHoldStatements() {
        return this.holdStatements;
    }

    @XmlAttribute(name = "isolationLevelSwitchingSupport")
    public void setIsolationLevelSwitchingSupport(String isolationLevelSwitchingSupport) {
        this.isolationLevelSwitchingSupport = isolationLevelSwitchingSupport;
    }

    public String getIsolationLevelSwitchingSupport() {
        return this.isolationLevelSwitchingSupport;
    }

    @XmlAttribute(name = "keepAlive")
    public void setKeepAlive(String keepAlive) {
        this.keepAlive = keepAlive;
    }

    public String getKeepAlive() {
        return this.keepAlive;
    }

    @XmlAttribute(name = "lazyClose")
    public void setLazyClose(String lazyClose) {
        this.lazyClose = lazyClose;
    }

    public String getLazyClose() {
        return this.lazyClose;
    }

    @XmlAttribute(name = "libraries")
    public void setLibraries(String libraries) {
        this.libraries = libraries;
    }

    public String getLibraries() {
        return this.libraries;
    }

    @XmlAttribute(name = "lobThreshold")
    public void setLobThreshold(String lobThreshold) {
        this.lobThreshold = lobThreshold;
    }

    public String getLobThreshold() {
        return this.lobThreshold;
    }

    @XmlAttribute(name = "maximumPrecision")
    public void setMaximumPrecision(String maximumPrecision) {
        this.maximumPrecision = maximumPrecision;
    }

    public String getMaximumPrecision() {
        return this.maximumPrecision;
    }

    @XmlAttribute(name = "maximumScale")
    public void setMaximumScale(String maximumScale) {
        this.maximumScale = maximumScale;
    }

    public String getMaximumScale() {
        return this.maximumScale;
    }

    @XmlAttribute(name = "metaDataSource")
    public void setMetaDataSource(String metaDataSource) {
        this.metaDataSource = metaDataSource;
    }

    public String getMetaDataSource() {
        return this.metaDataSource;
    }

    @XmlAttribute(name = "minimumDivideScale")
    public void setMinimumDivideScale(String minimumDivideScale) {
        this.minimumDivideScale = minimumDivideScale;
    }

    public String getMinimumDivideScale() {
        return this.minimumDivideScale;
    }

    @XmlAttribute(name = "naming")
    public void setNaming(String naming) {
        this.naming = naming;
    }

    public String getNaming() {
        return this.naming;
    }

    @XmlAttribute(name = "packageAdd")
    public void setPackageAdd(String packageAdd) {
        this.packageAdd = packageAdd;
    }

    public String getPackageAdd() {
        return this.packageAdd;
    }

    @XmlAttribute(name = "packageCCSID")
    public void setPackageCCSID(String packageCCSID) {
        this.packageCCSID = packageCCSID;
    }

    public String getPackageCCSID() {
        return this.packageCCSID;
    }

    @XmlAttribute(name = "packageCache")
    public void setPackageCache(String packageCache) {
        this.packageCache = packageCache;
    }

    public String getPackageCache() {
        return this.packageCache;
    }

    @XmlAttribute(name = "packageCriteria")
    public void setPackageCriteria(String packageCriteria) {
        this.packageCriteria = packageCriteria;
    }

    public String getPackageCriteria() {
        return this.packageCriteria;
    }

    @XmlAttribute(name = "packageError")
    public void setPackageError(String packageError) {
        this.packageError = packageError;
    }

    public String getPackageError() {
        return this.packageError;
    }

    @XmlAttribute(name = "packageLibrary")
    public void setPackageLibrary(String packageLibrary) {
        this.packageLibrary = packageLibrary;
    }

    public String getPackageLibrary() {
        return this.packageLibrary;
    }

    @XmlAttribute(name = "prefetch")
    public void setPrefetch(String prefetch) {
        this.prefetch = prefetch;
    }

    public String getPrefetch() {
        return this.prefetch;
    }

    @XmlAttribute(name = "prompt")
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return this.prompt;
    }

    @XmlAttribute(name = "proxyServer")
    public void setProxyServer(String proxyServer) {
        this.proxyServer = proxyServer;
    }

    public String getProxyServer() {
        return this.proxyServer;
    }

    @XmlAttribute(name = "qaqqiniLibrary")
    public void setQaqqiniLibrary(String qaqqiniLibrary) {
        this.qaqqiniLibrary = qaqqiniLibrary;
    }

    public String getQaqqiniLibrary() {
        return this.qaqqiniLibrary;
    }

    @XmlAttribute(name = "queryOptimizeGoal")
    public void setQueryOptimizeGoal(String queryOptimizeGoal) {
        this.queryOptimizeGoal = queryOptimizeGoal;
    }

    public String getQueryOptimizeGoal() {
        return this.queryOptimizeGoal;
    }

    @XmlAttribute(name = "receiveBufferSize")
    public void setReceiveBufferSize(String receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    public String getReceiveBufferSize() {
        return this.receiveBufferSize;
    }

    @XmlAttribute(name = "remarks")
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getRemarks() {
        return this.remarks;
    }

    @XmlAttribute(name = "rollbackCursorHold")
    public void setRollbackCursorHold(String rollbackCursorHold) {
        this.rollbackCursorHold = rollbackCursorHold;
    }

    public String getRollbackCursorHold() {
        return this.rollbackCursorHold;
    }

    @XmlAttribute(name = "savePasswordWhenSerialized")
    public void setSavePasswordWhenSerialized(String savePasswordWhenSerialized) {
        this.savePasswordWhenSerialized = savePasswordWhenSerialized;
    }

    public String getSavePasswordWhenSerialized() {
        return this.savePasswordWhenSerialized;
    }

    @XmlAttribute(name = "secondaryUrl")
    public void setSecondaryUrl(String secondaryUrl) {
        this.secondaryUrl = secondaryUrl;
    }

    public String getSecondaryUrl() {
        return this.secondaryUrl;
    }

    @XmlAttribute(name = "secure")
    public void setSecure(String secure) {
        this.secure = secure;
    }

    public String getSecure() {
        return this.secure;
    }

    @XmlAttribute(name = "sendBufferSize")
    public void setSendBufferSize(String sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    public String getSendBufferSize() {
        return this.sendBufferSize;
    }

    @XmlAttribute(name = "serverTraceCategories")
    public void setServerTraceCategories(String serverTraceCategories) {
        this.serverTraceCategories = serverTraceCategories;
    }

    public String getServerTraceCategories() {
        return this.serverTraceCategories;
    }

    @XmlAttribute(name = "soLinger")
    public void setSoLinger(String soLinger) {
        this.soLinger = soLinger;
    }

    public String getSoLinger() {
        return this.soLinger;
    }

    @XmlAttribute(name = "soTimeout")
    public void setSoTimeout(String soTimeout) {
        this.soTimeout = soTimeout;
    }

    public String getSoTimeout() {
        return this.soTimeout;
    }

    @XmlAttribute(name = "sort")
    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getSort() {
        return this.sort;
    }

    @XmlAttribute(name = "sortLanguage")
    public void setSortLanguage(String sortLanguage) {
        this.sortLanguage = sortLanguage;
    }

    public String getSortLanguage() {
        return this.sortLanguage;
    }

    @XmlAttribute(name = "sortTable")
    public void setSortTable(String sortTable) {
        this.sortTable = sortTable;
    }

    public String getSortTable() {
        return this.sortTable;
    }

    @XmlAttribute(name = "sortWeight")
    public void setSortWeight(String sortWeight) {
        this.sortWeight = sortWeight;
    }

    public String getSortWeight() {
        return this.sortWeight;
    }

    @XmlAttribute(name = "tcpNoDelay")
    public void setTcpNoDelay(String tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public String getTcpNoDelay() {
        return this.tcpNoDelay;
    }

    @XmlAttribute(name = "threadUsed")
    public void setThreadUsed(String threadUsed) {
        this.threadUsed = threadUsed;
    }

    public String getThreadUsed() {
        return this.threadUsed;
    }

    @XmlAttribute(name = "timeFormat")
    public void setTimeFormat(String timeFormat) {
        this.timeFormat = timeFormat;
    }

    public String getTimeFormat() {
        return this.timeFormat;
    }

    @XmlAttribute(name = "timeSeparator")
    public void setTimeSeparator(String timeSeparator) {
        this.timeSeparator = timeSeparator;
    }

    public String getTimeSeparator() {
        return this.timeSeparator;
    }

    @XmlAttribute(name = "toolboxTrace")
    public void setToolboxTrace(String toolboxTrace) {
        this.toolboxTrace = toolboxTrace;
    }

    public String getToolboxTrace() {
        return this.toolboxTrace;
    }

    @XmlAttribute(name = "trace")
    public void setTrace(String trace) {
        this.trace = trace;
    }

    public String getTrace() {
        return this.trace;
    }

    @XmlAttribute(name = "translateBinary")
    public void setTranslateBinary(String translateBinary) {
        this.translateBinary = translateBinary;
    }

    public String getTranslateBinary() {
        return this.translateBinary;
    }

    @XmlAttribute(name = "translateBoolean")
    public void setTranslateBoolean(String translateBoolean) {
        this.translateBoolean = translateBoolean;
    }

    public String getTranslateBoolean() {
        return this.translateBoolean;
    }

    @XmlAttribute(name = "translateHex")
    public void setTranslateHex(String translateHex) {
        this.translateHex = translateHex;
    }

    public String getTranslateHex() {
        return this.translateHex;
    }

    @XmlAttribute(name = "trueAutoCommit")
    public void setTrueAutoCommit(String trueAutoCommit) {
        this.trueAutoCommit = trueAutoCommit;
    }

    public String getTrueAutoCommit() {
        return this.trueAutoCommit;
    }

    @XmlAttribute(name = "xaLooselyCoupledSupport")
    public void setXaLooselyCoupledSupport(String xaLooselyCoupledSupport) {
        this.xaLooselyCoupledSupport = xaLooselyCoupledSupport;
    }

    public String getXaLooselyCoupledSupport() {
        return this.xaLooselyCoupledSupport;
    }

    /**
     * Returns a String listing the properties and their values used on this
     * data source.
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("{");
        if (access != null)
            buf.append("access=\"" + access + "\" ");
        if (behaviorOverride != null)
            buf.append("behaviorOverride=\"" + behaviorOverride + "\" ");
        if (bidiImplicitReordering != null)
            buf.append("bidiImplicitReordering=\"" + bidiImplicitReordering + "\" ");
        if (bidiNumericOrdering != null)
            buf.append("bidiNumericOrdering=\"" + bidiNumericOrdering + "\" ");
        if (bidiStringType != null)
            buf.append("bidiStringType=\"" + bidiStringType + "\" ");
        if (bigDecimal != null)
            buf.append("bigDecimal=\"" + bigDecimal + "\" ");
        if (blockCriteria != null)
            buf.append("blockCriteria=\"" + blockCriteria + "\" ");
        if (blockSize != null)
            buf.append("blockSize=\"" + blockSize + "\" ");
        if (cursorHold != null)
            buf.append("cursorHold=\"" + cursorHold + "\" ");
        if (cursorSensitivity != null)
            buf.append("cursorSensitivity=\"" + cursorSensitivity + "\" ");
        if (dataCompression != null)
            buf.append("dataCompression=\"" + dataCompression + "\" ");
        if (dataTruncation != null)
            buf.append("dataTruncation=\"" + dataTruncation + "\" ");
        if (super.getDatabaseName() != null)
            buf.append("databaseName=\"" + super.getDatabaseName() + "\" ");
        if (dateFormat != null)
            buf.append("dateFormat=\"" + dateFormat + "\" ");
        if (dateSeparator != null)
            buf.append("dateSeparator=\"" + dateSeparator + "\" ");
        if (decimalSeparator != null)
            buf.append("decimalSeparator=\"" + decimalSeparator + "\" ");
        if (driver != null)
            buf.append("driver=\"" + driver + "\" ");
        if (dsPackage != null)
            buf.append("dsPackage=\"" + dsPackage + "\" ");
        if (errors != null)
            buf.append("errors=\"" + errors + "\" ");
        if (extendedDynamic != null)
            buf.append("extendedDynamic=\"" + extendedDynamic + "\" ");
        if (extendedMetaData != null)
            buf.append("extendedMetaData=\"" + extendedMetaData + "\" ");
        if (fullOpen != null)
            buf.append("fullOpen=\"" + fullOpen + "\" ");
        if (holdInputLocators != null)
            buf.append("holdInputLocators=\"" + holdInputLocators + "\" ");
        if (holdStatements != null)
            buf.append("holdStatements=\"" + holdStatements + "\" ");
        if (isolationLevelSwitchingSupport != null)
            buf.append("isolationLevelSwitchingSupport=\"" + isolationLevelSwitchingSupport + "\" ");
        if (keepAlive != null)
            buf.append("keepAlive=\"" + keepAlive + "\" ");
        if (lazyClose != null)
            buf.append("lazyClose=\"" + lazyClose + "\" ");
        if (libraries != null)
            buf.append("libraries=\"" + libraries + "\" ");
        if (lobThreshold != null)
            buf.append("lobThreshold=\"" + lobThreshold + "\" ");
        if (super.getLoginTimeout() != null)
            buf.append("loginTimeout=\"" + super.getLoginTimeout() + "\" ");
        if (maximumPrecision != null)
            buf.append("maximumPrecision=\"" + maximumPrecision + "\" ");
        if (maximumScale != null)
            buf.append("maximumScale=\"" + maximumScale + "\" ");
        if (metaDataSource != null)
            buf.append("metaDataSource=\"" + metaDataSource + "\" ");
        if (minimumDivideScale != null)
            buf.append("minimumDivideScale=\"" + minimumDivideScale + "\" ");
        if (naming != null)
            buf.append("naming=\"" + naming + "\" ");
        if (packageAdd != null)
            buf.append("packageAdd=\"" + packageAdd + "\" ");
        if (packageCCSID != null)
            buf.append("packageCCSID=\"" + packageCCSID + "\" ");
        if (packageCache != null)
            buf.append("packageCache=\"" + packageCache + "\" ");
        if (packageCriteria != null)
            buf.append("packageCriteria=\"" + packageCriteria + "\" ");
        if (packageError != null)
            buf.append("packageError=\"" + packageError + "\" ");
        if (packageLibrary != null)
            buf.append("packageLibrary=\"" + packageLibrary + "\" ");
        if (super.getPassword() != null)
            buf.append("password=\"" + super.getPassword() + "\" ");
        if (prefetch != null)
            buf.append("prefetch=\"" + prefetch + "\" ");
        if (prompt != null)
            buf.append("prompt=\"" + prompt + "\" ");
        if (proxyServer != null)
            buf.append("proxyServer=\"" + proxyServer + "\" ");
        if (qaqqiniLibrary != null)
            buf.append("qaqqiniLibrary=\"" + qaqqiniLibrary + "\" ");
        if (queryOptimizeGoal != null)
            buf.append("queryOptimizeGoal=\"" + queryOptimizeGoal + "\" ");
        if (receiveBufferSize != null)
            buf.append("receiveBufferSize=\"" + receiveBufferSize + "\" ");
        if (remarks != null)
            buf.append("remarks=\"" + remarks + "\" ");
        if (rollbackCursorHold != null)
            buf.append("rollbackCursorHold=\"" + rollbackCursorHold + "\" ");
        if (savePasswordWhenSerialized != null)
            buf.append("savePasswordWhenSerialized=\"" + savePasswordWhenSerialized + "\" ");
        if (secondaryUrl != null)
            buf.append("secondaryUrl=\"" + secondaryUrl + "\" ");
        if (secure != null)
            buf.append("secure=\"" + secure + "\" ");
        if (sendBufferSize != null)
            buf.append("sendBufferSize=\"" + sendBufferSize + "\" ");
        if (super.getServerName() != null)
            buf.append("serverName=\"" + super.getServerName() + "\" ");
        if (serverTraceCategories != null)
            buf.append("serverTraceCategories=\"" + serverTraceCategories + "\" ");
        if (soLinger != null)
            buf.append("soLinger=\"" + soLinger + "\" ");
        if (soTimeout != null)
            buf.append("soTimeout=\"" + soTimeout + "\" ");
        if (sort != null)
            buf.append("sort=\"" + sort + "\" ");
        if (sortLanguage != null)
            buf.append("sortLanguage=\"" + sortLanguage + "\" ");
        if (sortTable != null)
            buf.append("sortTable=\"" + sortTable + "\" ");
        if (sortWeight != null)
            buf.append("sortWeight=\"" + sortWeight + "\" ");
        if (tcpNoDelay != null)
            buf.append("tcpNoDelay=\"" + tcpNoDelay + "\" ");
        if (threadUsed != null)
            buf.append("threadUsed=\"" + threadUsed + "\" ");
        if (timeFormat != null)
            buf.append("timeFormat=\"" + timeFormat + "\" ");
        if (timeSeparator != null)
            buf.append("timeSeparator=\"" + timeSeparator + "\" ");
        if (toolboxTrace != null)
            buf.append("toolboxTrace=\"" + toolboxTrace + "\" ");
        if (trace != null)
            buf.append("trace=\"" + trace + "\" ");
        if (translateBinary != null)
            buf.append("translateBinary=\"" + translateBinary + "\" ");
        if (translateBoolean != null)
            buf.append("translateBoolean=\"" + translateBoolean + "\" ");
        if (translateHex != null)
            buf.append("translateHex=\"" + translateHex + "\" ");
        if (trueAutoCommit != null)
            buf.append("trueAutoCommit=\"" + trueAutoCommit + "\" ");
        if (super.getUser() != null)
            buf.append("user=\"" + super.getUser() + "\" ");
        if (xaLooselyCoupledSupport != null)
            buf.append("xaLooselyCoupledSupport=\"" + xaLooselyCoupledSupport + "\" ");
        buf.append("}");
        return buf.toString();
    }

}