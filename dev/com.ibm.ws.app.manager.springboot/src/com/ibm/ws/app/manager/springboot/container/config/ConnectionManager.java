/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.container.config;

/**
 * A connectionManager element for holding a set of properties
 * relating to a connection manager;
 */
public class ConnectionManager extends ConfigElement {

    public final static String XML_ATTRIBUTE_NAME_AGED_TIMEOUT = "agedTimeout";
    private String agedTimeout;

    public final static String XML_ATTRIBUTE_NAME_CONNECTION_TIMEOUT = "connectionTimeout";
    private String connectionTimeout;

    public final static String XML_ATTRIBUTE_NAME_MAX_IDLE_TIME = "maxIdleTime";
    private String maxIdleTime;

    public final static String XML_ATTRIBUTE_NAME_MAX_POOL_SIZE = "maxPoolSize";
    private String maxPoolSize;

    public final static String XML_ATTRIBUTE_NAME_MIN_POOL_SIZE = "minPoolSize";
    private String minPoolSize;

    public final static String XML_ATTRIBUTE_NAME_PURGE_POLICY = "purgePolicy";
    private String purgePolicy;

    public final static String XML_ATTRIBUTE_NAME_REAP_TIME = "reapTime";
    private String reapTime;

    public final static String XML_ATTRIBUTE_NAME_MAX_CONNECTIONS_PER_THREAD = "maxConnectionsPerThread";
    private String maxConnectionsPerThread;

    public final static String XML_ATTRIBUTE_NAME_NUM_CONNECTIONS_PER_THREAD_LOCAL = "numConnectionsPerThreadLocal";
    private String numConnectionsPerThreadLocal;

    public final static String XML_ATTRIBUTE_NAME_MAX_RESERVE_POOL_CONNECTIONS_ALLOWED = "maxReservePoolConnectionsAllowed";
    private String maxReservePoolConnectionsAllowed;

    public final static String XML_ATTRIBUTE_NAME_NUM_OF_RESERVE_POOLS = "numberOfReservePools";
    private String numberOfReservePools;

    public final static String XML_ATTRIBUTE_NAME_STUCK_THRESHOLD = "stuckThreshold";
    private String stuckThreshold;

    public final static String XML_ATTRIBUTE_NAME_STUCK_TIMER_TIME = "stuckTimerTime";
    private String stuckTimerTime;

    public final static String XML_ATTRIBUTE_NAME_STUCK_TIME = "stuckTime";
    private String stuckTime;

    public final static String XML_ATTRIBUTE_NAME_SURGE_CREATION_INTERVAL = "surgeCreationInterval";
    private String surgeCreationInterval;

    public final static String XML_ATTRIBUTE_NAME_SURGE_THRESHOLD = "surgeThreshold";
    private String surgeThreshold;

    // Not supported yet.
    public void setStuckThreshold(String stuckThreshold) {
        this.stuckThreshold = stuckThreshold;
    }

    // Not supported yet.
    public String getStuckThreshold() {
        return this.stuckThreshold;
    }

    // Not supported yet.
    public void setStuckTimerTime(String stuckTimerTime) {
        this.stuckTimerTime = stuckTimerTime;
    }

    // Not supported yet.
    public String getStuckTimerTime() {
        return this.stuckTimerTime;
    }

    // Not supported yet.
    public void setStuckTime(String stuckTime) {
        this.stuckTime = stuckTime;
    }

    // Not supported yet.
    public String getStuckTime() {
        return this.stuckTime;
    }

    // Not supported yet.
    public void setSurgeCreationInterval(String surgeCreationInterval) {
        this.surgeCreationInterval = surgeCreationInterval;
    }

    // Not supported yet.
    public String getSurgeCreationInterval() {
        return this.surgeCreationInterval;
    }

    // Not supported yet.
    public void setSurgeThreshold(String surgeThreshold) {
        this.surgeThreshold = surgeThreshold;
    }

    // Not supported yet.
    public String getSurgeThreshold() {
        return this.surgeThreshold;
    }

    // Not supported yet.
    public void setMaxReservePoolConnectionsAllowed(String maxReservePoolConnectionsAllowed) {
        this.maxReservePoolConnectionsAllowed = maxReservePoolConnectionsAllowed;
    }

    // Not supported yet.
    public String getMaxReservePoolConnectionsAllowed() {
        return this.maxReservePoolConnectionsAllowed;
    }

    // Not supported yet.
    public void setNumberOfReservePools(String numberOfReservePools) {
        this.numberOfReservePools = numberOfReservePools;
    }

    // Not supported yet.
    public String getNumberOfReservePools() {
        return this.numberOfReservePools;
    }

    public void setAgedTimeout(String agedTimeout) {
        this.agedTimeout = agedTimeout;
    }

    public String getAgedTimeout() {
        return agedTimeout;
    }

    public void setConnectionTimeout(String connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public String getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setMaxIdleTime(String maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    public String getMaxIdleTime() {
        return maxIdleTime;
    }

    public void setMaxPoolSize(String maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public String getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMinPoolSize(String minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public String getMinPoolSize() {
        return minPoolSize;
    }

    public void setPurgePolicy(String purgePolicy) {
        this.purgePolicy = purgePolicy;
    }

    public String getPurgePolicy() {
        return purgePolicy;
    }

    public void setReapTime(String reapTime) {
        this.reapTime = reapTime;
    }

    public String getReapTime() {
        return reapTime;
    }

    public String getMaxConnectionsPerThread() {
        return maxConnectionsPerThread;
    }

    public void setMaxConnectionsPerThread(String maxConnectionsPerThread) {
        this.maxConnectionsPerThread = maxConnectionsPerThread;
    }

    public String getNumConnectionsPerThreadLocal() {
        return numConnectionsPerThreadLocal;
    }

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
