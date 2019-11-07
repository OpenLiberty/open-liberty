/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat.restmetrics;

import static com.ibm.ws.jaxrs20.fat.TestUtils.getBaseTestUri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class RestMetricsTest {

    public static final String ignore_message = "CWWKW1002W";

    @Server("com.ibm.ws.jaxrs.fat.restmetrics")
    public static LibertyServer server;

    private static final String metricswar = "restmetrics";

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, metricswar, "com.ibm.ws.jaxrs.fat.restmetrics");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    public HttpClient getHttpClient() {
        return new DefaultHttpClient();
    }

    public void resetHttpClient(HttpClient client) {
        client.getConnectionManager().shutdown();
    }

    private int getPort() {
        return server.getHttpDefaultPort();
    }

    private String getHost() {
        return server.getHostname();
    }

    private final String mappedUri = getBaseTestUri(metricswar, "rest", "restmetrics");

    private static String getMethodString = "restmetrics_com_ibm_ws_jaxrs_fat_restmetrics_RestMetricsResource_getMessage__";
    private static String postMethodString = "restmetrics_com_ibm_ws_jaxrs_fat_restmetrics_RestMetricsResource_postMessage_java_lang_String_";
    private static String putMethodString = "restmetrics_com_ibm_ws_jaxrs_fat_restmetrics_RestMetricsResource_putMessage_java_lang_String_";
    private static String deleteMethodString = "restmetrics_com_ibm_ws_jaxrs_fat_restmetrics_RestMetricsResource_deleteMessage__";


    @Test
    public void testSimple() throws IOException {
        try {
            server.stopServer(ignore_message);
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        int getCount = 0;

        int buffer = 2; // margin or error in seconds.
        int expectedSingleResponse = 2;
        runGetMethod(200, "/restmetrics/rest/restmetrics", "Metrics!");
        getCount++;
        checkCount(200, getMethodString, getCount);
        checkResponseTime(200, getMethodString, expectedSingleResponse * getCount, expectedSingleResponse * getCount + buffer);

    }

    @Test
    public void testAll() throws IOException {
        try {
            server.stopServer(ignore_message);
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        int getCount = 0;
        int postCount = 0;
        int putCount = 0;
        int deleteCount = 0;
        int buffer = 2; // margin or error in seconds.
        int expectedSingleResponse = 2;
        runGetMethod(200, "/restmetrics/rest/restmetrics", "Metrics!");
        getCount++;
        runPostMethod();
        postCount++;
        runPostMethod();
        postCount++;
        runPostMethod();
        postCount++;
        runPutMethod();
        putCount++;
        runPutMethod();
        putCount++;
        runPutMethod();
        putCount++;
        runPutMethod();
        putCount++;
        runDeleteMethod();
        deleteCount++;
        checkCount(200, getMethodString, getCount);
        checkResponseTime(200, getMethodString, expectedSingleResponse * getCount, expectedSingleResponse * getCount + buffer);
        checkCount(200, postMethodString, 3);
        checkResponseTime(200, postMethodString, expectedSingleResponse * postCount, expectedSingleResponse * postCount + buffer);
        checkCount(200, putMethodString, 4);
        checkResponseTime(200, putMethodString, expectedSingleResponse * putCount, expectedSingleResponse * putCount + buffer);
        checkCount(200, deleteMethodString, 1);
        checkResponseTime(200, deleteMethodString, expectedSingleResponse * deleteCount, expectedSingleResponse * deleteCount + buffer);

    }

    /**
     * Tests that JAX-RS engine can map a request URL containing encoded
     * and non-encoded characters to the proper target destination
     *
     * @throws IOException
     */
    /*
     * @Test
     * public void testSimpleWithEncodedURL() throws IOException {
     * runGetMethod(200, "/restmetrics/apppathrest!/restmetrics", "Metrics!");
     * runGetMethod(200, "/restmetrics/apppathrest%21/restmetrics", "Metrics!");
     * }
     */

    private void runGetMethod(int exprc, String requestUri, String testOut) throws IOException {
        URL url = new URL("http://" + getHost() + ":" + getPort() + requestUri);
        int retcode;
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            retcode = con.getResponseCode();

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
            }

            if (lines.indexOf(testOut) < 0)
                fail("Missing success message in output. " + lines);

            if (retcode != exprc)
                fail("Bad return Code from Get. Expected " + exprc + "Got"
                     + retcode);

            return;
        } finally {
            con.disconnect();
        }
    }

    private void runPostMethod() throws IOException {
        HttpPost postMethod = new HttpPost(mappedUri + "/post1");
        StringEntity entity = new StringEntity("Post");
        entity.setContentType("text/*");
        postMethod.setEntity(entity);
        HttpClient client = getHttpClient();

        try {
            HttpResponse resp = client.execute(postMethod);

            assertEquals(200, resp.getStatusLine().getStatusCode());

        } finally {
            resetHttpClient(client);
        }
    }

    private void runPutMethod() throws IOException {
        HttpPut putMethod = new HttpPut(mappedUri + "/put1");
        StringEntity entity = new StringEntity("Put");
        entity.setContentType("text/*");
        putMethod.setEntity(entity);

        HttpClient client = getHttpClient();

        try {
            HttpResponse resp = client.execute(putMethod);

            assertEquals(200, resp.getStatusLine().getStatusCode());

        } finally {
            resetHttpClient(client);
        }

    }


    private void runDeleteMethod() throws IOException {
        HttpDelete deleteMethod = new HttpDelete(mappedUri + "/delete1");

        HttpClient client = getHttpClient();

        try {
            HttpResponse resp = client.execute(deleteMethod);
            assertEquals(204, resp.getStatusLine().getStatusCode());

        } finally {
            resetHttpClient(client);
        }


    }

    private void checkCount(int exprc, String methodString, int expectedCount)
                    throws IOException {
        URL url = new URL("http://" + getHost() + ":" + getPort() + "/metrics/vendor/RESTful.request.total");
        int retcode;
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            retcode = con.getResponseCode();
            if (retcode != exprc) {
                fail("Bad return Code from Metrics method call. Expected " + exprc + "Got"
                     + retcode);

                return;
            }

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            int value = 0;

            // Read the lines from the output looking for one that looks like this:
            // "vendor_RESTful_request_total{restful="restmetrics_com_ibm_ws_jaxrs_fat_restmetrics_
            // RestMetricsResource_getMessage__"} 1" and get the count from the end of it
            for (String line = br.readLine(); line != null; line = br
                            .readLine()) {
                if (line.contains(methodString)) {
                    String stringValue = line.substring(line.indexOf("}") + 2);
                    value = Integer.parseInt(stringValue);
                }
            }

            if (value != expectedCount) {
                fail("Failure:  Expected Request Count of " + expectedCount +
                     " but received Request Count of " + value);
            }

            return;

        } finally {
            con.disconnect();
        }
    }

    private void checkResponseTime(int exprc, String methodString, int minExpectedResponseTime, int maxExpectedResponseTime) throws IOException {
        URL url = new URL("http://" + getHost() + ":" + getPort() + "/metrics/vendor/RESTful.responseTime.total");
        int retcode;
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            retcode = con.getResponseCode();
            if (retcode != exprc) {
                fail("Bad return Code from Metrics method call. Expected " + exprc + "Got"
                     + retcode);
                return;
            }

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            double responseTime = 0;

            // Read the lines from the output looking for one that looks like this:
            // "vendor_RESTful_responseTime_total_seconds{restful="restmetrics_com_ibm_ws_jaxrs_fat_restmetrics_
            // RestMetricsResource_getMessage__"} 5.028664531" and get the responseTime from the end of it
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (line.contains(methodString)) {
                    String stringValue = line.substring(line.indexOf("}") + 2);
                    responseTime = Double.parseDouble(stringValue);
                }
            }

            if (!((minExpectedResponseTime <= responseTime) && (responseTime <= maxExpectedResponseTime))) {
                fail("Failure:  Expected response time >= " + minExpectedResponseTime +
                     " and <= " + maxExpectedResponseTime +
                     " but received a response time of " + responseTime);
            }

            return;

        } finally {
            con.disconnect();
        }
    }
}
