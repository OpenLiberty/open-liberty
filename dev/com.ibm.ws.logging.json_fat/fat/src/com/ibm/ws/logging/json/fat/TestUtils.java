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
package com.ibm.ws.logging.json.fat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class TestUtils {

    private static Class<?> c = TestUtils.class;

    public static void runApp(LibertyServer server, String type) {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/LogstashApp";
        if (type.equals("access")) {
            url = url + "/AccessURL";
        } else if (type.equals("ffdc1")) {
            url = url + "?isFFDC=true";
        } else if (type.equals("ffdc2")) {
            url = url + "?secondFFDC=true";
        } else if (type.equals("ffdc3")) {
            url = url + "?thirdFFDC=true";
        } else if (type.equals("trace")) {
            url = url + "/TraceURL";
        }
        Log.info(c, "runApp", "---> Running the application with url : " + url);

        try {
            runGetMethod(url);
        } catch (Exception e) {
            Log.info(c, "runApp", " ---> Exception : " + e.getMessage());
        }
    }

    public static void runTrWriter(LibertyServer server, int level, String msgKey, String param) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        sb.append("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/trwriter/tr");
        String separator = "?";
        if (level >= 0) {
            sb.append(separator);
            sb.append("level=" + level);
            separator = "&";
        }
        if ((msgKey != null) && (msgKey.trim().length() > 0)) {
            sb.append(separator);
            sb.append("msgKey=" + msgKey.trim());
            separator = "&";
        }
        if ((param != null) && (param.trim().length() > 0)) {
            sb.append(separator);
            sb.append("msgParam=");
            sb.append(URLEncoder.encode(param.trim(), "UTF-8"));
        }
        String url = sb.toString();
        Log.info(c, "runTrWriter", "---> Running the application with url : " + url);

        try {
            runGetMethod(url);
        } catch (Exception e) {
            Log.info(c, "runTrWriter", " ---> Exception : " + e.getMessage());
        }
    }

    private static String runGetMethod(String urlStr) throws Exception {
        Log.info(c, "runGetMethod", "URL = " + urlStr);
        URL url = new URL(urlStr);
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
            for (String line = br.readLine(); line != null; line = br.readLine())
                lines.append(line).append(sep);

            return lines.toString();
        } finally {
            con.disconnect();
        }
    }
}
