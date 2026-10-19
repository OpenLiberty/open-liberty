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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

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

    /**
     * Issues a GET and returns the HTTP status. When the status is not 200 the response
     * body is logged, so a failed verify/verifyAbsent shows the unmatched, missing or
     * unexpectedly matched messages and not just the status code.
     */
    protected int getStatus(String path) throws Exception {
        URL url = new URL("http://" + getServer().getHostname() + ":" + getServer().getHttpDefaultPort() + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
        try {
            int status = conn.getResponseCode();
            if (status != 200) {
                Log.info(WsLogHandlerBaseTest.class, "getStatus",
                         "GET " + path + " returned " + status + "; body: " + readFully(conn.getErrorStream()));
            }
            return status;
        } finally {
            conn.disconnect();
        }
    }

    /** Reads the stream into a single line of text; returns "" for a null stream. */
    private static String readFully(InputStream in) throws Exception {
        if (in == null) {
            return "";
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
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

    /**
     * Posts a form. {@code body} is {@code name=value&name=value}, with <em>unencoded</em>
     * values (which must not contain '&'); each value is URL-encoded here.
     */
    protected int postForm(String path, String body) throws Exception {
        URL url = new URL("http://" + getServer().getHostname() + ":" + getServer().getHttpDefaultPort() + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(encodeForm(body).getBytes("UTF-8"));
        }
        int status = conn.getResponseCode();
        conn.disconnect();
        return status;
    }

    private static String encodeForm(String body) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String pair : body.split("&")) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            int eq = pair.indexOf('=');
            if (eq == -1) {
                sb.append(URLEncoder.encode(pair, "UTF-8"));
            } else {
                sb.append(URLEncoder.encode(pair.substring(0, eq), "UTF-8"))
                  .append('=')
                  .append(URLEncoder.encode(pair.substring(eq + 1), "UTF-8"));
            }
        }
        return sb.toString();
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
