/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Utilities for testing HTTP connections.
 */
public class SecurityFatHttpUtils extends HttpUtils {

    protected static Class<?> thisClass = SecurityFatHttpUtils.class;

    /**
     * This method creates a connection to a webpage and then returns the connection, it doesn't care what the response code is.
     *
     * @param server
     *            The liberty server that is hosting the URL
     * @param path
     *            The path to the URL with the output to test (excluding port and server information). For instance
     *            "/someContextRoot/servlet1"
     * @return The connection to the http address
     */
    public static HttpURLConnection getHttpConnectionWithAnyResponseCode(LibertyServer server, String path) throws IOException {
        int timeout = DEFAULT_TIMEOUT;
        URL url = createURL(server, path);
        HttpURLConnection con = getHttpConnection(url, timeout, HTTPRequestMethod.GET);
        Log.info(SecurityFatHttpUtils.class, "getHttpConnection", "Connecting to " + url.toExternalForm() + " expecting http response in " + timeout + " seconds.");
        con.connect();
        return con;
    }

    public static URL createURL(LibertyServer server, String path) throws MalformedURLException {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return new URL("http://" + server.getHostname() + ":" + server.getBvtPort() + path);
    }

    public static void saveServerPorts(LibertyServer server, String propertyNameRoot) throws Exception {
        server.setBvtPortPropertyName(propertyNameRoot);
        server.setBvtSecurePortPropertyName(propertyNameRoot + ".secure");
        Log.info(thisClass, "saveServerPorts", server.getServerName() + " ports are: " + server.getBvtPort() + " " + server.getBvtSecurePort());

    }

    public static InetAddress getServerIdentity() throws UnknownHostException {
        InetAddress addr = InetAddress.getLocalHost();
        return addr;
    }

    public static String getServerHostName() throws Exception {
        return getServerIdentity().getHostName();
    }

    public String getServerCanonicalHostName() throws Exception {
        return getServerIdentity().getCanonicalHostName();
    }

    public static String getServerHostIp() throws Exception {
        return getServerIdentity().toString().split("/")[1];
    }

    public static String getServerUrlBase(LibertyServer server) {
        return "http://" + server.getHostname() + ":" + server.getBvtPort() + "/";
    }

    public static String getServerSecureUrlBase(LibertyServer server) {
        return "https://" + server.getHostname() + ":" + server.getBvtSecurePort() + "/";
    }

    public static String getServerIpUrlBase(LibertyServer server) throws Exception {
        return "http://" + getServerHostIp() + ":" + server.getBvtPort() + "/";
    }

    public static String getServerIpSecureUrlBase(LibertyServer server) throws Exception {
        return "https://" + getServerHostIp() + ":" + server.getBvtSecurePort() + "/";
    }

}
