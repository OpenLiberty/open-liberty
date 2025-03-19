/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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
import javax.xml.bind.annotation.XmlElement;

/**
 * Defines an HTTP endpoint (host/port mapping)
 *
 * @author Tim Burns
 *
 */
public class HttpEndpoint extends ConfigElement {

    @XmlElement(name = "httpOptions")
    private HttpOptions httpOptions;
    @XmlElement(name = "tcpOptions")
    private TcpOptions tcpOptions;
    @XmlElement(name = "sslOptions")
    private SslOptions sslOptions;
    @XmlElement(name = "samesite")
    private SameSite sameSite;
    private String samesiteRef;
    @XmlElement(name = "headers")
    private Headers headers;
    private String headersRef;
    private String host;
    private String httpPort;
    private String httpsPort;
    private String protocolVersion;

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
     * return SameSite for this entry
     */
    public SameSite getSameSite() {
        if (this.sameSite == null) {
            this.sameSite = new SameSite();
        }
        return this.sameSite;
    }

    /**
     * @return the samesiteRef for this entry
     */
    public String getSameSiteRef() {
        return this.samesiteRef;
    }

    /**
     * 
     * @param samesiteRef The samesiteRef for this entry
     */
    @XmlAttribute
    public void setSameSiteRef(String samesiteRef) {
        this.samesiteRef = samesiteRef;
    }

    /**
     *
     * @return Headers for this entry
     */
    public Headers getHeaders() {
        if (this.headers == null) {
            this.headers = new Headers();
        }

        return this.headers;
    }

    /**
     *
     * @return The headersRef for this entry
     */
    public String getHeadersRef() {
        return this.headersRef;
    }

    /**
     *
     * @param headersRef The headersRef for this entry
     */
    @XmlAttribute
    public void setHeadersRef(String headersRef) {
        this.headersRef = headersRef;
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
        if (this.httpOptions != null)
            buf.append(httpOptions.toString());
        if (this.sameSite != null)
            buf.append(sameSite.toString());
        if (samesiteRef != null)
            buf.append("samesiteRef=\"" + samesiteRef + "\" ");

        buf.append("}");
        return buf.toString();
    }
}
