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
public class Properties_db2_i_native extends DataSourceProperties {
    private String access;
    private String autoCommit;
    private String batchStyle;
    private String behaviorOverride;
    private String blockSize;
    private String cursorHold;
    private String cursorSensitivity;
    private String dataTruncation;
    private String dateFormat;
    private String dateSeparator;
    private String decimalSeparator;
    private String directMap;
    private String doEscapeProcessing;
    private String fullErrors;
    private String libraries;
    private String lobThreshold;
    private String lockTimeout;
    private String maximumPrecision;
    private String maximumScale;
    private String minimumDivideScale;
    private String networkProtocol;
    private String prefetch;
    private String queryOptimizeGoal;
    private String reuseObjects;
    private String serverTraceCategories;
    private String systemNaming;
    private String timeFormat;
    private String timeSeparator;
    private String trace;
    private String transactionTimeout;
    private String translateBinary;
    private String translateHex;
    private String useBlockInsert;

    @Override
    public String getElementName() {
        return DB2_I_NATIVE;
    }

    @XmlAttribute(name = "access")
    public void setAccess(String access) {
        this.access = access;
    }

    public String getAccess() {
        return this.access;
    }

    @XmlAttribute(name = "autoCommit")
    public void setAutoCommit(String autoCommit) {
        this.autoCommit = autoCommit;
    }

    public String getAutoCommit() {
        return this.autoCommit;
    }

    @XmlAttribute(name = "batchStyle")
    public void setBatchStyle(String batchStyle) {
        this.batchStyle = batchStyle;
    }

    public String getBatchStyle() {
        return this.batchStyle;
    }

    @XmlAttribute(name = "behaviorOverride")
    public void setBehaviorOverride(String behaviorOverride) {
        this.behaviorOverride = behaviorOverride;
    }

