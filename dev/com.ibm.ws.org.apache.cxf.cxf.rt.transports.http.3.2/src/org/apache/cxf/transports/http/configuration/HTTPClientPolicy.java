/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.apache.cxf.transports.http.configuration;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

/**
 * This is a generated class from CXF. Between the 3.1.X and 3.4.X streams certain
 * methods changed from using {@code int} to {@code java.lang.Integer} which breaks
 * at runtime if the binaries of the callers were compiled against the wrong version.
 * So this file is overlayed to pull in the latest changes to allow the older code to
 * work with the newer.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HTTPClientPolicy")
public class HTTPClientPolicy {
    @XmlAttribute(name = "ConnectionTimeout")
    protected Long connectionTimeout;
    @XmlAttribute(name = "ReceiveTimeout")
    protected Long receiveTimeout;
    @XmlAttribute(name = "ConnectionRequestTimeout")
    protected Long connectionRequestTimeout;
    @XmlAttribute(name = "AsyncExecuteTimeout")
    protected Long asyncExecuteTimeout;
    @XmlAttribute(name = "AsyncExecuteTimeoutRejection")
    protected Boolean asyncExecuteTimeoutRejection;
    @XmlAttribute(name = "AutoRedirect")
    protected Boolean autoRedirect;
    @XmlAttribute(name = "MaxRetransmits")
    protected Integer maxRetransmits;
    @XmlAttribute(name = "AllowChunking")
    protected Boolean allowChunking;
    @XmlAttribute(name = "ChunkingThreshold")
    protected Integer chunkingThreshold;
    @XmlAttribute(name = "ChunkLength")
    protected Integer chunkLength;
    @XmlAttribute(name = "Accept")
    protected String accept;
    @XmlAttribute(name = "AcceptLanguage")
    protected String acceptLanguage;
    @XmlAttribute(name = "AcceptEncoding")
    protected String acceptEncoding;
    @XmlAttribute(name = "ContentType")
    protected String contentType;
    @XmlAttribute(name = "Host")
    protected String host;
    @XmlAttribute(name = "Connection")
    protected ConnectionType connection;
    @XmlAttribute(name = "CacheControl")
    protected String cacheControl;
    @XmlAttribute(name = "Cookie")
    protected String cookie;
    @XmlAttribute(name = "BrowserType")
    protected String browserType;
    @XmlAttribute(name = "Referer")
    protected String referer;
    @XmlAttribute(name = "DecoupledEndpoint")
    protected String decoupledEndpoint;
    @XmlAttribute(name = "ProxyServer")
    protected String proxyServer;
    @XmlAttribute(name = "ProxyServerPort")
    protected Integer proxyServerPort;
    @XmlAttribute(name = "NonProxyHosts")
    protected String nonProxyHosts;
    @XmlAttribute(name = "ProxyServerType")
    protected ProxyServerType proxyServerType;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap();
    @XmlTransient
    protected PropertyChangeSupport propertyListener = new PropertyChangeSupport(this);

    public String getAccept() {
        return this.accept;
    }

    public void setAccept(String value) {
        this.propertyListener.firePropertyChange("accept", this.accept, value);
        this.accept = value;
    }

    public boolean isSetAccept() {
        return (this.accept != null);
    }

    public String getAcceptLanguage() {
        return this.acceptLanguage;
    }

    public void setAcceptLanguage(String value) {
        this.propertyListener.firePropertyChange("acceptLanguage", this.acceptLanguage, value);
        this.acceptLanguage = value;
    }

    public boolean isSetAcceptLanguage() {
        return (this.acceptLanguage != null);
    }

    public String getAcceptEncoding() {
        return this.acceptEncoding;
    }

    public void setAcceptEncoding(String value) {
        this.propertyListener.firePropertyChange("acceptEncoding", this.acceptEncoding, value);
        this.acceptEncoding = value;
    }

    public boolean isSetAcceptEncoding() {
        return (this.acceptEncoding != null);
    }

    public String getContentType() {
        return this.contentType;
    }

    public void setContentType(String value) {
        this.propertyListener.firePropertyChange("contentType", this.contentType, value);
        this.contentType = value;
    }

    public boolean isSetContentType() {
        return (this.contentType != null);
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(String value) {
        this.propertyListener.firePropertyChange("host", this.host, value);
        this.host = value;
    }

    public boolean isSetHost() {
        return (this.host != null);
    }

    public void setConnection(ConnectionType value) {
        this.propertyListener.firePropertyChange("connection", this.connection, value);
        this.connection = value;
    }

    public boolean isSetConnection() {
        return (this.connection != null);
    }

    public String getCacheControl() {
        return this.cacheControl;
    }

    public void setCacheControl(String value) {
        this.propertyListener.firePropertyChange("cacheControl", this.cacheControl, value);
        this.cacheControl = value;
    }

    public boolean isSetCacheControl() {
        return (this.cacheControl != null);
    }

    public String getCookie() {
        return this.cookie;
    }

    public void setCookie(String value) {
        this.propertyListener.firePropertyChange("cookie", this.cookie, value);
        this.cookie = value;
    }

    public boolean isSetCookie() {
        return (this.cookie != null);
    }

    public String getBrowserType() {
        return this.browserType;
    }

    public void setBrowserType(String value) {
        this.propertyListener.firePropertyChange("browserType", this.browserType, value);
        this.browserType = value;
    }

    public boolean isSetBrowserType() {
        return (this.browserType != null);
    }

    public String getReferer() {
        return this.referer;
    }

    public void setReferer(String value) {
        this.propertyListener.firePropertyChange("referer", this.referer, value);
        this.referer = value;
    }

    public boolean isSetReferer() {
        return (this.referer != null);
    }

    public String getDecoupledEndpoint() {
        return this.decoupledEndpoint;
    }

    public void setDecoupledEndpoint(String value) {
        this.propertyListener.firePropertyChange("decoupledEndpoint", this.decoupledEndpoint, value);
        this.decoupledEndpoint = value;
    }

    public boolean isSetDecoupledEndpoint() {
        return (this.decoupledEndpoint != null);
    }

    public String getProxyServer() {
        return this.proxyServer;
    }

    public void setProxyServer(String value) {
        this.propertyListener.firePropertyChange("proxyServer", this.proxyServer, value);
        this.proxyServer = value;
    }

    public boolean isSetProxyServer() {
        return (this.proxyServer != null);
    }

    public Integer getProxyServerPort() {
        return this.proxyServerPort;
    }

    public void setProxyServerPort(Integer value) {
        this.propertyListener.firePropertyChange("proxyServerPort", this.proxyServerPort, value);
        this.proxyServerPort = value;
    }

    public void setProxyServerPort(int value) {
        this.propertyListener.firePropertyChange("proxyServerPort", proxyServerPort == null ? -1 : proxyServerPort.intValue(), value);
        this.proxyServerPort = value;
    }

    public boolean isSetProxyServerPort() {
        return (this.proxyServerPort != null);
    }

    public String getNonProxyHosts() {
        return this.nonProxyHosts;
    }

    public void setNonProxyHosts(String value) {
        this.propertyListener.firePropertyChange("nonProxyHosts", this.nonProxyHosts, value);
        this.nonProxyHosts = value;
    }

    public boolean isSetNonProxyHosts() {
        return (this.nonProxyHosts != null);
    }

    public void setProxyServerType(ProxyServerType value) {
        this.propertyListener.firePropertyChange("proxyServerType", this.proxyServerType, value);
        this.proxyServerType = value;
    }

    public boolean isSetProxyServerType() {
        return (this.proxyServerType != null);
    }

    public Map<QName, String> getOtherAttributes() {
        return this.otherAttributes;
    }

    public void setConnectionTimeout(long value) {
        this.propertyListener.firePropertyChange("connectionTimeout", this.connectionTimeout, Long.valueOf(value));
        this.connectionTimeout = Long.valueOf(value);
    }

    public void unsetConnectionTimeout() {
        this.connectionTimeout = null;
    }

    public boolean isSetConnectionTimeout() {
        return (this.connectionTimeout != null);
    }

    public long getConnectionTimeout() {
        if (null == this.connectionTimeout) {
            return 30000L;
        }
        return this.connectionTimeout.longValue();
    }

    public void setReceiveTimeout(long value) {
        this.propertyListener.firePropertyChange("receiveTimeout", this.receiveTimeout, Long.valueOf(value));
        this.receiveTimeout = Long.valueOf(value);
    }

    public void unsetReceiveTimeout() {
        this.receiveTimeout = null;
    }

    public boolean isSetReceiveTimeout() {
        return (this.receiveTimeout != null);
    }

    public long getReceiveTimeout() {
        if (null == this.receiveTimeout) {
            return 60000L;
        }
        return this.receiveTimeout.longValue();
    }

    public void setConnectionRequestTimeout(long value) {
        this.propertyListener.firePropertyChange("connectionRequestTimeout", this.connectionRequestTimeout, Long.valueOf(value));
        this.connectionRequestTimeout = Long.valueOf(value);
    }

    public void unsetConnectionRequestTimeout() {
        this.connectionRequestTimeout = null;
    }

    public boolean isSetConnectionRequestTimeout() {
        return (this.connectionRequestTimeout != null);
    }

    public long getConnectionRequestTimeout() {
        if (null == this.connectionRequestTimeout) {
            return 60000L;
        }
        return this.connectionRequestTimeout.longValue();
    }

    public void setAsyncExecuteTimeout(long value) {
        this.propertyListener.firePropertyChange("asyncExecuteTimeout", this.asyncExecuteTimeout, Long.valueOf(value));
        this.asyncExecuteTimeout = Long.valueOf(value);
    }

    public void unsetAsyncExecuteTimeout() {
        this.asyncExecuteTimeout = null;
    }

    public boolean isSetAsyncExecuteTimeout() {
        return (this.asyncExecuteTimeout != null);
    }

    public long getAsyncExecuteTimeout() {
        if (null == this.asyncExecuteTimeout) {
            return 5000L;
        }
        return this.asyncExecuteTimeout.longValue();
    }

    public void setAsyncExecuteTimeoutRejection(boolean value) {
        this.propertyListener.firePropertyChange("asyncExecuteTimeoutRejection", this.asyncExecuteTimeoutRejection, Boolean.valueOf(value));
        this.asyncExecuteTimeoutRejection = Boolean.valueOf(value);
    }

    public void unsetAsyncExecuteTimeoutRejection() {
        this.asyncExecuteTimeoutRejection = null;
    }

    public boolean isSetAsyncExecuteTimeoutRejection() {
        return (this.asyncExecuteTimeoutRejection != null);
    }

    public boolean isAsyncExecuteTimeoutRejection() {
        if (null == this.asyncExecuteTimeoutRejection) {
            return false;
        }
        return this.asyncExecuteTimeoutRejection.booleanValue();
    }

    public void setAutoRedirect(boolean value) {
        this.propertyListener.firePropertyChange("autoRedirect", this.autoRedirect, Boolean.valueOf(value));
        this.autoRedirect = Boolean.valueOf(value);
    }

    public void unsetAutoRedirect() {
        this.autoRedirect = null;
    }

    public boolean isSetAutoRedirect() {
        return (this.autoRedirect != null);
    }

    public boolean isAutoRedirect() {
        if (null == this.autoRedirect) {
            return false;
        }
        return this.autoRedirect.booleanValue();
    }

    public void setMaxRetransmits(int value) {
        this.propertyListener.firePropertyChange("maxRetransmits", this.maxRetransmits, Integer.valueOf(value));
        this.maxRetransmits = Integer.valueOf(value);
    }

    public void unsetMaxRetransmits() {
        this.maxRetransmits = null;
    }

    public boolean isSetMaxRetransmits() {
        return (this.maxRetransmits != null);
    }

    public int getMaxRetransmits() {
        if (null == this.maxRetransmits) {
            return -1;
        }
        return this.maxRetransmits.intValue();
    }

    public void setAllowChunking(boolean value) {
        this.propertyListener.firePropertyChange("allowChunking", this.allowChunking, Boolean.valueOf(value));
        this.allowChunking = Boolean.valueOf(value);
    }

    public void unsetAllowChunking() {
        this.allowChunking = null;
    }

    public boolean isSetAllowChunking() {
        return (this.allowChunking != null);
    }

    public boolean isAllowChunking() {
        if (null == this.allowChunking) {
            return true;
        }
        return this.allowChunking.booleanValue();
    }

    public void setChunkingThreshold(int value) {
        this.propertyListener.firePropertyChange("chunkingThreshold", this.chunkingThreshold, Integer.valueOf(value));
        this.chunkingThreshold = Integer.valueOf(value);
    }

    public void unsetChunkingThreshold() {
        this.chunkingThreshold = null;
    }

    public boolean isSetChunkingThreshold() {
        return (this.chunkingThreshold != null);
    }

    public int getChunkingThreshold() {
        if (null == this.chunkingThreshold) {
            return 4096;
        }
        return this.chunkingThreshold.intValue();
    }

    public void setChunkLength(int value) {
        this.propertyListener.firePropertyChange("chunkLength", this.chunkLength, Integer.valueOf(value));
        this.chunkLength = Integer.valueOf(value);
    }

    public void unsetChunkLength() {
        this.chunkLength = null;
    }

    public boolean isSetChunkLength() {
        return (this.chunkLength != null);
    }

    public int getChunkLength() {
        if (null == this.chunkLength) {
            return -1;
        }
        return this.chunkLength.intValue();
    }

    public ConnectionType getConnection() {
        if (null == this.connection) {
            return ConnectionType.fromValue("Keep-Alive");
        }
        return this.connection;
    }

    public ProxyServerType getProxyServerType() {
        if (null == this.proxyServerType) {
            return ProxyServerType.fromValue("HTTP");
        }
        return this.proxyServerType;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.propertyListener.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.propertyListener.removePropertyChangeListener(listener);
    }
}
