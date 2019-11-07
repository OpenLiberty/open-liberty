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
import java.util.ArrayList;

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
import com.ibm.ws.jaxrs.fat.restmetrics.MetricsUnmappedCheckedException;
import com.ibm.ws.jaxrs.fat.restmetrics.MetricsUnmappedUncheckedException;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class RestMetricsTest {

    public static final String ignore_message = "CWWKW1002W";
    final static String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") +
                    "/restmetrics/rest/";

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

    private static int getPort() {
        return server.getHttpDefaultPort();
    }

    private static String getHost() {
        return server.getHostname();
    }

    private final String mappedUri = getBaseTestUri(metricswar, "rest", "restmetrics");

    private static String[] methodStrings = { "restmetrics_com_ibm_ws_jaxrs_fat_restmetrics_RestMetricsResource_getMessage__",
                                              "restmetrics_com_ibm_ws_jaxrs_fat_restmetrics_RestMetricsResource_getMultiParamMessage_java_lang_String_",
                                              "restmetrics_com_ibm_ws_jaxrs_fat_restmetrics_RestMetricsResource_getMultiParamMessage_java_lang_String_java_lang_String_",
                                              "restmetrics_com_ibm_ws_jaxrs_fat_restmetrics_RestMetricsResource_postMessage_java_lang_String_",
                                              "restmetrics_com_ibm_ws_jaxrs_fat_restmetrics_RestMetricsResource_putMessage_java_lang_String_",
                                              "restmetrics_com_ibm_ws_jaxrs_fat_restmetrics_RestMetricsResource_deleteMessage__" };

    private final int[] methodCounts = new int[6];

    private final double[] responseTimes = new double[6];

    int buffer = 1; // margin or error in seconds for response times.

    int expectedRT = 1; //Expected min. response time for a single request.

    @Test
    public void testAll() throws IOException {
        try {
            // Execute a get method with varying numbers of parameters.
            runGetMethod(0, 200, "/restmetrics/rest/restmetrics", "Metrics!");

            runGetMethod(1, 200, "/restmetrics/rest/restmetrics/test", "Metrics!");

            runGetMethod(2, 200, "/restmetrics/rest/restmetrics/test/test", "Metrics!");

            runGetMethod(2, 200, "/restmetrics/rest/restmetrics/mappedChecked", "Mapped Checked");

            try {
                runGetMethod(2, 500, "/restmetrics/rest/restmetrics/unmappedChecked", "Unmapped Checked");
            } catch (MetricsUnmappedCheckedException e) {
                System.out.println("Caught expected exception: " + e);
            }

            runGetMethod(2, 200, "/restmetrics/rest/restmetrics/mappedUnchecked", "Mapped Unchecked");

            try {
                runGetMethod(2, 500, "/restmetrics/rest/restmetrics/unmappedUnchecked", "Unmapped Unchecked");
            } catch (MetricsUnmappedUncheckedException e) {
                System.out.println("Caught expected exception: " + e);
            }
        } catch (MetricsUnmappedCheckedException e1) {
            //no-op
        } catch (MetricsUnmappedUncheckedException e2) {
            //no-op
        }
        // Execute post method 3 times.
        runPostMethod(3);

        runPostMethod(3);

        runPostMethod(3);

        // Execute put method 4 times.
        runPutMethod(4);

        runPutMethod(4);

        runPutMethod(4);

        runPutMethod(4);

        // Execute delete method once.
        runDeleteMethod(5);

        ArrayList<String> countList = getMetricsStrings("/metrics/vendor/RESTful.request.total");
        ArrayList<String> responseList = getMetricsStrings("/metrics/vendor/RESTful.responseTime.total");
        //first lets confirm that the metrics information is correct
        //Now  lets confirm the metrics information is available.
        for (int i = 0; i < 6; i++) {
            // Confirm the metrics data
            checkCount(countList, i);
            responseTimes[i] = checkResponseTime(responseList, i);
            // Confirm the monitor data
            runCheckMonitorStats(200, i);
        }
    }

    private void runCheckMonitorStats(int exprc, int index) throws IOException {
        URL url = new URL("http://" + getHost() + ":" + getPort() +
                          "/restmetrics/rest/restmetrics/" + index +
                          "/" + methodCounts[index] + "/" + responseTimes[index]);
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

            if (lines.indexOf("Passed!") < 0)
                fail("Missing success message in output. " + lines);

            if (retcode != exprc)
                fail("Bad return Code from Get. Expected " + exprc + "Got"
                     + retcode);

            return;
        } finally {
            con.disconnect();
        }
    }

    private void runGetMethod(int index, int exprc, String requestUri, String testOut) throws IOException, MetricsUnmappedCheckedException, MetricsUnmappedUncheckedException {
        URL url = new URL("http://" + getHost() + ":" + getPort() + requestUri);
        int retcode;
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            retcode = con.getResponseCode();

            if (retcode == 200) {
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

            }

            if (retcode != exprc)
                fail("Bad return Code from Get. Expected: " + exprc + ", Received: "
                     + retcode);

            // Increment the count
            methodCounts[index]++;
            return;
        } finally {
            con.disconnect();
        }
    }

    private void runPostMethod(int index) throws IOException {

        HttpPost postMethod = new HttpPost(mappedUri + "/post1");
        org.apache.http.entity.
        StringEntity entity = new StringEntity("Post");
        entity.setContentType("text/*");
        postMethod.setEntity(entity);
        HttpClient client = getHttpClient();

        try {
            HttpResponse resp = client.execute(postMethod);

            assertEquals(200, resp.getStatusLine().getStatusCode());

            // Increment the count
            methodCounts[index]++;

        } finally {
            resetHttpClient(client);
        }
    }

    private void runPutMethod(int index) throws IOException {
        HttpPut putMethod = new HttpPut(mappedUri + "/put1");
        StringEntity entity = new StringEntity("Put");
        entity.setContentType("text/*");
        putMethod.setEntity(entity);

        HttpClient client = getHttpClient();

        try {
            HttpResponse resp = client.execute(putMethod);

            assertEquals(200, resp.getStatusLine().getStatusCode());

            // Increment the count
            methodCounts[index]++;

        } finally {
            resetHttpClient(client);
        }

    }


    private void runDeleteMethod(int index) throws IOException {
        HttpDelete deleteMethod = new HttpDelete(mappedUri + "/delete1");

        HttpClient client = getHttpClient();

        try {
            HttpResponse resp = client.execute(deleteMethod);
            assertEquals(204, resp.getStatusLine().getStatusCode());

            // Increment the count
            methodCounts[index]++;

        } finally {
            resetHttpClient(client);
        }


    }

    private ArrayList<String> getMetricsStrings(String metricString) {
        HttpURLConnection con = null;
        try {
            URL url = new URL("http://" + getHost() + ":" + getPort() + metricString);
            int retcode;
            con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            retcode = con.getResponseCode();
            if (retcode != 200) {

                fail("Bad return Code from Metrics method call. Expected 200: Got"
                     + retcode);

                return null;
            }

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);

            BufferedReader br = new BufferedReader(isr);

            ArrayList<String> lines = new ArrayList<String>();
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.add(line);
            }
            return lines;

        } catch (Exception e) {
            fail("Caught unexpected exception: " + e);
            return null;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }

    }

    private void checkCount(ArrayList<String> lines, int index) {
        int value = 0;

        // Read the lines from the output looking for one that looks like this:
        // "vendor_RESTful_request_total{restful="restmetrics_com_ibm_ws_jaxrs_fat_restmetrics_
        // RestMetricsResource_getMessage__"} 1" and get the count from the end of it
        for (String line : lines) {
            if (line.contains(methodStrings[index])) {
                String stringValue = line.substring(line.indexOf("}") + 2);
                value = Integer.parseInt(stringValue);
            }
        }

        if (value != methodCounts[index]) {
            fail("Failure:  Expected Request Count of " + methodCounts[index] +
                 " but received Request Count of " + value);
        }

        return;

    }

    private float checkResponseTime(ArrayList<String> lines, int index) {

        float responseTime = 0;

        // Read the lines from the output looking for one that looks like this:
        // "vendor_RESTful_responseTime_total_seconds{restful="restmetrics_com_ibm_ws_jaxrs_fat_restmetrics_
        // RestMetricsResource_getMessage__"} 5.028664531" and get the responseTime from the end of it
        for (String line : lines) {
            if (line.contains(methodStrings[index])) {
                String stringValue = line.substring(line.indexOf("}") + 2);
                responseTime = Float.parseFloat(stringValue);
            }
        }
        int minExpectedResponseTime = expectedRT * methodCounts[index];
        int maxExpectedResponseTime = minExpectedResponseTime + buffer;
        if (!((minExpectedResponseTime <= responseTime) && (responseTime <= maxExpectedResponseTime))) {
            fail("Failure:  Expected response time >= " + minExpectedResponseTime +
                 " and <= " + maxExpectedResponseTime +
                 " but received a response time of " + responseTime);
            return 0;
        }

        return responseTime;


    }
}
