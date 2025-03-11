/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Lists data source properties. Some properties are generic, others are driver specifc.
 */
public class Transaction extends ConfigElement {
    private Boolean acceptHeuristicHazard;
    private String clientInactivityTimeout;
    private String defaultMaxShutdownDelay;
    private Boolean enableLoggingForHeuristicReporting;
    private String heuristicRetryInterval;
    private Integer heuristicRetryWait;
    private String lpsHeuristicCompletion;
    private String propogatedOrBMTTranLifetimeTimeout;
    private Boolean recoverOnStartup;
    private Boolean timeoutGracePeriodEnabled;
    private String totalTranLifetimeTimeout;
    private String transactionLogDirectory;
    private Integer transactionLogSize;
    private Boolean waitForRecovery;
    private Boolean enableLogRetries;
    private String retriableSqlCodes;
    private String nonRetriableSqlCodes;
    private String recoveryIdentity;

    @XmlAttribute(name = "acceptHeuristicHazard")
    public void setAcceptHeuristicHazard(Boolean acceptHeuristicHazard) {
        this.acceptHeuristicHazard = acceptHeuristicHazard;
    }

    public Boolean getAcceptHeuristicHazard() {
        return this.acceptHeuristicHazard;
    }

    @XmlAttribute(name = "clientInactivityTimeout")
    public void setClientInactivityTimeout(String clientInactivityTimeout) {
        this.clientInactivityTimeout = clientInactivityTimeout;
    }

    public String getClientInactivityTimeout() {
        return this.clientInactivityTimeout;
    }

    @XmlAttribute(name = "defaultMaxShutdownDelay")
    public void setDefaultMaxShutdownDelay(String defaultMaxShutdownDelay) {
        this.defaultMaxShutdownDelay = defaultMaxShutdownDelay;
    }

    public String getDefaultMaxShutdownDelay() {
        return this.defaultMaxShutdownDelay;
    }

    @XmlAttribute(name = "enableLoggingForHeuristicReporting")
    public void setEnableLoggingForHeuristicReporting(Boolean enableLoggingForHeuristicReporting) {
        this.enableLoggingForHeuristicReporting = enableLoggingForHeuristicReporting;
    }

    public Boolean getEnableLoggingForHeuristicReporting() {
        return this.enableLoggingForHeuristicReporting;
    }

    @XmlAttribute(name = "heuristicRetryInterval")
    public void setHeuristicRetryInterval(String heuristicRetryInterval) {
        this.heuristicRetryInterval = heuristicRetryInterval;
    }

    public String getHeuristicRetryInterval() {
        return this.heuristicRetryInterval;
    }

    @XmlAttribute(name = "heuristicRetryWait")
    public void setHeuristicRetryWait(Integer heuristicRetryWait) {
        this.heuristicRetryWait = heuristicRetryWait;
    }

    public Integer getHeuristicRetryWait() {
        return this.heuristicRetryWait;
    }

    @XmlAttribute(name = "lpsHeuristicCompletion")
    public void setLpsHeuristicCompletion(String lpsHeuristicCompletion) {
        this.lpsHeuristicCompletion = lpsHeuristicCompletion;
    }

    public String getLpsHeuristicCompletion() {
        return this.lpsHeuristicCompletion;
    }

    @XmlAttribute(name = "propogatedOrBMTTranLifetimeTimeout")
    public void setPropogatedOrBMTTranLifetimeTimeout(String propogatedOrBMTTranLifetimeTimeout) {
        this.propogatedOrBMTTranLifetimeTimeout = propogatedOrBMTTranLifetimeTimeout;
    }

    public String getPropogatedOrBMTTranLifetimeTimeout() {
        return this.propogatedOrBMTTranLifetimeTimeout;
    }

    @XmlAttribute(name = "recoverOnStartup")
    public void setRecoverOnStartup(Boolean recoverOnStartup) {
        this.recoverOnStartup = recoverOnStartup;
    }

    public Boolean getRecoverOnStartup() {
        return this.recoverOnStartup;
    }

    @XmlAttribute(name = "timeoutGracePeriodEnabled")
    public void setTimeoutGracePeriodEnabled(Boolean timeoutGracePeriodEnabled) {
        this.timeoutGracePeriodEnabled = timeoutGracePeriodEnabled;
    }

    public Boolean getTimeoutGracePeriodEnabled() {
        return this.timeoutGracePeriodEnabled;
    }

    @XmlAttribute(name = "totalTranLifetimeTimeout")
    public void setTotalTranLifetimeTimeout(String totalTranLifetimeTimeout) {
        this.totalTranLifetimeTimeout = totalTranLifetimeTimeout;
    }

    public String getTotalTranLifetimeTimeout() {
        return this.totalTranLifetimeTimeout;
    }

