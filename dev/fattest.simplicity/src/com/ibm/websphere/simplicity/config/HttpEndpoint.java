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
import javax.xml.bind.annotation.XmlElement;

/**
 * Defines an HTTP endpoint (host/port mapping)
 *
 * @author Tim Burns
 *
 */
public class HttpEndpoint extends ConfigElement {

    @XmlElement(name = "tcpOptions")
    private TcpOptions tcpOptions;
    @XmlElement(name = "sslOptions")
    private SslOptions sslOptions;
    private String host;
    private String httpPort;
    private String httpsPort;
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
    @XmlAttribute
    public void setHost(String host) {
        this.host = ConfigElement.getValue(host);
    }

    /**
     * @return the port to use for non-secure traffic
     */
    public String getHttpPort() {
        return this.httpPort;
    }

    /**
     * @param httpPort the port to use for non-secure traffic
     */
    @XmlAttribute
    public void setHttpPort(String httpPort) {
        this.httpPort = httpPort;
    }

    /**
     * @return the port to use for secure traffic
     */
    public String getHttpsPort() {
        return this.httpsPort;
    }

    /**
     * @param httpsPort the port to use for secure traffic
     */
    @XmlAttribute
    public void setHttpsPort(String httpsPort) {
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
    @XmlAttribute
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

        buf.append("}");
        return buf.toString();
    }
}
