/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static com.ibm.ws.jbatch.test.BatchAppUtils.USE_PREBUILT;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.DataSourceProperties;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.jbatch.test.FatUtils;
import com.ibm.ws.jbatch.test.BatchAppUtils;
import com.ibm.ws.jbatch.test.dbservlet.DbServletClient;

import batch.fat.util.BatchFatUtils;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;


@RunWith(FATRunner.class)
public class BatchJobOperatorApiWithAppSecurityTest {

    protected final static String ADMIN_NAME = "adminAlice";
    protected final static String ADMIN_PASSWORD = "adminAlicePwd";
    private final static String SUBMITTER_NAME = "sarahSubmitter";
    private final static String SUBMITTER_PASSWORD = "sarahSubmitterPwd";
    private final static String SUBMITTER2_NAME = "simonSubmitter";
    private final static String SUBMITTER2_PASSWORD = "simonSubmitterPwd";
    private final static String SUBMITTER_AND_MONITOR_NAME = "submitterAndMonitor";
    private final static String SUBMITTER_AND_MONITOR_PASSWORD = "submitterAndMonitorPwd";
    private final static String MONITOR_NAME = "mindyMonitor";
    private final static String MONITOR_PASSWORD = "mindyMonitorPwd";
    private final static String NOBODY_NAME = "nancyNobody";
    private final static String NOBODY_PASSWORD = "nancyNobodyPwd";

    private static final String stepCtxBatchletJSL = "test_batchlet_stepCtx";

    private static final String sleepyBatchletJSL = "test_sleepyBatchlet";

    private static final int TIMEOUT = 10;

    public static final String HEADER_CONTENT_TYPE_KEY = "Content-Type";
    public static final String MEDIA_TYPE_APPLICATION_JSON = "application/json; charset=UTF-8";

    protected static final LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jbatch.fat");

    //Instance fields
    private static Map<String, String> adminHeaderMap, submitterHeaderMap, submitter2HeaderMap, submitterAndMonitorHeaderMap, monitorHeaderMap, nobodyHeaderMap;

    private static long jobinstance1;
    private static long jobexecution1;

    static {
        adminHeaderMap = Collections.singletonMap("Authorization", "Basic " + Base64Coder.base64Encode(ADMIN_NAME + ":" + ADMIN_PASSWORD));
        submitterHeaderMap = Collections.singletonMap("Authorization", "Basic " + Base64Coder.base64Encode(SUBMITTER_NAME + ":" + SUBMITTER_PASSWORD));
        submitter2HeaderMap = Collections.singletonMap("Authorization", "Basic " + Base64Coder.base64Encode(SUBMITTER2_NAME + ":" + SUBMITTER2_PASSWORD));
        submitterAndMonitorHeaderMap = Collections.singletonMap("Authorization",
                                                                "Basic " + Base64Coder.base64Encode(SUBMITTER_AND_MONITOR_NAME + ":" + SUBMITTER_AND_MONITOR_PASSWORD));
        monitorHeaderMap = Collections.singletonMap("Authorization", "Basic " + Base64Coder.base64Encode(MONITOR_NAME + ":" + MONITOR_PASSWORD));
        nobodyHeaderMap = Collections.singletonMap("Authorization", "Basic " + Base64Coder.base64Encode(NOBODY_NAME + ":" + NOBODY_PASSWORD));
    }

    @Rule
    public TestName name = new TestName();

