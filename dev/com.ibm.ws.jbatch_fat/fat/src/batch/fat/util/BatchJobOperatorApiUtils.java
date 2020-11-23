/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jbatch.test.BatchRestUtils;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

/**
 *
 */
public class BatchJobOperatorApiUtils {

    protected static LibertyServer server = null;
    protected static BatchRestUtils restUtils = null;
    protected static JsonObject validJobInstance = null;
    protected static JsonObject validExecutionRecord = null;

    /**
     * helper for simple logging.
     */
    protected static void log(String method, Object msg) {
        Log.info(BatchRestUtils.class, method, String.valueOf(msg));
    }

    /**
     * @return the response - a JsonArray
     */
    protected JsonArray getRunningJobExecutions(String jobName, int responseCode) throws Exception {

        return getRunningJobExecutions(jobName, responseCode, BatchRestUtils.ADMIN_USERNAME, BatchRestUtils.ADMIN_PASS);

    }

    /**
     * @return the response - a JsonArray
     */
    protected JsonArray getRunningJobExecutions(String jobName, int responseCode, String username, String password) throws Exception {
        JsonArray jsonArray = null;
        BufferedReader br = null;

        // Get the job instance
        HttpURLConnection con = sendGetRequest("/jobservlet?action=getRunningJobExecutions&jobName=" + jobName, responseCode, "batchSecurity");

        //JSL that does not exist will trigger the catch block.
        //Assert getRunningJobExecutions call fails as designed; NoSuchJobException instead of NullPointerException
        try {
            br = HttpUtils.getConnectionStream(con);
        } catch (IOException io) {
            assertTrue(1 <= server.findStringsInLogsAndTraceUsingMark("(javax|jakarta).servlet.ServletException: (javax|jakarta).batch.operations.NoSuchJobException").size());
            verfiyNoServletNPEs();

            return jsonArray;
        }

        jsonArray = Json.createReader(br).readArray();
        br.close();

        assertEquals(0, server.findStringsInLogsAndTraceUsingMark("(javax|jakarta).servlet.ServletException: java.lang.NullPointerException").size());

        return jsonArray;
    }

    /**
     * @return the response - a JsonArray
     */
    protected JsonArray getJobNames() throws Exception {

        HttpURLConnection con = sendGetRequest("/jobservlet?action=getJobNames", HttpURLConnection.HTTP_OK, "batchSecurity");

        BufferedReader br = HttpUtils.getConnectionStream(con);
        JsonArray jsonArray = Json.createReader(br).readArray();
        br.close();

        assertEquals(1, jsonArray.size());
        verfiyNoServletNPEs();

        return jsonArray;
    }

    /**
     * @return the response - a JsonArray
     */
    protected JsonArray getJobExecutions() throws Exception {
        //String s = validJobInstance.getJsonNumber("instanceId").longValue().toString();

        HttpURLConnection con = sendGetRequest("/jobservlet?action=getJobExecutions&instanceId=" + validJobInstance.getJsonNumber("instanceId").longValue(),
                                               HttpURLConnection.HTTP_OK, "batchSecurity");

        BufferedReader br = HttpUtils.getConnectionStream(con);
        JsonArray jsonArray = Json.createReader(br).readArray();
        br.close();

        assertEquals(1, jsonArray.size());
        verfiyNoServletNPEs();

        return jsonArray;
    }

    /**
     * @return the response - a jobInstance JsonObject
     */
    protected JsonObject submitInvalidJob(String appName, String jobName) throws Exception {

        return submitInvalidJob(appName, jobName, BatchRestUtils.BATCH_BASE_URL + "jobinstances");

    }

    /**
     * @return the response - a jobInstance JsonObject
     */
    protected JsonObject submitInvalidJob(String appName, String jobName, String baseUrl) throws Exception {
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder().add("applicationName", appName).add("jobXMLName", jobName);

        return submitInvalidJob(payloadBuilder.build(), baseUrl, BatchRestUtils.ADMIN_USERNAME, BatchRestUtils.ADMIN_PASS, "");
    }

    /**
     * @return the response - a jobInstance JsonObject
     */
    protected JsonObject submitInvalidJob(String appName, String jobName, String baseUrl, String contextRoot) throws Exception {
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder().add("applicationName", appName).add("jobXMLName", jobName);

        return submitInvalidJob(payloadBuilder.build(), baseUrl, BatchRestUtils.ADMIN_USERNAME, BatchRestUtils.ADMIN_PASS, contextRoot);
    }

