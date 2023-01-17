/*******************************************************************************
 * Copyright (c) 2011, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import java.util.List;
import java.util.Map;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.web.WebRequestUtils;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Utilities for testing HTTP connections.
 */
public class SecurityFatHttpUtils extends HttpUtils {

    protected static Class<?> thisClass = SecurityFatHttpUtils.class;

    public WebRequestUtils webReqUtils = new WebRequestUtils();

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

    public HttpURLConnection getHttpConnectionWithAnyResponseCode(String path, HTTPRequestMethod method, Map<String, List<String>> requestParms) throws Exception {
        int timeout = DEFAULT_TIMEOUT;
        String urlString = path;
        String builtParms = webReqUtils.buildUrlQueryString(requestParms);
        Log.info(thisClass, "getHttpConnectionWithAnyResponseCode", "builtParms: " + builtParms);
        if ((HTTPRequestMethod.DELETE.equals(method) || HTTPRequestMethod.PUT.equals(method)) && builtParms != null) {
            urlString = urlString + "?" + builtParms;
        }
        URL url = new URL(urlString);
        HttpURLConnection con = getHttpConnection(url, timeout, method);
        Log.info(SecurityFatHttpUtils.class, "getHttpConnection", "Connecting to " + url.toExternalForm() + " expecting http response in " + timeout + " seconds.");
        con.connect();
        Log.info(thisClass, "getHttpConnectionWithAnyResponseCode", "HttpURLConnection successfully completed con.connect");
        Log.info(thisClass, "getHttpConnectionWithAnyResponseCode", "Response (Status):  " + con.getResponseCode());
        Log.info(thisClass, "getHttpConnectionWithAnyResponseCode", "Response (Message):  " + con.getResponseMessage());
        return con;
    }

    protected HttpURLConnection prepareConnection(String rawUrl, HTTPRequestMethod method) throws Exception {
        String thisMethod = "HttpURLConnection";
        URL url = AutomationTools.getNewUrl(rawUrl);
        Log.info(thisClass, thisMethod, "HttpURLConnection URL is set to: " + url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        Log.info(thisClass, thisMethod, "HttpURLConnection successfully opened the connection to URL " + url);
        connection.setRequestMethod(method.toString());
        Log.info(thisClass, thisMethod, "HttpURLConnection set request method to " + method);
        connection.setConnectTimeout(120000); // 2 minutes
        Log.info(thisClass, thisMethod, "HttpURLConnection set connect timeout to 2 min " + method);
        connection.setReadTimeout(120000); // 2 minutes
        Log.info(thisClass, thisMethod, "HttpURLConnection set read timeout to 2 min " + method);
        // connection.setInstanceFollowRedirects(true); // allow redirect
        // Log.info(thisClass, thisMethod,
        // "HttpURLConnection setInstanceFollowRedirects is set to true");
        connection.setDoInput(true);
        if (method != HTTPRequestMethod.GET) {
            connection.setDoOutput(true);
        }
        if (method == HTTPRequestMethod.PUT) {
            connection.addRequestProperty("Content-Type", "application/json");
        }
        return connection;
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
