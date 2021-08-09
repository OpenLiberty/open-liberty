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
 * KeyStore element is defined here:<br>
 * /com.ibm.ws.ssl/resources/OSGI-INF/metatype/metatype.xml
 */
public class OutboundConnection extends ConfigElement {

    public final static String XML_ATTRIBUTE_NAME_HOST = "host";
    private String host;

    public final static String XML_ATTRIBUTE_NAME_PORT = "port";
    private String port;

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host to set
     */

    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the port
     */
    public String getPort() {
        return port;
    }

    /**
     * @param port to set
     */
    public void setPort(String port) {
        this.port = port;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("OutboundConnection{");
        if (host != null)
            buf.append("host=\"" + host + "\" ");
        if (port != null)
            buf.append("port=\"" + port + "\" ");
        buf.append("}");
        return buf.toString();
    }

}