    /**
     * Startup the server.
     */
    @BeforeClass
    public static void beforeClass() throws Exception {

        HttpUtils.trustAllCertificates();

        BatchAppUtils.addDropinsDbServletAppWar(server);
        BatchAppUtils.addDropinsBatchSecurityWar(server);

        server.startServer();
        FatUtils.waitForStartupAndSsl(server);

        //wait for the security keys get generated.
        FatUtils.waitForLTPA(server);

        jobinstance1 = submitJob(false);
        JsonObject jobExec = waitForFirstJobExecution(jobinstance1);
        jobexecution1 = jobExec.getJsonNumber("executionId").longValue();
    }


    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKY0011W", "SRVE0777E", "CWWKY0041W");
        }
    }

    @Test
    @ExpectedFFDC({ "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                    "java.lang.Exception" })
    public void testAbandonAsAdmin() throws Exception {
        Long jobinstance = submitJob(true);
        JsonObject jobExec = waitForFirstJobExecution(jobinstance);
        JsonObject completedJobExecution = waitForJobExecutionToFinish(jobExec);
        Long jobexecution = completedJobExecution.getJsonNumber("executionId").longValue();

        // Abandon the job...
        HttpURLConnection con = getConnection("/batchSecurity/jobservlet?action=abandon&executionId=" + jobexecution,
                                              HttpURLConnection.HTTP_OK,
                                              HTTPRequestMethod.GET,
                                              null,
                                              adminHeaderMap);
    }

    @Test
    @ExpectedFFDC({ "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                    "java.lang.Exception" })
    public void testAbandonAsSubmitter() throws Exception {
        Long jobinstance = submitJob(true);
        JsonObject jobExec = waitForFirstJobExecution(jobinstance);
        JsonObject completedJobExecution = waitForJobExecutionToFinish(jobExec);
        Long jobexecution = completedJobExecution.getJsonNumber("executionId").longValue();

        // Abandon the job...
        HttpURLConnection con = getConnection("/batchSecurity/jobservlet?action=abandon&executionId=" + jobexecution,
                                              HttpURLConnection.HTTP_OK,
                                              HTTPRequestMethod.GET,
                                              null,
                                              submitterHeaderMap);
    }

    @Test
    @ExpectedFFDC({ "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                    "java.lang.Exception" })
    public void testAbandonAsSubmitterNotOwner() throws Exception {
        Long jobinstance = submitJob(true);
        JsonObject jobExec = waitForFirstJobExecution(jobinstance);
        JsonObject completedJobExecution = waitForJobExecutionToFinish(jobExec);
        Long jobexecution = completedJobExecution.getJsonNumber("executionId").longValue();

        // Abandon the job...
        HttpURLConnection con = getConnection("/batchSecurity/jobservlet?action=abandon&executionId=" + jobexecution,
                                              HttpURLConnection.HTTP_UNAUTHORIZED,
                                              HTTPRequestMethod.GET,
                                              null,
                                              submitter2HeaderMap);

        BufferedReader br = HttpUtils.getErrorStream(con);
        String body = org.apache.commons.io.IOUtils.toString(br);
        br.close();

        Assert.assertTrue("Actual:" + body, body.contains(SUBMITTER2_NAME));
        Assert.assertTrue("Actual:" + body, body.contains("not authorized"));
    }

    @Test
    @ExpectedFFDC({ "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                    "java.lang.Exception" })
    public void testAbandonAsMonitor() throws Exception {
        Long jobinstance = submitJob(true);
        JsonObject jobExec = waitForFirstJobExecution(jobinstance);
        JsonObject completedJobExecution = waitForJobExecutionToFinish(jobExec);
        Long jobexecution = completedJobExecution.getJsonNumber("executionId").longValue();

        // Abandon the job...
        HttpURLConnection con = getConnection("/batchSecurity/jobservlet?action=abandon&executionId=" + jobexecution,
                                              HttpURLConnection.HTTP_UNAUTHORIZED,
                                              HTTPRequestMethod.GET,
                                              null,
                                              monitorHeaderMap);

        BufferedReader br = HttpUtils.getErrorStream(con);
        String body = org.apache.commons.io.IOUtils.toString(br);
        br.close();

        Assert.assertTrue("Actual:" + body, body.contains(MONITOR_NAME));
        Assert.assertTrue("Actual:" + body, body.contains("not authorized"));
    }

    @Test
    @ExpectedFFDC({ "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                    "java.lang.Exception" })
    public void testAbandonWithNoBatchRoles() throws Exception {
        Long jobinstance = submitJob(true);
        JsonObject jobExec = waitForFirstJobExecution(jobinstance);
        JsonObject completedJobExecution = waitForJobExecutionToFinish(jobExec);
        Long jobexecution = completedJobExecution.getJsonNumber("executionId").longValue();

        // Abandon the job...
        HttpURLConnection con = getConnection("/batchSecurity/jobservlet?action=abandon&executionId=" + jobexecution,
                                              HttpURLConnection.HTTP_UNAUTHORIZED,
                                              HTTPRequestMethod.GET,
                                              null,
                                              nobodyHeaderMap);

        BufferedReader br = HttpUtils.getErrorStream(con);
        String body = org.apache.commons.io.IOUtils.toString(br);
        br.close();

        Assert.assertTrue("Actual:" + body, body.contains(NOBODY_NAME));
        Assert.assertTrue("Actual:" + body, body.contains("not authorized"));
    }

    @Test
    public void testGetJobExecutionAsAdmin() throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobExecution&executionId=" + jobexecution1),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            adminHeaderMap,
                                                            null);
    }

    @Test
    public void testGetJobExecutionAsSubmitter() throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobExecution&executionId=" + jobexecution1),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            submitterHeaderMap,
                                                            null);
    }

    @Test
    @ExpectedFFDC({ "javax.batch.operations.JobSecurityException" })
    public void testGetJobExecutionAsSubmitterNotOwner() throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobExecution&executionId=" + jobexecution1),
                                                            HttpURLConnection.HTTP_UNAUTHORIZED,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            submitter2HeaderMap,
                                                            null);
        BufferedReader br = HttpUtils.getErrorStream(con);
        String body = org.apache.commons.io.IOUtils.toString(br);
        br.close();

        Assert.assertTrue("Actual:" + body, body.contains(SUBMITTER2_NAME));
        Assert.assertTrue("Actual:" + body, body.contains("not authorized"));
    }

    @Test
    public void testGetJobExecutionAsMonitor() throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobExecution&executionId=" + jobexecution1),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            monitorHeaderMap,
                                                            null);
    }

    @Test
    @ExpectedFFDC({ "javax.batch.operations.JobSecurityException" })
    public void testGetJobExecutionWithNoBatchRoles() throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobExecution&executionId=" + jobexecution1),
                                                            HttpURLConnection.HTTP_UNAUTHORIZED,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            nobodyHeaderMap,
                                                            null);

        BufferedReader br = HttpUtils.getErrorStream(con);
        String body = org.apache.commons.io.IOUtils.toString(br);
        br.close();

        Assert.assertTrue("Actual:" + body, body.contains(NOBODY_NAME));
        Assert.assertTrue("Actual:" + body, body.contains("not authorized"));
    }

    @Test
    public void testGetJobExecutionsAsAdmin() throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobExecutions&instanceId=" + jobinstance1),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            adminHeaderMap,
                                                            null);
    }

    @Test
    public void testGetJobExecutionsAsSubmitter() throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobExecutions&instanceId=" + jobinstance1),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            submitterHeaderMap,
                                                            null);
    }

    @Test
    @ExpectedFFDC({ "javax.batch.operations.JobSecurityException" })
    public void testGetJobExecutionsAsSubmitterNotOwner() throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobExecutions&instanceId=" + jobinstance1),
                                                            HttpURLConnection.HTTP_UNAUTHORIZED,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            submitter2HeaderMap,
                                                            null);

        BufferedReader br = HttpUtils.getErrorStream(con);
        String body = org.apache.commons.io.IOUtils.toString(br);
        br.close();

        Assert.assertTrue("Actual:" + body, body.contains(SUBMITTER2_NAME));
        Assert.assertTrue("Actual:" + body, body.contains("not authorized"));
    }

    @Test
    public void testGetJobExecutionsAsMonitor() throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobExecutions&instanceId=" + jobinstance1),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            monitorHeaderMap,
                                                            null);

    }

    @Test
    @ExpectedFFDC({ "javax.batch.operations.JobSecurityException" })
    public void testGetJobExecutionsWithNoBatchRoles() throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobExecutions&instanceId=" + jobinstance1),
                                                            HttpURLConnection.HTTP_UNAUTHORIZED,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            nobodyHeaderMap,
                                                            null);
        BufferedReader br = HttpUtils.getErrorStream(con);
        String body = org.apache.commons.io.IOUtils.toString(br);
        br.close();

        Assert.assertTrue("Actual:" + body, body.contains(NOBODY_NAME));
        Assert.assertTrue("Actual:" + body, body.contains("not authorized"));
    }

    @Test
    public void testGetJobInstanceAsAdmin() throws Exception {

        // Get the job instance
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobInstance&executionId=" + jobexecution1),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            adminHeaderMap,
                                                            null);

    }

    @Test
    public void testGetJobInstanceAsSubmitter() throws Exception {
        // Get the job instance
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobInstance&executionId=" + jobexecution1),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            submitterHeaderMap,
                                                            null);
    }

    @Test
    public void testGetJobInstanceAsSubmitterNotOwner() throws Exception {
        // Get the job instance
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobInstance&executionId=" + jobexecution1),
                                                            HttpURLConnection.HTTP_UNAUTHORIZED,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            submitter2HeaderMap,
                                                            null);

        BufferedReader br = HttpUtils.getErrorStream(con);
        String body = org.apache.commons.io.IOUtils.toString(br);
        br.close();

        Assert.assertTrue("Actual:" + body, body.contains(SUBMITTER2_NAME));
        Assert.assertTrue("Actual:" + body, body.contains("not authorized"));
    }

    @Test
    public void testGetJobInstanceAsMonitor() throws Exception {
        // Get the job instance
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobInstance&executionId=" + jobexecution1),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            monitorHeaderMap,
                                                            null);
    }

    @Test
    public void testGetJobInstanceWithNoBatchRoles() throws Exception {

        // Get the job instance
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobInstance&executionId=" + jobexecution1),
                                                            HttpURLConnection.HTTP_UNAUTHORIZED,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            nobodyHeaderMap,
                                                            null);

        BufferedReader br = HttpUtils.getErrorStream(con);
        String body = org.apache.commons.io.IOUtils.toString(br);
        br.close();

        Assert.assertTrue("Actual:" + body, body.contains(NOBODY_NAME));
        Assert.assertTrue("Actual:" + body, body.contains("not authorized"));
    }

    @Test
    public void testGetJobInstanceCountAsAdmin() throws Exception {
        // Get the job instance
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobInstanceCount&jobName=SecurityBatchlet"),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            adminHeaderMap,
                                                            null);

        BufferedReader br = HttpUtils.getConnectionStream(con);
        JsonObject jsonObject = Json.createReader(br).readObject();
        br.close();

        assertTrue(jsonObject.getInt("count") > 0);
    }

    @Test
    public void testGetJobInstanceCountAsSubmitter() throws Exception {
        // Get the job instance
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobInstanceCount&jobName=SecurityBatchlet"),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            submitterHeaderMap,
                                                            null);

        BufferedReader br = HttpUtils.getConnectionStream(con);
        JsonObject jsonObject = Json.createReader(br).readObject();
        br.close();

        assertTrue(jsonObject.getInt("count") > 0);
    }

    @Test
    @ExpectedFFDC({ "javax.servlet.ServletException" })
    public void testGetJobInstanceCountAsSubmitterNotOwner() throws Exception {
        // Get the job instance
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobInstanceCount&jobName=SecurityBatchlet"),
                                                            HttpURLConnection.HTTP_INTERNAL_ERROR,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            submitter2HeaderMap,
                                                            null);
    }

    @Test
    public void testGetJobInstanceCountAsMonitor() throws Exception {
        // Get the job instance
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobInstanceCount&jobName=SecurityBatchlet"),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            monitorHeaderMap,
                                                            null);

        BufferedReader br = HttpUtils.getConnectionStream(con);
        JsonObject jsonObject = Json.createReader(br).readObject();
        br.close();

        assertTrue(jsonObject.getInt("count") > 0);
    }

    @Test
    public void testGetJobInstanceCountWithNoBatchRoles() throws Exception {
        // Get the job instance
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobInstanceCount&jobName=SecurityBatchlet"),
                                                            HttpURLConnection.HTTP_UNAUTHORIZED,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            nobodyHeaderMap,
                                                            null);

        BufferedReader br = HttpUtils.getErrorStream(con);
        String body = org.apache.commons.io.IOUtils.toString(br);
        br.close();

        Assert.assertTrue("Actual:" + body, body.contains(NOBODY_NAME));
        Assert.assertTrue("Actual:" + body, body.contains("not authorized"));
    }

    @Test
    public void testGetJobInstancesAsAdmin() throws Exception {
        // Get the job instances
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobInstances&jobName=SecurityBatchlet"),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            adminHeaderMap,
                                                            null);
    }

    @Test
    public void testGetJobInstancesAsSubmitter() throws Exception {
        // Get the job instances
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobInstances&jobName=SecurityBatchlet"),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            submitterHeaderMap,
                                                            null);
    }

    @Test
    @ExpectedFFDC({ "javax.servlet.ServletException", "javax.batch.operations.NoSuchJobException" })
    public void testGetJobInstancesAsSubmitterNotOwner() throws Exception {
        // Get the job instances
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobInstances&jobName=SecurityBatchlet"),
                                                            HttpURLConnection.HTTP_INTERNAL_ERROR,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            submitter2HeaderMap,
                                                            null);

    }

    @Test
    public void testGetJobInstancesAsMonitor() throws Exception {
        // Get the job instances
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobInstances&jobName=SecurityBatchlet"),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            monitorHeaderMap,
                                                            null);
    }

    @Test
    public void testGetJobInstancesWithNoBatchRoles() throws Exception {
        // Get the job instances
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobInstances&jobName=SecurityBatchlet"),
                                                            HttpURLConnection.HTTP_UNAUTHORIZED,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            nobodyHeaderMap,
                                                            null);
        BufferedReader br = HttpUtils.getErrorStream(con);
        String body = org.apache.commons.io.IOUtils.toString(br);
        br.close();

        Assert.assertTrue("Actual:" + body, body.contains(NOBODY_NAME));
        Assert.assertTrue("Actual:" + body, body.contains("not authorized"));
    }

    @Test
    public void testGetJobNamesAsAdmin() throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobNames"),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            adminHeaderMap,
                                                            null);

        BufferedReader br = HttpUtils.getConnectionStream(con);
        JsonArray jsonArray = Json.createReader(br).readArray();
        br.close();

        assertTrue(jsonArray.size() > 0);

    }

    @Test
    public void testGetJobNamesAsSubmitter() throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobNames"),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            submitterHeaderMap,
                                                            null);

        BufferedReader br = HttpUtils.getConnectionStream(con);
        JsonArray jsonArray = Json.createReader(br).readArray();
        br.close();

        assertTrue(jsonArray.size() > 0);

    }

    @Test
    public void testGetJobNamesAsSubmitterNotOwner() throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobNames"),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            submitter2HeaderMap,
                                                            null);

        BufferedReader br = HttpUtils.getConnectionStream(con);
        JsonArray jsonArray = Json.createReader(br).readArray();
        br.close();

        assertTrue(jsonArray.size() == 0);
    }

    @Test
    public void testGetJobNamesAsMonitor() throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobNames"),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            monitorHeaderMap,
                                                            null);

        BufferedReader br = HttpUtils.getConnectionStream(con);
        JsonArray jsonArray = Json.createReader(br).readArray();
        br.close();

        assertTrue(jsonArray.size() > 0);
    }

    @Test
    public void testGetJobNamesWithNoBatchRoles() throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getJobNames"),
                                                            HttpURLConnection.HTTP_UNAUTHORIZED,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            nobodyHeaderMap,
                                                            null);

        BufferedReader br = HttpUtils.getErrorStream(con);
        String body = org.apache.commons.io.IOUtils.toString(br);
        br.close();

        Assert.assertTrue("Actual:" + body, body.contains(NOBODY_NAME));
        Assert.assertTrue("Actual:" + body, body.contains("not authorized"));
    }

    @Test
    public void testGetParametersAsAdmin() throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getParameters&executionId=" + jobexecution1),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            adminHeaderMap,
                                                            null);
    }

    @Test
    public void testGetParametersAsSubmitter() throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getParameters&executionId=" + jobexecution1),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            submitterHeaderMap,
                                                            null);
    }

    @Test
    public void testGetParametersAsSubmitterNotOwner() throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getParameters&executionId=" + jobexecution1),
                                                            HttpURLConnection.HTTP_UNAUTHORIZED,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            submitter2HeaderMap,
                                                            null);
        BufferedReader br = HttpUtils.getErrorStream(con);
        String body = org.apache.commons.io.IOUtils.toString(br);
        br.close();

        Assert.assertTrue("Actual:" + body, body.contains(SUBMITTER2_NAME));
        Assert.assertTrue("Actual:" + body, body.contains("not authorized"));
    }

    @Test
    public void testGetParametersAsMonitor() throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getParameters&executionId=" + jobexecution1),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            monitorHeaderMap,
                                                            null);
    }

    @Test
    public void testGetParametersWithNoBatchRoles() throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=getParameters&executionId=" + jobexecution1),
                                                            HttpURLConnection.HTTP_UNAUTHORIZED,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            nobodyHeaderMap,
                                                            null);

        BufferedReader br = HttpUtils.getErrorStream(con);
        String body = org.apache.commons.io.IOUtils.toString(br);
        br.close();

        Assert.assertTrue("Actual:" + body, body.contains(NOBODY_NAME));
        Assert.assertTrue("Actual:" + body, body.contains("not authorized"));
    }

    public void testGetRunningExecutionsAsAdmin() throws Exception {

    }

    public void testGetRunningExecutionsAsSubmitter() throws Exception {

    }

    public void testGetRunningExecutionsAsSubmitterNotOwner() throws Exception {

    }

    public void testGetRunningExecutionsAsMonitor() throws Exception {

    }

    public void testGetRunningExecutionsWithNoBatchRoles() throws Exception {

    }

    public void testGetStepExecutionsAsAdmin() throws Exception {

    }

    public void testGetStepExecutionsAsSubmitter() throws Exception {

    }

    public void testGetStepExecutionsAsSubmitterNotOwner() throws Exception {

    }

    public void testGetStepExecutionsAsMonitor() throws Exception {

    }

    public void testGetStepExecutionsWithNoBatchRoles() throws Exception {

    }

    @Test
    @ExpectedFFDC({ "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                    "java.lang.Exception" })
    public void testRestartAsAdmin() throws Exception {
        Long jobinstance = submitJob(true);
        JsonObject jobExec = waitForFirstJobExecution(jobinstance);
        JsonObject completedJobExecution = waitForJobExecutionToFinish(jobExec);
        Long jobexecution = completedJobExecution.getJsonNumber("executionId").longValue();

        // Restart the job...
        HttpURLConnection con = getConnection("/batchSecurity/jobservlet?action=restart&executionId=" + jobexecution,
                                              HttpURLConnection.HTTP_OK,
                                              HTTPRequestMethod.GET,
                                              null,
                                              adminHeaderMap);
    }

    @Test
    @ExpectedFFDC({ "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                    "java.lang.Exception" })
    public void testRestartAsSubmitter() throws Exception {
        Long jobinstance = submitJob(true);
        JsonObject jobExec = waitForFirstJobExecution(jobinstance);
        JsonObject completedJobExecution = waitForJobExecutionToFinish(jobExec);
        Long jobexecution = completedJobExecution.getJsonNumber("executionId").longValue();

        // Restart the job...
        HttpURLConnection con = getConnection("/batchSecurity/jobservlet?action=restart&executionId=" + jobexecution,
                                              HttpURLConnection.HTTP_OK,
                                              HTTPRequestMethod.GET,
                                              null,
                                              submitterHeaderMap);
    }

    @Test
    @ExpectedFFDC({ "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                    "java.lang.Exception" })
    public void testRestartAsSubmitterNotOwner() throws Exception {
        Long jobinstance = submitJob(true);
        JsonObject jobExec = waitForFirstJobExecution(jobinstance);
        JsonObject completedJobExecution = waitForJobExecutionToFinish(jobExec);
        Long jobexecution = completedJobExecution.getJsonNumber("executionId").longValue();

        // Restart the job...
        HttpURLConnection con = getConnection("/batchSecurity/jobservlet?action=restart&executionId=" + jobexecution,
                                              HttpURLConnection.HTTP_UNAUTHORIZED,
                                              HTTPRequestMethod.GET,
                                              null,
                                              submitter2HeaderMap);

        BufferedReader br = HttpUtils.getErrorStream(con);
        String body = org.apache.commons.io.IOUtils.toString(br);
        br.close();

        Assert.assertTrue("Actual:" + body, body.contains(SUBMITTER2_NAME));
        Assert.assertTrue("Actual:" + body, body.contains("not authorized"));
    }

    @Test
    @ExpectedFFDC({ "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                    "java.lang.Exception" })
    public void testRestartAsMonitor() throws Exception {
        Long jobinstance = submitJob(true);
        JsonObject jobExec = waitForFirstJobExecution(jobinstance);
        JsonObject completedJobExecution = waitForJobExecutionToFinish(jobExec);
        Long jobexecution = completedJobExecution.getJsonNumber("executionId").longValue();

        // Restart the job...
        HttpURLConnection con = getConnection("/batchSecurity/jobservlet?action=restart&force.failure=true&executionId=" + jobexecution,
                                              HttpURLConnection.HTTP_UNAUTHORIZED,
                                              HTTPRequestMethod.GET,
                                              null,
                                              monitorHeaderMap);

        BufferedReader br = HttpUtils.getErrorStream(con);
        String body = org.apache.commons.io.IOUtils.toString(br);
        br.close();

        Assert.assertTrue("Actual:" + body, body.contains(MONITOR_NAME));
        Assert.assertTrue("Actual:" + body, body.contains("not authorized"));
    }

    @Test
    @ExpectedFFDC({ "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                    "java.lang.Exception" })
    public void testRestartWithNoBatchRoles() throws Exception {
        Long jobinstance = submitJob(true);
        JsonObject jobExec = waitForFirstJobExecution(jobinstance);
        JsonObject completedJobExecution = waitForJobExecutionToFinish(jobExec);
        Long jobexecution = completedJobExecution.getJsonNumber("executionId").longValue();

        // Restart the job...
        HttpURLConnection con = getConnection("/batchSecurity/jobservlet?action=restart&force.failure=true&executionId=" + jobexecution,
                                              HttpURLConnection.HTTP_UNAUTHORIZED,
                                              HTTPRequestMethod.GET,
                                              null,
                                              nobodyHeaderMap);

        BufferedReader br = HttpUtils.getErrorStream(con);
        String body = org.apache.commons.io.IOUtils.toString(br);
        br.close();

        Assert.assertTrue("Actual:" + body, body.contains(NOBODY_NAME));
        Assert.assertTrue("Actual:" + body, body.contains("not authorized"));
    }

    @Test
    public void testStartAsAdmin() throws Exception {
        // Submit the job...
        HttpURLConnection con = getConnection("/batchSecurity/jobservlet?action=start&jobXMLName=SecurityBatchlet",
                                              HttpURLConnection.HTTP_OK,
                                              HTTPRequestMethod.GET,
                                              null,
                                              adminHeaderMap);
    }

    @Test
    public void testStartAsSubmitter() throws Exception {
        // Submit the job...
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=start&jobXMLName=SecurityBatchlet"),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            submitterHeaderMap,
                                                            null);

    }

    @Test
    @ExpectedFFDC({ "javax.batch.operations.JobSecurityException" })
    public void testStartAsMonitor() throws Exception {

        // Submit the job...
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=start&jobXMLName=SecurityBatchlet"),
                                                            HttpURLConnection.HTTP_UNAUTHORIZED,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            monitorHeaderMap,
                                                            null);

        BufferedReader br = HttpUtils.getErrorStream(con);
        String body = org.apache.commons.io.IOUtils.toString(br);
        br.close();

        Assert.assertTrue("Actual:" + body, body.contains(MONITOR_NAME));
        Assert.assertTrue("Actual:" + body, body.contains("not authorized"));

    }

    @Test
    @ExpectedFFDC({ "javax.batch.operations.JobSecurityException" })
    public void testStartWithNoBatchRoles() throws Exception {
        // Submit the job...
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=start&jobXMLName=SecurityBatchlet"),
                                                            HttpURLConnection.HTTP_UNAUTHORIZED,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            nobodyHeaderMap,
                                                            null);
    }

    public void testStopAsAdmin() throws Exception {

    }

    public void testStopAsSubmitter() throws Exception {

    }

    public void testStopAsSubmitterNotOwner() throws Exception {

    }

    public void testStopAsMonitor() throws Exception {

    }

    public void testStopWithNoBatchRoles() throws Exception {

    }

    private static String getPort() {
        return System.getProperty("HTTP_default.secure", "8020");
    }

    private static URL getURL(String path) throws MalformedURLException {
        URL myURL = new URL("https://localhost:" + getPort() + path);
        System.out.println("Built URL: " + myURL.toString());
        return myURL;
    }

    protected static HttpURLConnection getConnection(String path, int expectedResponseCode, HTTPRequestMethod method, InputStream streamToWrite,
                                                     Map<String, String> map) throws IOException {
        return HttpUtils.getHttpConnection(getURL(path), expectedResponseCode, new int[0], TIMEOUT, method, map, streamToWrite);
    }

    /**
     * @return BASE64-encoded user:pass value for Basic Authorization header.
     */
    private String encodeAuthzHeader(String user, String pass) {
        return "Basic " + Base64Coder.base64Encode(user + ":" + pass);
    }

    /**
     * Similar to LibertyServer.getFileFromLibertyServerRoot, except it returns
     * a File, not RemoteFile.
     *
     * @param relativePath, should begin with "/".
     *
     * @return a file handle to the given file under the server dir
     */
    private File getFileFromLibertyServerRoot(LibertyServer server, String relativePath) {
        return new File(server.getServerRoot() + relativePath);
    }

    /**
     * helper for simple logging.
     */
    private static void log(String method, String msg) {
        Log.info(BatchJobOperatorApiWithAppSecurityTest.class, method, msg);
    }

    private static void reloadApp(LibertyServer server, File appFile, String appName) throws IOException {
        assertTrue(appFile.exists());

        long lastModified = appFile.lastModified();
        log("reloadApp", "lastmodified: " + lastModified);
        boolean success = appFile.setLastModified(System.currentTimeMillis());
        if (!success) {
            assertEquals(lastModified, appFile.lastModified());

            // Hack for updating last modified.
            RandomAccessFile raf = new RandomAccessFile(appFile, "rw");
            long length = raf.length();
            raf.setLength(length + 1);
            raf.setLength(length);
            raf.close();
        }

        assertTrue(lastModified != appFile.lastModified());

        log("reloadApp", "new lastmodified: " + appFile.lastModified());

        assertNotNull("The server did not report the app was updated",
                      server.waitForStringInLog("CWWKZ0003I: The application " + appName));
    }

    public static String executeSql(LibertyServer server, String dataSourceJndi, String sql) throws Exception {

        String userName = "user";
        String password = "pass";

        ServerConfiguration configuration = server.getServerConfiguration();
        ConfigElementList<DataSource> dataSourcesList = configuration.getDataSources();
        Iterator<DataSource> dataSourcesListIterator = dataSourcesList.iterator();

        while (dataSourcesListIterator.hasNext()) {
            DataSource dataSource = dataSourcesListIterator.next();

            if (dataSource.getJndiName().equals(dataSourceJndi)) {
                Set<DataSourceProperties> dataSourcePropertiesList = dataSource.getDataSourceProperties();
                Iterator<DataSourceProperties> dataSourcePropertiesListIterator = dataSourcePropertiesList.iterator();

                while (dataSourcePropertiesListIterator.hasNext()) {
                    DataSourceProperties dataSourceProperties = dataSourcePropertiesListIterator.next();
                    userName = dataSourceProperties.getUser();
                    password = dataSourceProperties.getPassword();
                    break;
                }
            }

            if (!userName.equals("user"))
                break;
        }

        return new DbServletClient().setDataSourceJndi(dataSourceJndi).setDataSourceUser(userName, password).setHostAndPort(server.getHostname(),
                                                                                                                            server.getHttpDefaultPort()).setSql(sql).executeQuery();
    }

    public static URL buildUrl(LibertyServer server, String contextRoot, String uri) throws IOException {
        return new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + contextRoot + uri);
    }

    private void logReaderContents(String method, String prefix, BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        String nl = "";
        while ((line = reader.readLine()) != null) {
            sb.append(nl);
            sb.append(line);
            nl = System.getProperty("line.separator");
        }
        log(method, prefix + sb.toString());
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
     * Poll the job execution record until it's done.
     *
     * @return the job execution record, once it has completed.
     */
    protected JsonObject waitForJobExecutionToFinish(JsonObject jobExecution) throws IOException, InterruptedException {
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
     * Send a GET request to the given uri. The uri is converted to a URL via
     * BatchFatUtils.buildUrl(server, "batchSecurity", uri).
     *
     * @return the httpUrlConnection
     */
    protected static HttpURLConnection sendGetRequest(String uri) throws IOException {
        return HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", uri),
                                           HttpURLConnection.HTTP_OK,
                                           new int[0],
                                           10,
                                           HTTPRequestMethod.GET,
                                           adminHeaderMap,
                                           null);
    }

    /**
     * @return the instance id of the job
     * @throws Exception
     */
    protected static long submitJob(boolean forceFailure) throws Exception {
        String forceFailureParam = "";

        if (forceFailure == true) {
            forceFailureParam = "&force.failure=true";
        }

        // Submit the job...
        HttpURLConnection con = HttpUtils.getHttpConnection(BatchFatUtils.buildUrl(server, "batchSecurity", "/jobservlet?action=start&jobXMLName=SecurityBatchlet"
                                                                                                            + forceFailureParam),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10,
                                                            HTTPRequestMethod.GET,
                                                            submitterHeaderMap,
                                                            null);

        BufferedReader br = HttpUtils.getConnectionStream(con);
        JsonObject jsonResponse = Json.createReader(br).readObject();
        br.close();

        //Verify that the proper mappings are returned
        long instanceId = jsonResponse.getJsonNumber("instanceId").longValue(); //verifies this is a valid number

        return instanceId;

    }

}