    @XmlAttribute(name = "transactionLogDirectory")
    public void setTransactionLogDirectory(String transactionLogDirectory) {
        this.transactionLogDirectory = transactionLogDirectory;
    }

    public String getTransactionLogDirectory() {
        return this.transactionLogDirectory;
    }

    @XmlAttribute(name = "transactionLogSize")
    public void setTransactionLogSize(Integer transactionLogSize) {
        this.transactionLogSize = transactionLogSize;
    }

    public Integer getTransactionLogSize() {
        return this.transactionLogSize;
    }

    @XmlAttribute(name = "waitForRecovery")
    public void setWaitForRecovery(Boolean waitForRecovery) {
        this.waitForRecovery = waitForRecovery;
    }

    public Boolean getWaitForRecovery() {
        return this.waitForRecovery;
    }

    @XmlAttribute(name = "enableLogRetries")
    public void setEnableLogRetries(Boolean enableLogRetries) {
        this.enableLogRetries = enableLogRetries;
    }

    public Boolean getEnableLogRetries() {
        return this.enableLogRetries;
    }

    @XmlAttribute(name = "retriableSqlCodes")
    public void setRetriableSqlCodes(String retriableSqlCodes) {
        this.retriableSqlCodes = retriableSqlCodes;
    }

    public String getRetriableSqlCodes() {
        return this.retriableSqlCodes;
    }

    @XmlAttribute(name = "nonRetriableSqlCodes")
    public void setNonRetriableSqlCodes(String nonRetriableSqlCodes) {
        this.nonRetriableSqlCodes = nonRetriableSqlCodes;
    }

    public String getNonRetriableSqlCodes() {
        return this.nonRetriableSqlCodes;
    }

    @XmlAttribute(name = "recoveryIdentity")
    public void setRecoveryIdentity(String recoveryIdentity) {
        this.recoveryIdentity = recoveryIdentity;
    }

    public String getRecoveryIdentity() {
        return this.recoveryIdentity;
    }

    /**
     * Returns a String listing the properties and their values used on this
     * transaction element.
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("Transaction{");
        if (acceptHeuristicHazard != null)
            buf.append("acceptHeuristicHazard=\"" + acceptHeuristicHazard + "\" ");
        if (clientInactivityTimeout != null)
            buf.append("clientInactivityTimeout=\"" + clientInactivityTimeout + "\" ");
        if (defaultMaxShutdownDelay != null)
            buf.append("defaultMaxShutdownDelay=\"" + defaultMaxShutdownDelay + "\" ");
        if (enableLoggingForHeuristicReporting != null)
            buf.append("enableLoggingForHeuristicReporting=\"" + enableLoggingForHeuristicReporting + "\" ");
        if (heuristicRetryInterval != null)
            buf.append("heuristicRetryInterval=\"" + heuristicRetryInterval + "\" ");
        if (heuristicRetryWait != null)
            buf.append("heuristicRetryWait=\"" + heuristicRetryWait + "\" ");
        if (lpsHeuristicCompletion != null)
            buf.append("lpsHeuristicCompletion=\"" + lpsHeuristicCompletion + "\" ");
        if (propogatedOrBMTTranLifetimeTimeout != null)
            buf.append("propogatedOrBMTTranLifetimeTimeout=\"" + propogatedOrBMTTranLifetimeTimeout + "\" ");
        if (recoverOnStartup != null)
            buf.append("recoverOnStartup=\"" + recoverOnStartup + "\" ");
        if (timeoutGracePeriodEnabled != null)
            buf.append("timeoutGracePeriodEnabled=\"" + timeoutGracePeriodEnabled + "\" ");
        if (totalTranLifetimeTimeout != null)
            buf.append("totalTranLifetimeTimeout=\"" + totalTranLifetimeTimeout + "\" ");
        if (transactionLogDirectory != null)
            buf.append("transactionLogDirectory=\"" + transactionLogDirectory + "\" ");
        if (transactionLogSize != null)
            buf.append("transactionLogSize=\"" + transactionLogSize + "\" ");
        if (waitForRecovery != null)
            buf.append("waitForRecovery=\"" + waitForRecovery + "\" ");
        if (enableLogRetries != null)
            buf.append("enableLogRetries=\"" + enableLogRetries + "\" ");
        if (retriableSqlCodes != null)
            buf.append("retriableSqlCodes=\"" + retriableSqlCodes + "\" ");
        if (nonRetriableSqlCodes != null)
            buf.append("nonRetriableSqlCodes=\"" + nonRetriableSqlCodes + "\" ");
        if (recoveryIdentity != null)
            buf.append("recoveryIdentity=\"" + recoveryIdentity + "\" ");
        buf.append("}");
        return buf.toString();
    }

}