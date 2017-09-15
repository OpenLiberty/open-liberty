/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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

public class CouchDBElement extends ConfigElement {
    private String jndiName, libraryRef, host, username, password, connectionTimeout, socketTimeout;
    private Boolean enableSSL, relaxedSSLSettings, caching, useExpectContinue, cleanupIdleConnections;
    private Integer port, maxConnections, maxCacheEntries, maxObjectSizeBytes;

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    @XmlAttribute
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return enableSSL
     */
    public Boolean getEnableSSL() {
        return enableSSL;
    }

    /**
     * @param enableSSL the enableSSL to set
     */
    @XmlAttribute
    public void setEnableSSL(Boolean enableSSL) {
        this.enableSSL = enableSSL;
    }

    /**
     * @return relaxedSSLSettings
     */
    public Boolean getRelaxedSSLSettings() {
        return relaxedSSLSettings;
    }

    /**
     * @param relaxedSSLSettings the relaxedSSLSettings to set
     */
    @XmlAttribute
    public void setRelaxedSSLSettings(Boolean relaxedSSLSettings) {
        this.relaxedSSLSettings = relaxedSSLSettings;
    }

    /**
     * @return caching
     */
    public Boolean getCaching() {
        return caching;
    }

    /**
     * @param caching the caching to set
     */
    @XmlAttribute
    public void setCaching(Boolean caching) {
        this.caching = caching;
    }

    /**
     * @return useExpectContinue
     */
    public Boolean getUseExpectContinue() {
        return useExpectContinue;
    }

    /**
     * @param useExpectContinue the useExpectContinue to set
     */
    @XmlAttribute
    public void setUseExpectContinue(Boolean useExpectContinue) {
        this.useExpectContinue = useExpectContinue;
    }

    /**
     * @return cleanupIdleConnections
     */
    public Boolean getCleanupIdleConnections() {
        return cleanupIdleConnections;
    }

    /**
     * @param cleanupIdleConnections the cleanupIdleConnections to set
     */
    @XmlAttribute
    public void setCleanupIdleConnections(Boolean cleanupIdleConnections) {
        this.cleanupIdleConnections = cleanupIdleConnections;
    }

    /**
     * @return the jndiName
     */
    public String getJndiName() {
        return jndiName;
    }

    /**
     * @param jndiName the jndiName to set
     */
    @XmlAttribute
    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    /**
     * @return the libraryRef
     */
    public String getLibraryRef() {
        return libraryRef;
    }

    /**
     * @param libraryRef the libraryRef to set
     */
    @XmlAttribute
    public void setLibraryRef(String libraryRef) {
        this.libraryRef = libraryRef;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    @XmlAttribute
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param user the username to set
     */
    @XmlAttribute
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the connectTimeout
     */
    public String getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * @param connectTimeout the connectTimeout to set
     */
    @XmlAttribute
    public void setConnectionTimeout(String connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * @return the socketTimeout
     */
    public String getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * @param socketTimeout the socketTimeout to set
     */
    @XmlAttribute
    public void setSocketTimeout(String socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    /**
     * @return the port
     */
    public Integer getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    @XmlAttribute
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * @return the maxConnections
     */
    public Integer getMaxConnections() {
        return maxConnections;
    }

    /**
     * @param maxConnections the maxConnections to set
     */
    @XmlAttribute
    public void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }

    /**
     * @return the maxCacheEntries
     */
    public Integer getMaxCacheEntries() {
        return maxCacheEntries;
    }

    /**
     * @param maxCacheEntries the maxCacheEntries to set
     */
    @XmlAttribute
    public void setMaxCacheEntries(Integer maxCacheEntries) {
        this.maxCacheEntries = maxCacheEntries;
    }

    /**
     * @return the maxObjectSizeBytes
     */
    public Integer getMaxObjectSizeBytes() {
        return maxObjectSizeBytes;
    }

    /**
     * @param maxObjectSizeBytes the maxObjectSizeBytes to set
     */
    @XmlAttribute
    public void setMaxObjectSizeBytes(Integer maxObjectSizeBytes) {
        this.maxObjectSizeBytes = maxObjectSizeBytes;
    }

}
