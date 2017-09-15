/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
 * A connectionManager element for holding a set of properties
 * relating to a connection manager;
 */
public class ConnectionManager extends ConfigElement {
    private String agedTimeout;
    private String connectionTimeout;
    private String maxIdleTime;
    private String maxPoolSize;
    private String minPoolSize;
    private String purgePolicy;
    private String reapTime;
    private String maxConnectionsPerThread;
    private String numConnectionsPerThreadLocal;
    private String maxReservePoolConnectionsAllowed;
    private String numberOfReservePools;
    private String stuckThreshold;
    private String stuckTimerTime;
    private String stuckTime;
    private String surgeCreationInterval;
    private String surgeThreshold;

    // Not supported yet.
    @XmlAttribute(name = "stuckThreshold")
    public void setStuckThreshold(String stuckThreshold) {
        this.stuckThreshold = stuckThreshold;
    }

    // Not supported yet.
    public String getStuckThreshold() {
        return this.stuckThreshold;
    }

    // Not supported yet.
    @XmlAttribute(name = "stuckTimerTime")
    public void setStuckTimerTime(String stuckTimerTime) {
        this.stuckTimerTime = stuckTimerTime;
    }

    // Not supported yet.
    public String getStuckTimerTime() {
        return this.stuckTimerTime;
    }

    // Not supported yet.
    @XmlAttribute(name = "stuckTime")
    public void setStuckTime(String stuckTime) {
        this.stuckTime = stuckTime;
    }

    // Not supported yet.
    public String getStuckTime() {
        return this.stuckTime;
    }

    // Not supported yet.
    @XmlAttribute(name = "surgeCreationInterval")
    public void setSurgeCreationInterval(String surgeCreationInterval) {
        this.surgeCreationInterval = surgeCreationInterval;
    }

    // Not supported yet.
    public String getSurgeCreationInterval() {
        return this.surgeCreationInterval;
    }

    // Not supported yet.
    @XmlAttribute(name = "surgeThreshold")
    public void setSurgeThreshold(String surgeThreshold) {
        this.surgeThreshold = surgeThreshold;
    }

    // Not supported yet.
    public String getSurgeThreshold() {
        return this.surgeThreshold;
    }

    // Not supported yet.
    @XmlAttribute(name = "maxReservePoolConnectionsAllowed")
    public void setMaxReservePoolConnectionsAllowed(String maxReservePoolConnectionsAllowed) {
        this.maxReservePoolConnectionsAllowed = maxReservePoolConnectionsAllowed;
    }

    // Not supported yet.
    public String getMaxReservePoolConnectionsAllowed() {
        return this.maxReservePoolConnectionsAllowed;
    }

    // Not supported yet.
    @XmlAttribute(name = "numberOfReservePools")
    public void setNumberOfReservePools(String numberOfReservePools) {
        this.numberOfReservePools = numberOfReservePools;
    }

    // Not supported yet.
    public String getNumberOfReservePools() {
        return this.numberOfReservePools;
    }

    @XmlAttribute
    public void setAgedTimeout(String agedTimeout) {
        this.agedTimeout = agedTimeout;
    }

    public String getAgedTimeout() {
        return agedTimeout;
    }

    @XmlAttribute
    public void setConnectionTimeout(String connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public String getConnectionTimeout() {
        return connectionTimeout;
    }

    @XmlAttribute
    public void setMaxIdleTime(String maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    public String getMaxIdleTime() {
        return maxIdleTime;
    }

    @XmlAttribute
    public void setMaxPoolSize(String maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public String getMaxPoolSize() {
        return maxPoolSize;
    }

    @XmlAttribute
    public void setMinPoolSize(String minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public String getMinPoolSize() {
        return minPoolSize;
    }

    @XmlAttribute
    public void setPurgePolicy(String purgePolicy) {
        this.purgePolicy = purgePolicy;
    }

    public String getPurgePolicy() {
        return purgePolicy;
    }

    @XmlAttribute
    public void setReapTime(String reapTime) {
        this.reapTime = reapTime;
    }

    public String getReapTime() {
        return reapTime;
    }

    public String getMaxConnectionsPerThread() {
        return maxConnectionsPerThread;
    }

    @XmlAttribute(name = "maxConnectionsPerThread")
    public void setMaxConnectionsPerThread(String maxConnectionsPerThread) {
        this.maxConnectionsPerThread = maxConnectionsPerThread;
    }

    public String getNumConnectionsPerThreadLocal() {
        return numConnectionsPerThreadLocal;
    }

    @XmlAttribute(name = "numConnectionsPerThreadLocal")
    public void setNumConnectionsPerThreadLocal(String numConnectionsPerThreadLocal) {
        this.numConnectionsPerThreadLocal = numConnectionsPerThreadLocal;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("ConnectionManager{");
        buf.append("id=\"" + (getId() == null ? "" : getId()) + "\" ");
        if (agedTimeout != null)
            buf.append("agedTimeout=\"" + agedTimeout + "\" ");
        if (connectionTimeout != null)
            buf.append("connectionTimeout=\"" + connectionTimeout + "\" ");
        if (maxIdleTime != null)
            buf.append("maxIdleTime=\"" + maxIdleTime + "\" ");
        if (maxPoolSize != null)
            buf.append("maxPoolSize=\"" + maxPoolSize + "\" ");
        if (minPoolSize != null)
            buf.append("minPoolSize=\"" + minPoolSize + "\" ");
        if (purgePolicy != null)
            buf.append("purgePolicy=\"" + purgePolicy + "\" ");
        if (reapTime != null)
            buf.append("reapTime=\"" + reapTime + "\" ");
        if (numConnectionsPerThreadLocal != null)
            buf.append("numConnectionsPerThreadLocal=\"" + numConnectionsPerThreadLocal + "\" ");
        if (maxConnectionsPerThread != null)
            buf.append("maxConnectionsPerThread=\"" + maxConnectionsPerThread + "\" ");
        if (maxReservePoolConnectionsAllowed != null)
            buf.append("maxReservePoolConnectionsAllowed=\"" + maxReservePoolConnectionsAllowed + "\" ");
        if (numberOfReservePools != null)
            buf.append("numberOfReservePools=\"" + numberOfReservePools + "\" ");
        if (stuckThreshold != null)
            buf.append("stuckThreshold=\"" + stuckThreshold + "\" ");
        if (stuckTimerTime != null)
            buf.append("stuckTimerTime=\"" + stuckTimerTime + "\" ");
        if (stuckTime != null)
            buf.append("stuckTime=\"" + stuckTime + "\" ");
        if (surgeCreationInterval != null)
            buf.append("surgeCreationInterval=\"" + surgeCreationInterval + "\" ");
        if (surgeThreshold != null)
            buf.append("surgeThreshold=\"" + surgeThreshold + "\" ");
        buf.append("}");

        return buf.toString();
    }
}
