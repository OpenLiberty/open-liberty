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
import static org.junit.Assert.assertFalse;

import java.net.HttpURLConnection;
import java.nio.file.Paths;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.Logging;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import com.ibm.ws.jbatch.test.BatchAppUtils;
import com.ibm.ws.jbatch.test.BatchRestUtils;
import com.ibm.ws.jbatch.test.FatUtils;

import batch.fat.util.BatchJobOperatorApiUtils;
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
public class InMemoryPersistenceBatchJobOperatorApiTest extends BatchJobOperatorApiUtils {

    private static String[] expectedWarnings = { "SRVE0777E", "SRVE0315E", "CWWKY0004E" };
    private static String validJobName = "simple_partition";
    private static String invalidJobName = "ghost_job";
    private static String traceSpec = "*=info:com.ibm.jbatch.container.services.impl.MemoryPersistenceManagerImpl=all";
    private static String jobInstanceReg = "JobInstance :[\\s]*[\\d]+[\\s]* batchStatus updated : FAILED InstanceState updated : FAILED";
    private static String executionReg = "JobExecution : [\\s]*[\\d]+[\\s]* batchStatus updated : FAILED";

    @BeforeClass
    public static void beforeClass() throws Exception {

        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jbatch.fat.memory.persistence");

        HttpUtils.trustAllCertificates();

        FatUtils.checkJava7();

        BatchAppUtils.addDropinsBatchFATWar(server);
        BatchAppUtils.addDropinsBatchSecurityWar(server);
        BatchAppUtils.addDropinsDbServletAppWar(server);

        server.startServer();

        FatUtils.waitForStartupAndSsl(server);
        FatUtils.waitForRestAPI(server);

        restUtils = new BatchRestUtils(server);

        submitInitialJob();

        enableAllInMemoryTrace();

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

    /**
     * Change traceSpecification to help with verification.
     */
    private static void enableAllInMemoryTrace() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        Logging lc = config.getLogging();
        lc.setTraceSpecification(traceSpec);

        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);
        log("enableAllInMemoryTrace", "TraceSpecification set to:" + traceSpec);
    }

    /**
     * Used to mark where to start looking through trace.
     *
     * @throws Exception
     */
    public void setMarkToEndOfLogs() throws Exception {
        server.setMarkToEndOfLog(new RemoteFile[] {
                                                    server.getMostRecentTraceFile(),
                                                    server.getConsoleLogFile(),
                                                    server.getDefaultLogFile()
        });
    }

//    /**
//     * helper for simple logging.
//     */
//    private static void log(String method, Object msg) {
//        Log.info(BatchRestUtils.class, method, String.valueOf(msg));
//    }

    @Test
    @ExpectedFFDC({ "javax.servlet.ServletException",
                    "com.ibm.jbatch.container.ws.BatchSubmitInvalidParametersException" })
    @AllowedFFDC({ "java.lang.reflect.InvocationTargetException" })
    public void testGetRunningJobExecutionsWithGhostJSL() throws Exception {
        //By ghost; we mean the batch JSL doesn't exist.

        // Submit a JSL that does not exist.
        // server request should fail with Internal Error. IOException follows with a getInputStream call.
        // In memory Data Store is populated with incomplete jobInstance/Execution entry.
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
        // In memory Data Store is populated with incomplete jobInstance/Execution entry.
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
        // In memory Data Store is populated with incomplete jobInstance/Execution entry.
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
        // In memory Data Store is populated with incomplete jobInstance/Execution entry.
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

        setMarkToEndOfLogs();

        // Submit a JSL that does not exist.
        // server request should fail with Internal Error. IOException follows with a getInputStream call.
        // JPA Data Store is populated with incomplete jobInstance/Execution entry.
        submitInvalidJob("batchFAT", invalidJobName);

        //The jobinstance id isn't returned on fail. Check the logs
        verifyStartJobIsMarkedFailedInMemory();
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

        //The jobinstance id isn't returned on fail. Check the logs
        verifyStartJobIsMarkedFailedInMemory();
    }

    /**
     * Verify the job instances and execution trace log as FAILED.
     *
     * @throws Exception
     */
    private void verifyStartJobIsMarkedFailedInMemory() throws Exception {

        List<String> jlst = findMemoryTraceInLogs(jobInstanceReg);
        List<String> elst = findMemoryTraceInLogs(executionReg);

        assertFalse(jlst.isEmpty());
        logMemoryTrace("verifyStartJobIsMarkedFailedInMemory", jlst);

        assertFalse(elst.isEmpty());
        logMemoryTrace("verifyStartJobIsMarkedFailedInMemory", elst);

    }

    /**
     * Match the trace string.
     *
     * @throws Exception
     */
    private List<String> findMemoryTraceInLogs(String reg) throws Exception {

        return server.findStringsInLogsAndTraceUsingMark(reg);

    }

    /**
     * Log the trace string match.
     *
     * @throws Exception
     */
    private void logMemoryTrace(String s, List<String> lst) throws Exception {

        for (String x : lst) {
            log(s, "Trace Found:\n" + x);
        }

    }

}
