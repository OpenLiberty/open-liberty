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
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Lists data source properties. Some properties are generic, others are driver specifc.
 */
public class Transaction extends ConfigElement {
    private Boolean acceptHeuristicHazard;
    private Integer clientInactivityTimeout;
    private Integer defaultMaxShutdownDelay;
    private Boolean enableLoggingForHeuristicReporting;
    private Integer heuristicRetryInterval;
    private Integer heuristicRetryWait;
    private String lpsHeuristicCompletion;
    private Integer propogatedOrBMTTranLifetimeTimeout;
    private Boolean recoverOnStartup;
    private Boolean timeoutGracePeriodEnabled;
    private String totalTranLifetimeTimeout;
    private String transactionLogDirectory;
    private Integer transactionLogSize;
    private Boolean waitForRecovery;

    @XmlAttribute(name = "acceptHeuristicHazard")
    public void setAcceptHeuristicHazard(Boolean acceptHeuristicHazard) {
        this.acceptHeuristicHazard = acceptHeuristicHazard;
    }

    public Boolean getAcceptHeuristicHazard() {
        return this.acceptHeuristicHazard;
    }

    @XmlAttribute(name = "clientInactivityTimeout")
    public void setClientInactivityTimeout(Integer clientInactivityTimeout) {
        this.clientInactivityTimeout = clientInactivityTimeout;
    }

    public Integer getClientInactivityTimeout() {
        return this.clientInactivityTimeout;
    }

    @XmlAttribute(name = "defaultMaxShutdownDelay")
    public void setDefaultMaxShutdownDelay(Integer defaultMaxShutdownDelay) {
        this.defaultMaxShutdownDelay = defaultMaxShutdownDelay;
    }

    public Integer getDefaultMaxShutdownDelay() {
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
    public void setHeuristicRetryInterval(Integer heuristicRetryInterval) {
        this.heuristicRetryInterval = heuristicRetryInterval;
    }

    public Integer getHeuristicRetryInterval() {
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
    public void setPropogatedOrBMTTranLifetimeTimeout(Integer propogatedOrBMTTranLifetimeTimeout) {
        this.propogatedOrBMTTranLifetimeTimeout = propogatedOrBMTTranLifetimeTimeout;
    }

    public Integer getPropogatedOrBMTTranLifetimeTimeout() {
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
        buf.append("}");
        return buf.toString();
    }
}