/*******************************************************************************
 * Copyright (c) 2009, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jca.fat.regr.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import componenttest.topology.impl.LibertyServer;

public class HttpHelper {
    private final static String CLASSNAME = HttpHelper.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // Create a connection to a servlet on this host.  Specify whether the home or business interface
    // should be used and whether the remote or local interface should be used.
    public static HttpURLConnection getHttpURLConnection(String servlet, int testNum, String contextRoot, LibertyServer server) {
        HttpURLConnection connection = null;
        String parameters = "testNum=" + Integer.toString(testNum);
        int portNumber = server.getHttpDefaultPort();
        String hostName = server.getHostname();

        try {
            URL url = new URL("http", hostName, portNumber, "/" + contextRoot + "/" + servlet + "?" + parameters);
            svLogger.logp(Level.INFO, CLASSNAME, "getHttpURLConnection", "Trying to connect to URL: " + "http://" + hostName + ":" + portNumber + "/" + contextRoot + "/" + servlet
                                                                         + "?" + parameters);
            connection = (HttpURLConnection) url.openConnection();
        } catch (Exception e) {
            svLogger.logp(Level.WARNING, CLASSNAME, "getHttpURLConnection", "Caught exception", e);
        }
        return connection;
    }

    // Create a connection to a servlet on this host.  Specify whether the home or business interface
    // should be used and whether the remote or local interface should be used.
    public static HttpURLConnection getHttpURLConnection(String servlet, String testMethodName, String contextRoot, LibertyServer server) {
        HttpURLConnection connection = null;
        String parameters = "testMethodName=" + testMethodName;
        int portNumber = server.getHttpDefaultPort();
        String hostName = server.getHostname();

        try {
            URL url = new URL("http", hostName, portNumber, "/" + contextRoot + "/" + servlet + "?" + parameters);
            svLogger.logp(Level.INFO, CLASSNAME, "getHttpURLConnection", "Trying to connect to URL: " + "http://" + hostName + ":" + portNumber + "/" + contextRoot + "/" + servlet
                                                                         + "?" + parameters);
            connection = (HttpURLConnection) url.openConnection();
        } catch (Exception e) {
            svLogger.logp(Level.WARNING, CLASSNAME, "getHttpURLConnection", "Caught exception", e);
        }
        return connection;
    }

    // Read buffered output from the servlet.  Output should be returned, line by line so
    // it can be processed by the test class.
    public static String[] readFromHttpConnection(HttpURLConnection conn) throws IOException {
        InputStreamReader isr = new InputStreamReader(conn.getInputStream());
        BufferedReader urlReader = new BufferedReader(isr);
        String line = "";

        Vector<String> strVector = new Vector<String>();
        while ((line = urlReader.readLine()) != null) {
            if (!line.equals("")) {
                strVector.add(line);
            }
        }
        String[] strs = new String[strVector.size()];
        return strVector.toArray(strs);
    }
}