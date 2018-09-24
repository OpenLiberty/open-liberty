/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.cdi12.fat.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
public class AbstractTest extends FATServletClient {

    private final static int REQUEST_TIMEOUT = 10;

    protected static String appname;

    protected LibertyServer serverRef;

    protected void runTestOnServer(String target, String testMethod, Map<String, String> params, String expectedResponse) throws ProtocolException, MalformedURLException, IOException {

        //build basic URI
        StringBuilder sBuilder = new StringBuilder("http://").append(serverRef.getHostname())
                        .append(":")
                        .append(serverRef.getHttpDefaultPort())
                        .append("/")
                        .append(target)
                        .append("?test=")
                        .append(testMethod);

        //add params to URI
        if (params != null && params.size() > 0) {

            StringBuilder paramStr = new StringBuilder();

            Iterator<String> itr = params.keySet().iterator();

            while (itr.hasNext()) {
                String key = itr.next();
                paramStr.append("&@" + key + "=" + params.get(key));
            }

            sBuilder.append(paramStr.toString());
        }

        sBuilder.append("&@secport=" + serverRef.getHttpDefaultSecurePort());
        sBuilder.append("&@hostname=" + serverRef.getHostname());

        String urlStr = sBuilder.toString();
        Log.info(this.getClass(), testMethod, "Calling ClientTestApp with URL=" + urlStr);

        HttpURLConnection con;

        con = HttpUtils.getHttpConnection(new URL(urlStr), HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();

        Log.info(this.getClass(), testMethod, "The response: " + line);
        assertTrue("Real response is " + line + " and the expected response is " + expectedResponse, line.contains(expectedResponse));

    }

    protected StringBuilder runGetMethod(String path, int exprc, String testOut, boolean check) throws IOException {
        URL url = new URL("http://localhost:" + serverRef.getHttpDefaultPort()
                          + "/" + appname + path);
        int retcode;
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            retcode = con.getResponseCode();
            if (check) {
                if (retcode != exprc) {
                    fail("Bad return Code from Get. Expected " + exprc + "Got"
                         + retcode);
                }
            }

            StringBuilder lines = new StringBuilder();
            if (retcode == 200) {
                InputStream is = con.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                String sep = System.getProperty("line.separator");
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    lines.append(line).append(sep);
                }

                if (check) {
                    if (lines.indexOf(testOut) < 0) {
                        fail("Missing success message in output. " + lines);
                    }
                }
            }

            return lines;
        } finally {
            con.disconnect();
        }
    }

    protected void assertLibertyMessage(String message, int number, String equal) {
        try {
            List<String> messages = serverRef.findStringsInLogs(message, serverRef.getMostRecentTraceFile());
            if (equal.equals("more")) {
                assertTrue("Expect to get CDI test message more than " + number + ": " + message, messages.size() > number);

            } else if (equal.equals("less")) {
                //Why use this equal param? Because for each PerRequest request, the message will be report, so there will be several messages in logs
                //Also because in other jdk such as oracle jdk, maybe the execution order is unexpectable
                assertTrue("Expect to get CDI test message less than " + number + ": " + message, messages.size() <= number && messages.size() > 0);
            } else {
                assertTrue("Expect to get CDI test messages equal " + number + ": " + message, messages.size() == number);
            }
        } catch (Exception e) {
            fail("Get Exception " + e.getMessage() + " when assertLibertyMessage");
        }
    }
}
