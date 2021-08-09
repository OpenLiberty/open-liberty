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
 * Defines an HTTP endpoint (host/port mapping)
 */
public class HttpEndpoint extends ConfigElement {

    public static final String XML_ELEMENT_NAME_TCP_OPTIONS = "tcpOptions";
    private TcpOptions tcpOptions;

    public static final String XML_ELEMENT_NAME_HTTP_OPTIONS = "httpOptions";
    private HttpOptions httpOptions;

    public static final String XML_ELEMENT_NAME_SSL_OPTIONS = "sslOptions";
    private SslOptions sslOptions;

    public static final String XML_ATTRIBUTE_NAME_HOST = "host";
    private String host;

    public static final String XML_ATTRIBUTE_NAME_HTTP_PORT = "httpPort";
    private Integer httpPort;

    public static final String XML_ATTRIBUTE_NAME_HTTPS_PORT = "httpsPort";
    private Integer httpsPort;

    public static final String XML_ATTRIBUTE_NAME_PROTOCOL_VERSION = "protocolVersion";
    private String protocolVersion;

    /**
     * @return TCP options for this configuration
     */
    public TcpOptions getTcpOptions() {
        if (this.tcpOptions == null) {
            this.tcpOptions = new TcpOptions();
        }
        return this.tcpOptions;
    }

    /**
     * @return HTTP options for this configuration
     */
    public HttpOptions getHttpOptions() {
        if (this.httpOptions == null) {
            this.httpOptions = new HttpOptions();
        }
        return this.httpOptions;
    }

    /**
     * @return Ssl options for this configuration
     */
    public SslOptions getSslOptions() {
        if (this.sslOptions == null) {
            this.sslOptions = new SslOptions();
        }
        return this.sslOptions;
    }

    /**
     * @return the host mapping for this entry
     */
    public String getHost() {
        return this.host;
    }

    /**
     * @param host the host mapping for this entry
     */
    public void setHost(String host) {
        this.host = ConfigElement.getValue(host);
    }

    /**
     * @return the port to use for non-secure traffic
     */
    public Integer getHttpPort() {
        return this.httpPort;
    }

    /**
     * @param httpPort the port to use for non-secure traffic
     */
    public void setHttpPort(Integer httpPort) {
        this.httpPort = httpPort;
    }

    /**
     * @return the port to use for secure traffic
     */
    public Integer getHttpsPort() {
        return this.httpsPort;
    }

    /**
     * @param httpsPort the port to use for secure traffic
     */
    public void setHttpsPort(Integer httpsPort) {
        this.httpsPort = httpsPort;
    }

    /**
     * @return the protocolVersion for this entry
     */
    public String getProtocolVersion() {
        return this.protocolVersion;
    }

    /**
     * @param protocolVersion for this entry
     */
    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = ConfigElement.getValue(protocolVersion);
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("HttpEndpoint{");
        buf.append("id=\"" + this.getId() + "\" ");
        if (this.host != null)
            buf.append("host=\"" + this.host + "\" ");
        if (this.httpPort != null)
            buf.append("httpPort=\"" + this.httpPort + "\" ");
        if (this.httpsPort != null)
            buf.append("httpsPort=\"" + this.httpsPort + "\" ");
        if (this.protocolVersion != null)
            buf.append("protocolVersion=\"" + this.protocolVersion + "\" ");
        if (this.tcpOptions != null)
            buf.append(tcpOptions.toString());
        if (this.sslOptions != null)
            buf.append(sslOptions.toString());
        if (this.httpOptions != null)
            buf.append(httpOptions.toString());

        buf.append("}");
        return buf.toString();
    }
}
