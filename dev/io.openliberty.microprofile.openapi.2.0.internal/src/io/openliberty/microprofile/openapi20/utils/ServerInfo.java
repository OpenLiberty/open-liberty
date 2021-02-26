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
package io.openliberty.microprofile.openapi20.utils;

/**
 *
 */
public class ServerInfo {

    private int httpPort = -1;
    private int httpsPort = -1;
    private String host;

    public ServerInfo() {
    }

    public ServerInfo(String host, int httpPort, int httpsPort) {
        this.host = host;
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
    }

    public ServerInfo(ServerInfo serverInfo) {
        this.host = serverInfo.host;
        this.httpPort = serverInfo.httpPort;
        this.httpsPort = serverInfo.httpsPort;
    }

    /**
     * @return the httpPort
     */
    public int getHttpPort() {
        return httpPort;
    }

    /**
     * @param httpPort the httpPort to set
     */
    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    /**
     * @return the httpsPort
     */
    public int getHttpsPort() {
        return httpsPort;
    }

    /**
     * @param httpsPort the httpsPort to set
     */
    public void setHttpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ServerInfo : [");
        builder.append("host=").append(this.host).append(", ");
        builder.append("httpPort=").append(this.httpPort).append(", ");
        builder.append("httpsPort=").append(this.httpsPort);
        builder.append("]");
        return builder.toString();
    }
}
