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

package com.ibm.websphere.simplicity.config.wim;

import javax.xml.bind.annotation.XmlAttribute;

import com.ibm.websphere.simplicity.config.ConfigElement;

/**
 * Configuration for the following nested elements:
 *
 * <ul>
 * <li>ldapRegistry --> failoverServers --> server</li>
 * </ul>
 */
public class Server extends ConfigElement {

    private String host;
    private String port; // Integer in metatype, but need to support properties.

    public Server() {}

    public Server(String host, String port) {
        this.host = host;
        this.port = port;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @return the port
     */
    public String getPort() {
        return port;
    }

    /**
     * @param host the host to set
     */
    @XmlAttribute(name = "host")
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @param port the port to set
     */
    @XmlAttribute(name = "port")
    public void setPort(String port) {
        this.port = port;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (host != null) {
            sb.append("host=\"").append(host).append("\" ");;
        }
        if (port != null) {
            sb.append("port=\"").append(port).append("\" ");;
        }

        sb.append("}");

        return sb.toString();
    }
}