/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

/**
 * Format url and invoke test servlet
 */
public class BatchJmsFatUtils {
    private static final int TIMEOUT = 30000;

    public static final String HEADER_CONTENT_TYPE_KEY = "Content-Type";
    public static final String MEDIA_TYPE_APPLICATION_JSON = "application/json; charset=UTF-8";

    public static StringBuilder runInServlet(String host, int port, String action, String appName, String operation, String cfJndi, String queueJndi) throws IOException {
        URL url = new URL("http://" + host + ":" + port + "/jmsweb/BatchJmsServlet?action=" + action + "&appName=" + appName + "&operation=" + operation + "&cfJndi="
                          + cfJndi + "&queueJndi=" + queueJndi);
        System.out.println("The Servlet URL is : " + url.toString());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            con.connect();
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine())
                lines.append(line).append(sep);

            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0)
                org.junit.Assert.fail("Missing success message in output. " + lines);

            return lines;
        } finally {
            con.disconnect();
        }
    }

    public static StringBuilder runInServlet(String host, int port, String action, String appName) throws IOException {
        return runInServlet(host, port, action, appName, "Start", "jms%2Fbatch%2FconnectionFactory", "jms%2Fbatch%2FjobSubmissionQueue");
    }

    public static StringBuilder runInServlet(String host, int port, String action, String appName, String operation) throws IOException {
        return runInServlet(host, port, action, appName, operation, "jms%2Fbatch%2FconnectionFactory", "jms%2Fbatch%2FjobSubmissionQueue");
    }

    public static StringBuilder publishInServlet(String host, int port, String action, String topicName) throws IOException {
        return publishInServlet(host, port, action, topicName, "jms%2Fbatch%2FtopicConnectionFactory", "jms%2Fbatch%2FbatchJobTopic");
    }

    public static StringBuilder publishInServlet(String host, int port, String action, String topicName, String cfJndi, String topicJndi) throws IOException {
        URL url = new URL("http://" + host + ":" + port + "/jmsweb/BatchJmsServlet?action=" + action + "&topicName=" + topicName + "&cfJndi="
                          + cfJndi + "&topicJndi=" + topicJndi);
        System.out.println("The Servlet URL is : " + url.toString());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            con.connect();
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine())
                lines.append(line).append(sep);

            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0)
                org.junit.Assert.fail("Missing success message in output. " + lines);

            return lines;
        } finally {
            con.disconnect();
        }
    }

    /**
     * escape the / character for url
     * 
     * @return
     */
    private static String format(String input) {
        final StringBuilder result = new StringBuilder();
        final StringCharacterIterator iterator = new StringCharacterIterator(input);
        char character = iterator.current();
        while (character != CharacterIterator.DONE) {
            if (character == '/') {
                result.append("%2F");
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }

    public static HttpURLConnection getConnection(String path, int expectedResponseCode, HTTPRequestMethod method, InputStream streamToWrite, Map<String, String> map) throws IOException {
        return HttpUtils.getHttpConnection(getURL(path), expectedResponseCode, new int[0], TIMEOUT, method, map, streamToWrite);
    }

    protected static URL getURL(String path) throws MalformedURLException {
        URL myURL = new URL("https://localhost:" + getPort() + path);
        System.out.println("Built URL: " + myURL.toString());
        return myURL;
    }

    protected static String getPort() {
        return System.getProperty("HTTP_default.secure", "8020");
    }

    /**
     * @return a JsonObject for the given map
     */
    public static JsonObject buildJsonObjectFromMap(Map map) {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        for (Map.Entry entry : (Set<Map.Entry>) map.entrySet()) {
            builder.add((String) entry.getKey(), (String) entry.getValue());
        }

        return builder.build();
    }
}
