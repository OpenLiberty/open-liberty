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
package com.ibm.ws.jaxrs20.cdi12.fat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpResponse;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * JAX-RS Fat utility class for some commonly used functionality.
 * More methods to be added later.
 */
public class TestUtils {

    public static int getPort() {
        return Integer.valueOf(System.getProperty("HTTP_default", "8000"));
    }

    /**
     * Utility method for converting HttpResponse to String
     *
     * @param HttpResponse response
     * @return String representation of response
     * @throws IOException
     */
    public static String asString(HttpResponse response) throws IOException {
        if (response.getEntity() == null) {
            return "";
        }

        final InputStream in = response.getEntity().getContent();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            out.flush();
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    // TODO: In the getBaseTestUri() methods, need to make sure we
    // are not appending double forward slashes

    /**
     * Utility for constructing the resource URL needed for
     * test client to call service.
     *
     * @param contextRoot Tests assume this is the WAR file name
     * @param urlPattern Specified in web.xml's url-pattern
     * @param path Value of resource's @Path annotation
     * @return
     */
    public static String getBaseTestUri(String contextRoot, String urlPattern, String resourcePath) {

        // If any of the parameters are null, return empty; usage error
        if (contextRoot == null || urlPattern == null || resourcePath == null) {
            System.out.println("getBaseTestUri(contextRoot, urlPattern, resourcePath) returning empty string");
            return "";
        }

        // For tests, service will always be in the same machine
        StringBuffer sb = new StringBuffer("http://localhost:");
        sb.append(getPort());
        sb.append("/");
        sb.append(contextRoot);
        sb.append("/");
        sb.append(urlPattern);
        sb.append("/");
        sb.append(resourcePath);
        return sb.toString();
    }

    /**
     * Utility for constructing the resource URL needed for
     * test client to call service.
     *
     * @param contextRoot Tests assume this is the WAR file name
     * @param path Either the url-pattern or the resource path
     * @return
     */
    public static String getBaseTestUri(String contextRoot, String path) {

        // If either of the parameters are null, return empty; usage error
        if (contextRoot == null || path == null) {
            System.out.println("getBaseTestUri(contextRoot, path) returning empty string");
            return "";
        }

        // For tests, service will always be in the same machine
        StringBuffer sb = new StringBuffer("http://localhost:");
        sb.append(getPort());
        sb.append("/");
        sb.append(contextRoot);
        sb.append("/");
        sb.append(path);
        return sb.toString();
    }

    /**
     * Utility for constructing the resource URL needed for
     * test client to call service.
     *
     * @param contextRoot Tests assume this is the WAR file name
     * @param path Either the url-pattern or the resource path
     * @return
     */
    public static String getBaseTestUri(String contextRoot, String urlPattern, String resourcePath, String query) {

        // If any of the parameters are null, return empty; usage error
        if (contextRoot == null || urlPattern == null || resourcePath == null || query == null) {
            System.out.println("getBaseTestUri(contextRoot, urlPattern, resourcePath, query) returning empty string");
            return "";
        }

        // For tests, service will always be in the same machine
        StringBuffer sb = new StringBuffer("http://localhost:");
        sb.append(getPort());
        sb.append("/");
        sb.append(contextRoot);
        sb.append("/");
        sb.append(urlPattern);
        sb.append("/");
        sb.append(resourcePath);
        sb.append("/");
        sb.append(query);
        return sb.toString();
    }

    public static String accessWithURLConn(String urlStr, String httpMethod, String reqMediaType, int expectrc, byte[] postData) throws IOException {
        URL url = new URL(urlStr);
        int retcode;
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            if (reqMediaType != null)
                con.setRequestProperty("Content-Type", reqMediaType);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod(httpMethod);

            if (httpMethod.equalsIgnoreCase("post") && postData != null) {
                con.getOutputStream().write(postData);
            }

            retcode = con.getResponseCode();
            if (retcode != expectrc)
                fail("Bad return Code from " + httpMethod + ",expect=" + expectrc + ", but resp=" + retcode);

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

    public static void runTestOnServer(LibertyServer server, String target, String testMethod, Map<String, String> params, String expectedResponse) throws Exception {

        //build basic URI
        StringBuilder sBuilder = new StringBuilder("http://").append(server.getHostname())
                        .append(":")
                        .append(server.getHttpDefaultPort())
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

        String urlStr = sBuilder.toString();

        HttpURLConnection con;

        con = HttpUtils.getHttpConnection(new URL(urlStr), HttpURLConnection.HTTP_OK, 10);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();

        assertTrue("The expected response is " + expectedResponse + ",the current response is " + line, line.contains(expectedResponse));

    }
}
