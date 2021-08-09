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
package batch.fat.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.nio.file.Paths;

import javax.batch.runtime.BatchStatus;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.jbatch.test.BatchAppUtils;
import com.ibm.ws.jbatch.test.BatchRestUtils;
import com.ibm.ws.jbatch.test.FatUtils;

import batch.fat.util.BatchJobOperatorApiUtils;
import batch.fat.util.InstanceStateMirrorImage;
import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JPAPersistenceBatchJobOperatorApiTest extends BatchJobOperatorApiUtils {

    private static String[] expectedWarnings = { "SRVE0777E", "SRVE0315E", "CWWKY0004E" };
    private static String validJobName = "simple_partition";
    private static String invalidJobName = "ghost_job";

    @BeforeClass
    public static void beforeClass() throws Exception {

        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jbatch.fat.jpa.persistence");

        HttpUtils.trustAllCertificates();

        FatUtils.checkJava7();

        BatchAppUtils.addDropinsBatchFATWar(server);
        BatchAppUtils.addDropinsBatchSecurityWar(server);
        BatchAppUtils.addDropinsDbServletAppWar(server);

        server.startServer();

        FatUtils.waitForStartupSslAndLTPA(server);

        restUtils = new BatchRestUtils(server);

        submitInitialJob();

    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server.isStarted()) {
            server.stopServer(expectedWarnings);
        }
    }

    private static void submitInitialJob() throws Exception {
        //Submit a Valid Batch JSL.
        validJobInstance = restUtils.submitJob("batchFAT", validJobName, BatchRestUtils.BATCH_BASE_URL);
        validJobInstance = restUtils.waitForJobInstanceToFinish(validJobInstance.getJsonNumber("instanceId").longValue(), BatchRestUtils.BATCH_BASE_URL);

        validExecutionRecord = waitForFirstJobExecution(validJobInstance.getJsonNumber("instanceId").longValue());

        assertEquals("COMPLETED", validJobInstance.getString("batchStatus"));
    }

    @Test
    @ExpectedFFDC({ "javax.servlet.ServletException",
                    "com.ibm.jbatch.container.ws.BatchSubmitInvalidParametersException" })
    @AllowedFFDC({ "java.lang.reflect.InvocationTargetException" })
    public void testGetRunningJobExecutionsWithGhostJSL() throws Exception {
        //By ghost; we mean the batch JSL doesn't exist.

        // Submit a JSL that does not exist.
        // server request should fail with Internal Error. IOException follows with a getInputStream call.
        // JPA Data Store is populated with incomplete jobInstance/Execution entry.
        // getRunningExecution will also fail with Internal Error.
        // Check that the trace/server logs represent the failures as expected. (No Servlet NPE's).
        submitInvalidJob("batchFAT", invalidJobName);
        getRunningJobExecutions(invalidJobName, HttpURLConnection.HTTP_INTERNAL_ERROR);

    }

    @Test
    @ExpectedFFDC({ "com.ibm.jbatch.container.ws.BatchSubmitInvalidParametersException" })
    @AllowedFFDC({ "java.lang.reflect.InvocationTargetException" })
    public void testGetJobNamesWithGhostJSL() throws Exception {
        //By ghost; we mean the batch JSL doesn't exist.

        // Submit a JSL that does not exist.
        // server request should fail with Internal Error. IOException follows with a getInputStream call.
        // JPA Data Store is populated with incomplete jobInstance/Execution entry.
        // getJobNames request will only return valid JobInstance/Execution entries.
        submitInvalidJob("batchFAT", invalidJobName);
        getJobNames();

    }

    @Test
    @ExpectedFFDC({ "com.ibm.jbatch.container.ws.BatchSubmitInvalidParametersException" })
    @AllowedFFDC({ "java.lang.reflect.InvocationTargetException" })
    public void testGetJobExecutionsWithGhostJSL() throws Exception {
        //By ghost; we mean the batch JSL doesn't exist.

        // Submit a JSL that does not exist.
        // server request should fail with Internal Error. IOException follows with a getInputStream call.
        // JPA Data Store is populated with incomplete jobInstance/Execution entry.
        // getJobNames request will only return valid JobInstance/Execution entries.
        submitInvalidJob("batchFAT", invalidJobName);
        getJobExecutions();

    }

    @Test
    @ExpectedFFDC({ "javax.servlet.ServletException",
                    "com.ibm.jbatch.container.ws.BatchSubmitInvalidParametersException" })
    @AllowedFFDC({ "java.lang.reflect.InvocationTargetException" })
    public void testGetJobInstanceCountWithGhostJSL() throws Exception {
        //By ghost; we mean the batch JSL doesn't exist.

        // Submit a JSL that does not exist.
        // server request should fail with Internal Error. IOException follows with a getInputStream call.
        // JPA Data Store is populated with incomplete jobInstance/Execution entry.
        submitInvalidJob("batchFAT", invalidJobName);

        //Should run without problem.
        getJobInstanceCount(validJobName, HttpURLConnection.HTTP_OK);
        //Should fail without NPE.
        getJobInstanceCount(invalidJobName, HttpURLConnection.HTTP_INTERNAL_ERROR);

    }

    @Test
    @ExpectedFFDC({ "com.ibm.jbatch.container.ws.BatchSubmitInvalidParametersException" })
    @AllowedFFDC({ "java.lang.reflect.InvocationTargetException" })
    public void testVerifyGhostJSLMarkedFailed() throws Exception {
        //By ghost; we mean the batch JSL doesn't exist.

        // Submit a JSL that does not exist.
        // server request should fail with Internal Error. IOException follows with a getInputStream call.
        // JPA Data Store is populated with incomplete jobInstance/Execution entry.
        // submitInvalidJob("batchFAT", invalidJobName, BatchRestUtils.BATCH_BASE_URL);
        submitInvalidJob("batchFAT", invalidJobName);

        //The jobinstance id isn't returned. But can do direct queries.
        verifyStartFailedDBEntries();
    }

    @Test
    @ExpectedFFDC({ "javax.servlet.ServletException",
                    "com.ibm.jbatch.container.ws.BatchSubmitInvalidParametersException",
                    "javax.batch.operations.JobStartException" })
    public void testSubmitInvalidJobViaJobOperator() throws Exception {
        //Use servlet to Submit an invalid Batch JSL to JobOperatorImpl instead of Rest interface.

        // Submit a JSL that does not exist.
        // server request should fail with Internal Error. IOException follows with a getInputStream call.
        // JPA Data Store is populated with incomplete jobInstance/Execution entry.

        submitInvalidJob("batchFAT", invalidJobName, "/jobservlet?action=start&jobXMLName=" + invalidJobName, "batchSecurity");
        verifyStartFailedDBEntries();
    }

    /**
     * Verify the DB has all but 1st job instances and execution marked FAILED.
     *
     * @throws Exception
     */
    private void verifyStartFailedDBEntries() throws Exception {
        String schema = server.getServerConfiguration().getDatabaseStores().get(0).getSchema();

        //All but first job instance should be marked failed.
        String queryExecution = "SELECT BATCHSTATUS FROM " + schema + ".JOBEXECUTION WHERE FK_JOBINSTANCEID > 1";
        String queryInstance = "SELECT BATCHSTATUS,instancestate FROM " + schema + ".JOBINSTANCE WHERE JOBINSTANCEID > 1";

        String instanceResponse = restUtils.executeSql(server, "jdbc/batch", queryInstance);

        assertTrue(verifyStatusMatches(instanceResponse, BatchStatus.FAILED, InstanceStateMirrorImage.FAILED));

        String executionResponse = restUtils.executeSql(server, "jdbc/batch", queryExecution);
        assertTrue(verifyStatusMatches(executionResponse, BatchStatus.FAILED, null));

        log("verifyStartFailedDBEntries", "Success");
    }

    private boolean verifyStatusMatches(String resp, BatchStatus bs, InstanceStateMirrorImage ismi) {
        String[] s = resp.split(",");
        for (String row : s) {
            log("verifyStatusMatches", row);

            if (bs != null && ismi != null) {
                if (!(row.contains(bs.ordinal() + "|" + ismi.ordinal()))) {
                    return false;
                }
            } else {
                if (!(row.contains(String.valueOf(bs.ordinal())))) {
                    return false;
                }
            }

        }
        return true;
    }

}