    /**
     * @return the response - a jobInstance JsonObject
     */
    protected JsonObject submitInvalidJob(JsonObject jobSubmitPayload, String baseUrl, String username, String password, String contextRoot) throws Exception {
        JsonObject jobInstance = null;

        log("submitInvalidJob", "Request: jobSubmitPayload= skipping");

        HttpURLConnection con = sendGetRequest(contextRoot, baseUrl, HttpURLConnection.HTTP_INTERNAL_ERROR, username, password, jobSubmitPayload);

        try {
            jobInstance = Json.createReader(con.getInputStream()).readObject();
        } catch (java.io.IOException io) {
            log("submitInvalidJob", "IOException caught");

            return jobInstance;
        }

        log("submitInvalidJob", "Response: jsonResponse= " + jobInstance.toString());
        jobInstance.getJsonNumber("instanceId"); // verifies this is a valid number

        return jobInstance;
    }

    /**
     * @return the response - a jobInstance count JsonObject
     */
    protected JsonObject getJobInstanceCount(String jobName, int responseCode) throws Exception {

        JsonObject jsonObject = null;
        BufferedReader br = null;

        // Get the job instance
        HttpURLConnection con = sendGetRequest("/jobservlet?action=getJobInstanceCount&jobName=" + jobName, responseCode, "batchSecurity");

        try {
            br = HttpUtils.getConnectionStream(con);
        } catch (IOException io) {

            assertTrue(1 <= server.findStringsInLogsAndTraceUsingMark("(javax|jakarta).servlet.ServletException: (javax|jakarta).batch.operations.NoSuchJobException").size());
            verfiyNoServletNPEs();

            return jsonObject;
        }

        jsonObject = Json.createReader(br).readObject();
        br.close();

        assertEquals(1, jsonObject.getInt("count"));
        verfiyNoServletNPEs();

        return jsonObject;

    }

    protected void verfiyNoServletNPEs() throws Exception {
        assertEquals(0, server.findStringsInLogsAndTraceUsingMark("(javax|jakarta).servlet.ServletException: java.lang.NullPointerException").size());
    }

    /**
     * Poll the REST api until the first job execution record shows up.
     *
     * @return the first job execution record for the given instanceId.
     */
    protected static JsonObject waitForFirstJobExecution(long instanceId) throws IOException, InterruptedException {

        // Wait for the job execution to show up...
        int loopCount = 0;
        JsonArray jobExecutions = null;
        do {
            Thread.sleep(1 * 1000);
            HttpURLConnection con = sendGetRequest("/jobservlet?action=getJobExecutions&instanceId=" + instanceId);
            assertEquals(BatchFatUtils.MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));

            jobExecutions = Json.createReader(con.getInputStream()).readArray();
            log("waitForFirstJobExecution", "JobExecutions: " + jobExecutions);
        } while ((jobExecutions == null || jobExecutions.isEmpty()) && ++loopCount < 60);

        assertFalse(jobExecutions == null || jobExecutions.isEmpty());

        return (JsonObject) jobExecutions.get(0);
    }

    /**
     * Send a GET request to the given uri. The uri is converted to a URL via
     * BatchFatUtils.buildUrl(server, "batchSecurity", uri).
     *
     * @return the httpUrlConnection
     */
    protected static HttpURLConnection sendGetRequest(String uri) throws IOException {
        return sendGetRequest(uri, HttpURLConnection.HTTP_OK);
    }

    protected static HttpURLConnection sendGetRequest(String uri, int responseCode) throws IOException {
        return sendGetRequest(uri, responseCode, "batchSecurity");
    }

    protected static HttpURLConnection sendGetRequest(String uri, int responseCode, String contextRoot) throws IOException {
        return sendGetRequest(contextRoot, uri, responseCode, BatchRestUtils.ADMIN_USERNAME, BatchRestUtils.ADMIN_PASS, null);
    }

    protected static HttpURLConnection sendGetRequest(String contextRoot, String uri, int responseCode, String username, String password,
                                                      JsonObject jobSubmitPayload) throws IOException {

        if (!contextRoot.isEmpty()) {
            return HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, contextRoot, uri),
                                               responseCode,
                                               new int[0],
                                               10,
                                               HTTPRequestMethod.GET,
                                               BatchRestUtils.buildHeaderMap(username, password),
                                               null);
        } else {
            return HttpUtils.getHttpConnection(restUtils.buildURL(uri),
                                               responseCode,
                                               new int[0],
                                               10,
                                               HTTPRequestMethod.GET,
                                               BatchRestUtils.buildHeaderMap(username, password),
                                               new ByteArrayInputStream(jobSubmitPayload.toString().getBytes("UTF-8")));
        }
    }

}