    public String getBehaviorOverride() {
        return this.behaviorOverride;
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

    @XmlAttribute(name = "directMap")
    public void setDirectMap(String directMap) {
        this.directMap = directMap;
    }

    public String getDirectMap() {
        return this.directMap;
    }

    @XmlAttribute(name = "doEscapeProcessing")
    public void setDoEscapeProcessing(String doEscapeProcessing) {
        this.doEscapeProcessing = doEscapeProcessing;
    }

    public String getDoEscapeProcessing() {
        return this.doEscapeProcessing;
    }

    @XmlAttribute(name = "fullErrors")
    public void setFullErrors(String fullErrors) {
        this.fullErrors = fullErrors;
    }

    public String getFullErrors() {
        return this.fullErrors;
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

    @XmlAttribute(name = "lockTimeout")
    public void setLockTimeout(String lockTimeout) {
        this.lockTimeout = lockTimeout;
    }

    public String getLockTimeout() {
        return this.lockTimeout;
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

    @XmlAttribute(name = "minimumDivideScale")
    public void setMinimumDivideScale(String minimumDivideScale) {
        this.minimumDivideScale = minimumDivideScale;
    }

    public String getMinimumDivideScale() {
        return this.minimumDivideScale;
    }

    @XmlAttribute(name = "networkProtocol")
    public void setNetworkProtocol(String networkProtocol) {
        this.networkProtocol = networkProtocol;
    }

    public String getNetworkProtocol() {
        return this.networkProtocol;
    }

    @XmlAttribute(name = "prefetch")
    public void setPrefetch(String prefetch) {
        this.prefetch = prefetch;
    }

    public String getPrefetch() {
        return this.prefetch;
    }

    @XmlAttribute(name = "queryOptimizeGoal")
    public void setQueryOptimizeGoal(String queryOptimizeGoal) {
        this.queryOptimizeGoal = queryOptimizeGoal;
    }

    public String getQueryOptimizeGoal() {
        return this.queryOptimizeGoal;
    }

    @XmlAttribute(name = "reuseObjects")
    public void setReuseObjects(String reuseObjects) {
        this.reuseObjects = reuseObjects;
    }

    public String getReuseObjects() {
        return this.reuseObjects;
    }

    @XmlAttribute(name = "serverTraceCategories")
    public void setServerTraceCategories(String serverTraceCategories) {
        this.serverTraceCategories = serverTraceCategories;
    }

    public String getServerTraceCategories() {
        return this.serverTraceCategories;
    }

    @XmlAttribute(name = "systemNaming")
    public void setSystemNaming(String systemNaming) {
        this.systemNaming = systemNaming;
    }

    public String getSystemNaming() {
        return this.systemNaming;
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

    @XmlAttribute(name = "trace")
    public void setTrace(String trace) {
        this.trace = trace;
    }

    public String getTrace() {
        return this.trace;
    }

    @XmlAttribute(name = "transactionTimeout")
    public void setTransactionTimeout(String transactionTimeout) {
        this.transactionTimeout = transactionTimeout;
    }

    public String getTransactionTimeout() {
        return this.transactionTimeout;
    }

    @XmlAttribute(name = "translateBinary")
    public void setTranslateBinary(String translateBinary) {
        this.translateBinary = translateBinary;
    }

    public String getTranslateBinary() {
        return this.translateBinary;
    }

    @XmlAttribute(name = "translateHex")
    public void setTranslateHex(String translateHex) {
        this.translateHex = translateHex;
    }

    public String getTranslateHex() {
        return this.translateHex;
    }

    @XmlAttribute(name = "useBlockInsert")
    public void setUseBlockInsert(String useBlockInsert) {
        this.useBlockInsert = useBlockInsert;
    }

    public String getUseBlockInsert() {
        return this.useBlockInsert;
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
        if (autoCommit != null)
            buf.append("autoCommit=\"" + autoCommit + "\" ");
        if (batchStyle != null)
            buf.append("batchStyle=\"" + batchStyle + "\" ");
        if (behaviorOverride != null)
            buf.append("behaviorOverride=\"" + behaviorOverride + "\" ");
        if (blockSize != null)
            buf.append("blockSize=\"" + blockSize + "\" ");
        if (cursorHold != null)
            buf.append("cursorHold=\"" + cursorHold + "\" ");
        if (cursorSensitivity != null)
            buf.append("cursorSensitivity=\"" + cursorSensitivity + "\" ");
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
        if (directMap != null)
            buf.append("directMap=\"" + directMap + "\" ");
        if (doEscapeProcessing != null)
            buf.append("doEscapeProcessing=\"" + doEscapeProcessing + "\" ");
        if (fullErrors != null)
            buf.append("fullErrors=\"" + fullErrors + "\" ");
        if (libraries != null)
            buf.append("libraries=\"" + libraries + "\" ");
        if (lobThreshold != null)
            buf.append("lobThreshold=\"" + lobThreshold + "\" ");
        if (lockTimeout != null)
            buf.append("lockTimeout=\"" + lockTimeout + "\" ");
        if (super.getLoginTimeout() != null)
            buf.append("loginTimeout=\"" + super.getLoginTimeout() + "\" ");
        if (maximumPrecision != null)
            buf.append("maximumPrecision=\"" + maximumPrecision + "\" ");
        if (maximumScale != null)
            buf.append("maximumScale=\"" + maximumScale + "\" ");
        if (minimumDivideScale != null)
            buf.append("minimumDivideScale=\"" + minimumDivideScale + "\" ");
        if (networkProtocol != null)
            buf.append("networkProtocol=\"" + networkProtocol + "\" ");
        if (super.getPassword() != null)
            buf.append("password=\"" + super.getPassword() + "\" ");
        if (super.getPortNumber() != null)
            buf.append("portNumber=\"" + super.getPortNumber() + "\" ");
        if (prefetch != null)
            buf.append("prefetch=\"" + prefetch + "\" ");
        if (queryOptimizeGoal != null)
            buf.append("queryOptimizeGoal=\"" + queryOptimizeGoal + "\" ");
        if (reuseObjects != null)
            buf.append("reuseObjects=\"" + reuseObjects + "\" ");
        if (super.getServerName() != null)
            buf.append("serverName=\"" + super.getServerName() + "\" ");
        if (serverTraceCategories != null)
            buf.append("serverTraceCategories=\"" + serverTraceCategories + "\" ");
        if (systemNaming != null)
            buf.append("systemNaming=\"" + systemNaming + "\" ");
        if (timeFormat != null)
            buf.append("timeFormat=\"" + timeFormat + "\" ");
        if (timeSeparator != null)
            buf.append("timeSeparator=\"" + timeSeparator + "\" ");
        if (trace != null)
            buf.append("trace=\"" + trace + "\" ");
        if (transactionTimeout != null)
            buf.append("transactionTimeout=\"" + transactionTimeout + "\" ");
        if (translateBinary != null)
            buf.append("translateBinary=\"" + translateBinary + "\" ");
        if (translateHex != null)
            buf.append("translateHex=\"" + translateHex + "\" ");
        if (useBlockInsert != null)
            buf.append("useBlockInsert=\"" + useBlockInsert + "\" ");
        if (super.getUser() != null)
            buf.append("user=\"" + super.getUser() + "\" ");
        buf.append("}");
        return buf.toString();
    }

}