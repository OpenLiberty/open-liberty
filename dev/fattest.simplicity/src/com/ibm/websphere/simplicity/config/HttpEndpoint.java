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
    private String host;
    private Integer httpPort;
    private Integer httpsPort;

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
    public Integer getHttpPort() {
        return this.httpPort;
    }

    /**
     * @param httpPort the port to use for non-secure traffic
     */
    @XmlAttribute
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
    @XmlAttribute
    public void setHttpsPort(Integer httpsPort) {
        this.httpsPort = httpsPort;
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
        if (this.tcpOptions != null)
            buf.append(tcpOptions.toString());

        buf.append("}");
        return buf.toString();
    }
}
