/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.transaction.fat.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Transaction;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

public class TxFATServletClient extends FATServletClient {

    /**
     * Runs a test in the servlet and returns the servlet output.
     *
     * @param  server      the started server containing the started application
     * @param  path        the url path (e.g. myApp/myServlet)
     * @param  queryString query string including at least the test name
     *                         (e.g. testName or testname&key=value&key=value)
     * @return             output of the servlet
     */
    public StringBuilder runInServlet(LibertyServer server, String path, String queryString) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + getPathAndQuery(path, queryString));
        Log.info(getClass(), testName.getMethodName(), "URL is " + url);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(getClass(), "runInServlet", line);
            }

            return lines;
        } finally {
            con.disconnect();
        }
    }

    /**
     * Temporarily set an extra transaction attribute
     */
    public static AutoCloseable withExtraTranAttributes(LibertyServer server, String appName, String... attrs) throws Exception {
        final ServerConfiguration config = server.getServerConfiguration();
        final ServerConfiguration originalConfig = config.clone();
        final Transaction transaction = config.getTransaction();

        if (attrs == null || attrs.length % 2 != 0) {
            throw new IllegalArgumentException();
        }

        for (int i = 0; (i + 1) < attrs.length; i += 2) {
            transaction.setExtraAttribute(attrs[i], attrs[i + 1]);
        }

        try {
        	if (server.isStarted()) server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            if (server.isStarted()) server.waitForConfigUpdateInLogUsingMark(Collections.singleton(appName));
        } catch (Exception e) {
            try {
            	server.updateServerConfiguration(originalConfig);
            } catch (Exception e1) {
                e.addSuppressed(e1);
            }
            throw e;
        }

        return () -> server.updateServerConfiguration(originalConfig);
    }
}