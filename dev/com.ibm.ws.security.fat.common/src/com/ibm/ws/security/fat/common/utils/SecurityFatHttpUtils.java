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
import java.net.MalformedURLException;
import java.net.URL;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Utilities for testing HTTP connections.
 */
public class SecurityFatHttpUtils extends HttpUtils {

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
        Log.finer(HttpUtils.class, "getHttpConnection", "Connecting to " + url.toExternalForm() + " expecting http response in " + timeout + " seconds.");
        con.connect();
        return con;
    }

    public static URL createURL(LibertyServer server, String path) throws MalformedURLException {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return new URL("http://" + server.getHostname() + ":" + server.getBvtPort() + path);
    }

}
