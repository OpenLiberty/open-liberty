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
package com.ibm.ws.openapi31;

import java.util.ArrayList;
import java.util.List;

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
    private List<Server> customServers = null;

    public ServerInfo(ServerInfo serverInfo) {
        this.host = serverInfo.host;
        this.httpPort = serverInfo.httpPort;
        this.httpsPort = serverInfo.httpsPort;
        this.customServers = serverInfo.customServers;
    }

    public List<Server> setCustomServers(List<Server> servers) {
        return this.customServers = servers;
    }

    public List<Server> getCustomServers() {
        return this.customServers;
    }

    public ServerInfo(String host, int httpPort, int httpsPort) {
        this.host = host;
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
    }

    public String getHost() {
        return this.host;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public String getHTTPServerURL() {
        return this.httpPort < 1 ? null : "http://" + this.host + ":" + this.httpPort;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    public String getHTTPSServerURL() {
        return this.httpsPort < 1 ? null : "https://" + this.host + ":" + this.httpsPort;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public void setHttpsPort(int secureHttpPort) {
        this.httpsPort = secureHttpPort;
    }

    @Override
    public String toString() {
        return "ServerInfo [host=" + this.host + ", httpPort=" + this.httpPort + ", httpsPort="
               + this.httpsPort + "]";
    }

    public void updateServers(OpenAPI openAPI) {
        if (OpenAPIUtils.isDebugEnabled(tc)) {
            String serversString = OpenAPIUtils.stringify(openAPI.getServers());
            Tr.debug(this, tc, "Updating the servers list. Old value: \n" + serversString);
        }
        if (this.customServers != null && !this.customServers.isEmpty()) {
            openAPI.setServers(customServers);
            if (OpenAPIUtils.isDebugEnabled(tc)) {
                String serversString = OpenAPIUtils.stringify(openAPI.getServers());
                Tr.debug(this, tc, "Updated the servers list. New value: \n" + serversString);
            }
            return;
        }

        openAPI.setServers(new ArrayList<>());
        if (getHTTPServerURL() != null) {
            openAPI.addServer(new ServerImpl().url(getHTTPServerURL()));
        }
        if (getHTTPSServerURL() != null) {
            openAPI.addServer(new ServerImpl().url(getHTTPSServerURL()));
        }

        if (OpenAPIUtils.isDebugEnabled(tc)) {
            String serversString = OpenAPIUtils.stringify(openAPI.getServers());
            Tr.debug(this, tc, "Updated the servers list. New value: \n" + serversString);
        }
    }
}
