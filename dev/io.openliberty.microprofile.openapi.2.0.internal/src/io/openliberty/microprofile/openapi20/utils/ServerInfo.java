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

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.servers.Server;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
public class ServerInfo {
    private static final TraceComponent tc = Tr.register(ServerInfo.class);

    private int httpPort = -1;
    private int httpsPort = -1;
    private String host;
    private String applicationPath;
    private boolean isUserServer = false;

    public ServerInfo() {

    }

    public ServerInfo(String host, int httpPort, int httpsPort, String applicationPath, boolean isUserServer) {
        this.host = host;
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
        this.applicationPath = applicationPath;
        this.isUserServer = isUserServer;
    }

    public ServerInfo(ServerInfo serverInfo) {
        this.host = serverInfo.host;
        this.httpPort = serverInfo.httpPort;
        this.httpsPort = serverInfo.httpsPort;
        this.applicationPath = serverInfo.applicationPath;
        this.isUserServer = serverInfo.isUserServer;
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

    /**
     * @return value to indicate whether the server information was set by user
     */
    public boolean getIsUserServer() {
        return isUserServer;
    }

    /**
     * @param isUserServer value to indicate whether the server information was set by user
     */
    public void setIsUserServer(boolean isUserServer) {
        this.isUserServer = isUserServer;
    }

    @Trivial
    public void updateOpenAPIWithServers(OpenAPI openapi) {
        if (isUserServer) {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(this, tc, "Server information was already set by the user. So not setting Liberty's server information");
            }
            return;
        }

        //Remove any servers added by Liberty previously
        openapi.setServers(null);

        if (httpPort > 0) {
            String port = httpPort == 80 ? "" : (":" + httpPort);
            String url = "http://" + host + port;
            if (applicationPath != null) {
                url += applicationPath;
            }
            Server server = OASFactory .createServer();
            server.setUrl(url);
            openapi.addServer(server);
        }
        if (httpsPort > 0) {
            String port = httpsPort == 443 ? "" : (":" + httpsPort);
            String secureUrl = "https://" + host + port;
            if (applicationPath != null) {
                secureUrl += applicationPath;
            }
            Server secureServer = OASFactory .createServer();
            secureServer.setUrl(secureUrl);
            openapi.addServer(secureServer);
        }
    }

    @Override
    public String toString() {
        return "ServerInfo [host=" + this.host + ", httpPort=" + this.httpPort + ", httpsPort="
               + this.httpsPort + ", applicationPath=" + this.applicationPath + ", isUserServer="
               + this.isUserServer + "]";
    }
}
