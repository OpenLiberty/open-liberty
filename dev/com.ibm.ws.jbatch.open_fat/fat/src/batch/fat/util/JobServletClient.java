/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
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

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

/**
 * Java client for interacting with the JobServlet.
 */
public class JobServletClient {

    /**
     * The server hosting JobServlet.
     */
    private final LibertyServer server;

    /**
     * The context root for JobServlet.
     */
    private final String contextRoot;

    /**
     * user and pass.
     */
    private String user;
    private String pass;

    /**
     * Indicates whether jobservlet requests should run under a UserTransaction
     */
    private boolean beginTran = false;

    /**
     * CTOR.
     */
    public JobServletClient(LibertyServer server, String contextRoot) {
        this.server = server;
        this.contextRoot = contextRoot;
    }

    /**
     * Set the user/pass to pass to the servlet.
     *
     * The JobServlet will manually create a Subject for the user/pass and run
     * the operation under that Subject via WSSubject.doAs.
     */
    public JobServletClient setUserAndPass(String user, String pass) {
        this.user = user;
        this.pass = pass;
        return this;
    }

    /**
     * Set flag indicating that all request should be run under a UserTransaction
     */
    public JobServletClient setBeginTran(boolean beginTran) {
        this.beginTran = beginTran;
        return this;
    }

    /**
     * Send a GET request to the given uri. The uri is converted to a URL via
     * BatchFatUtils.buildUrl(server, "batchSecurity", uri).
     *
     * @return the httpUrlConnection
     */
    public HttpURLConnection sendGetRequest(String uri) throws IOException {

        if (!StringUtils.isEmpty(user)) {
            uri = uri + "&user=" + user + "&password=" + pass; // assumes the uri already contains a query string (safe assumption for now).
        }

        if (beginTran) {
            uri = uri + "&beginTran=true";
        }

        return HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, contextRoot, uri),
                                           HttpURLConnection.HTTP_OK,
                                           new int[0],
                                           10,
                                           HTTPRequestMethod.GET,
                                           null,
                                           null);
    }

    /**
     * Poll the REST api until the first job execution record shows up.
     *
     * @return the first job execution record for the given instanceId.
     */
    public JsonObject waitForFirstJobExecution(long instanceId) throws IOException, InterruptedException {

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
     * Poll the job execution record until it's done.
     *
     * @return the job execution record, once it has completed.
     */
    public JsonObject waitForJobExecutionToFinish(JsonObject jobExecution) throws IOException, InterruptedException {
        int loopCount = 0;
        while (!BatchFatUtils.isDone(jobExecution) && ++loopCount < 60) {
            Thread.sleep(1 * 1000);
            HttpURLConnection con = sendGetRequest("/jobservlet?action=getJobExecution&executionId=" + jobExecution.getJsonNumber("executionId").longValue());
            assertEquals(BatchFatUtils.MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));

            jobExecution = Json.createReader(con.getInputStream()).readObject();
            log("waitForJobExecutionToFinish", "JobExecution: " + jobExecution);
        }

        assertTrue(BatchFatUtils.isDone(jobExecution));

        return jobExecution;
    }

    /**
     * Log helper.
     */
    public static void log(String method, String msg) {
        Log.info(JobServletClient.class, method, msg);
    }

}