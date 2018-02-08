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
package com.ibm.ws.microprofile.openapi.utils;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.servers.Server;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.model.servers.ServerImpl;

/**
 *
 */
public class ServerInfo {
    private static final TraceComponent tc = Tr.register(ServerInfo.class);

    private int httpPort = -1;
    private int httpsPort = -1;
    private String host;
    private String applicationPath;

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

    /**
     * @return the applicationPath
     */
    public String getApplicationPath() {
        return applicationPath;
    }

    /**
     * @param applicationPath the applicationPath to set
     */
    public void setApplicationPath(String applicationPath) {
        this.applicationPath = applicationPath;
    }

    public void updateOpenAPIWithServers(OpenAPI openapi) {
        if (openapi.getServers() != null && openapi.getServers().size() > 0) {
            return;
        }
        if (httpPort > 0) {
            String url = "http://" + host + ":" + httpPort;
            if (applicationPath != null) {
                url += applicationPath;
            }
            Server server = new ServerImpl().url(url);
            openapi.addServer(server);
        }
        if (httpsPort > 0) {
            String secureUrl = "https://" + host + ":" + httpsPort;
            if (applicationPath != null) {
                secureUrl += applicationPath;
            }
            Server secureServer = new ServerImpl().url(secureUrl);
            openapi.addServer(secureServer);
        }
    }
}
