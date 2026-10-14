/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.logging.osgi.fat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 * Common base class for WsLogHandler FAT test classes, providing shared
 * constants and HTTP client helper methods.
 */
public abstract class WsLogHandlerBaseTest {

    /** Bundle symbolic name — no .jar suffix. */
    protected static final String BUNDLE = "com.ibm.ws.logging.osgi.fat.wsloghandler";
    /** Feature short name — no .mf suffix. */
    protected static final String FEATURE = "wsLogHandler-1.0";

    protected abstract LibertyServer getServer();

    protected int getStatus(String path) throws Exception {
        URL url = new URL("http://" + getServer().getHostname() + ":" + getServer().getHttpDefaultPort() + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
        int status = conn.getResponseCode();
        conn.disconnect();
        return status;
    }

    protected String getBody(String path) throws Exception {
        URL url = new URL("http://" + getServer().getHostname() + ":" + getServer().getHttpDefaultPort() + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    protected int postForm(String path, String body) throws Exception {
        URL url = new URL("http://" + getServer().getHostname() + ":" + getServer().getHttpDefaultPort() + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes("UTF-8"));
        }
        int status = conn.getResponseCode();
        conn.disconnect();
        return status;
    }

    protected int getCapturedCount() throws Exception {
        String body = getBody("/wsLogHandler/messages");
        Log.info(WsLogHandlerBaseTest.class, "messages", body);
        int idx = body.indexOf("\"totalCaptured\":");
        if (idx != -1) {
            int start = idx + "\"totalCaptured\":".length();
            int end = body.indexOf(",", start);
            if (end == -1) {
                end = body.indexOf("}", start);
            }
            return Integer.parseInt(body.substring(start, end).trim());
        } 
        return 0;
    }
}
