/*******************************************************************************
 * Copyright (c) 2022, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.junit;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.batch.runtime.BatchStatus;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jbatch.test.BatchAppUtils;
import com.ibm.ws.jbatch.test.BatchRestUtils;
import com.ibm.ws.jbatch.test.FatUtils;

import batch.fat.util.BatchFATHelper;
import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;


/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class PartitionReducerTest extends BatchFATHelper {

    private static final Class<PartitionReducerTest> testClass = PartitionReducerTest.class;

    @BeforeClass
    @SuppressWarnings("deprecation")
    public static void setup() throws Exception {
        server = LibertyServerFactory.getLibertyServer("batchFAT");
        HttpUtils.trustAllCertificates();
        BatchFATHelper.setConfig("BatchManagementEnabledTests/server.xml", testClass);

        DatabaseContainerUtil.setupDataSourceDatabaseProperties(server, FATSuite.jdbcContainer);
        server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(FATSuite.jdbcContainer).getDriverName());

        BatchRestUtils.updateDatabaseStoreIfNecessary(server, DatabaseContainerType.valueOf(FATSuite.jdbcContainer));

        BatchAppUtils.addDropinsBatchFATWar(server);
        BatchAppUtils.addDropinsDbServletAppWar(server);

        BatchFATHelper.startServer(server, testClass);
        FatUtils.waitForStartupAndSsl(server);
        FatUtils.waitForSSLKeyFile(server);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKY0011W", "CWWKY0041W", "CWWKS9582E");
        }
    }

    @AllowedFFDC({ "com.ibm.jbatch.container.exception.BatchContainerRuntimeException", "java.lang.IllegalStateException" })
    @Test
    public void testPartitionReducerMethodsForceFailure() throws Exception {

        BatchRestUtils serverUtils = new BatchRestUtils(server);
        Properties props = new Properties();
        props.setProperty("forceFailure", "true");
        JsonObject jobInstance = serverUtils.submitJob("batchFAT", "partitionSleepyBatchletWithExitStatusPartitionReducer", props, BatchRestUtils.BATCH_BASE_URL);
        long instanceId = jobInstance.getJsonNumber("instanceId").longValue();
        serverUtils.waitForJobInstanceToFinish(instanceId, BatchRestUtils.BATCH_BASE_URL);

        JsonObject jobExecution = serverUtils.getJobExecutionsMostRecentFirst(instanceId, BatchRestUtils.BATCH_BASE_URL).getJsonObject(0);
        JsonObject stepExecution = serverUtils.getStepExecutionFromExecutionIdAndStepName(jobExecution.getJsonNumber("executionId").longValue(), "step1",
                                                                                          BatchRestUtils.BATCH_BASE_URL)
                        .getJsonObject(0);

        String exitStatus = stepExecution.getString("exitStatus");

        assertTrue(exitStatus.contains("rollbackPartitionedStep"));

        assertTrue(!server.findStringsInLogs("com.ibm.jbatch.container.exception.BatchContainerRuntimeException.*Forcing failure in batchlet").isEmpty());

    }

    @Test
    public void testPartitionReducerMethods() throws Exception {

        BatchRestUtils serverUtils = new BatchRestUtils(server);
        Properties props = new Properties();
        props.setProperty("forceFailure", "false");
        JsonObject jobInstance = serverUtils.submitJob("batchFAT", "partitionSleepyBatchletWithExitStatusPartitionReducer", props, BatchRestUtils.BATCH_BASE_URL);
        long instanceId = jobInstance.getJsonNumber("instanceId").longValue();
        serverUtils.waitForJobInstanceToFinish(instanceId, BatchRestUtils.BATCH_BASE_URL);

        JsonObject jobExecution = serverUtils.getJobExecutionsMostRecentFirst(instanceId, BatchRestUtils.BATCH_BASE_URL).getJsonObject(0);
        JsonObject stepExecution = serverUtils.getStepExecutionFromExecutionIdAndStepName(jobExecution.getJsonNumber("executionId").longValue(), "step1",
                                                                                          BatchRestUtils.BATCH_BASE_URL)
                        .getJsonObject(0);

        String exitStatus = stepExecution.getString("exitStatus");
        assertTrue(exitStatus.contains("beforePartitionedStepCompletion"));

    }
    
    @Test
    public void testMultiplePartitionReducerJobs() throws Exception {

        String methodName = "testMultiplePartitionReducerJobs";

        BatchRestUtils serverUtils = new BatchRestUtils(server);
        Properties props = new Properties();
        props.setProperty("forceFailure", "false");
        
        // Submit multiple jobs
        int numJobs = 5;
        List<Long> jobInstanceIds = new ArrayList<>();
        
        for (int i = 0; i < numJobs; i++) {
            JsonObject jobInstance = serverUtils.submitJob("batchFAT", "partitionSleepyBatchletWithExitStatusPartitionReducer", props, BatchRestUtils.BATCH_BASE_URL);
            long instanceId = jobInstance.getJsonNumber("instanceId").longValue();
            jobInstanceIds.add(instanceId);
            log(methodName, "Submitted job " + (i + 1) + " with instanceId: " + instanceId);
        }
        
        // Wait for all jobs to complete
        log(methodName, "Waiting for all " + numJobs + " jobs to complete...");
        List<JsonObject> completedJobs = serverUtils.waitForJobInstancesToFinish(jobInstanceIds, BatchRestUtils.BATCH_BASE_URL);
        
        // Verify all jobs completed successfully
        for (int i = 0; i < completedJobs.size(); i++) {
            JsonObject jobInstance = completedJobs.get(i);
            long instanceId = jobInstance.getJsonNumber("instanceId").longValue();
            String batchStatus = jobInstance.getString("batchStatus");
            
            log(methodName, "Job " + instanceId + " completed with status: " + batchStatus);
            
            // Get the job execution and step execution to verify exit status
            JsonObject jobExecution = serverUtils.getJobExecutionsMostRecentFirst(instanceId, BatchRestUtils.BATCH_BASE_URL).getJsonObject(0);
            JsonObject stepExecution = serverUtils.getStepExecutionFromExecutionIdAndStepName(
                jobExecution.getJsonNumber("executionId").longValue(), 
                "step1",
                BatchRestUtils.BATCH_BASE_URL
            ).getJsonObject(0);
            
            String exitStatus = stepExecution.getString("exitStatus");
            assertTrue("Job " + instanceId + " should have 'beforePartitionedStepCompletion' in exit status", 
                       exitStatus.contains("beforePartitionedStepCompletion"));
        }
        
        log(methodName, "All " + numJobs + " jobs completed successfully");
    }

    @Test
    @AllowedFFDC({ "com.ibm.jbatch.container.exception.PersistenceException", "java.lang.IllegalArgumentException" })
    public void testStopMultiplePartitionedJobs() throws Exception {

        String methodName = "testStopMultiplePartitionedJobs";

        BatchRestUtils serverUtils = new BatchRestUtils(server);
        Properties props = new Properties();
        props.setProperty("sleep", "60");
        
        // Submit multiple jobs
        int numJobs = 5;
        List<Long> jobInstanceIds = new ArrayList<>();
        List<Long> jobExecutionIds = new ArrayList<>();
        
        for (int i = 0; i < numJobs; i++) {
            JsonObject jobInstance = serverUtils.submitJob("batchFAT", "partitionSleepyBatchletWithExitStatusPartitionReducer", props, BatchRestUtils.BATCH_BASE_URL);
            long instanceId = jobInstance.getJsonNumber("instanceId").longValue();
            jobInstanceIds.add(instanceId);
            log(methodName, "Submitted job " + (i + 1) + " with instanceId: " + instanceId);
        }
        
        // Wait for all jobs to start and get their execution IDs
        log(methodName, "Waiting for all " + numJobs + " jobs to start...");
        for (Long instanceId : jobInstanceIds) {
            JsonObject jobExecution = serverUtils.waitForFirstJobExecution(instanceId, BatchRestUtils.BATCH_BASE_URL);
            long executionId = jobExecution.getJsonNumber("executionId").longValue();
            jobExecutionIds.add(executionId);
            
            // Wait for at least one partition to start
            serverUtils.waitForAnyPartitionToStart(executionId, "step1", BatchRestUtils.BATCH_BASE_URL);
            log(methodName, "Job " + instanceId + " (execution " + executionId + ") has started");
        }
        
        // Stop all jobs
        log(methodName, "Stopping all " + numJobs + " jobs...");
        for (Long executionId : jobExecutionIds) {
            serverUtils.stopJobExecution(executionId, BatchRestUtils.BATCH_BASE_URL);
            log(methodName, "Issued stop for execution " + executionId);
        }
        
        // Wait for all jobs to stop
        log(methodName, "Waiting for all " + numJobs + " jobs to stop...");
        for (Long executionId : jobExecutionIds) {
            serverUtils.waitForJobExecutionToReachStatus(executionId, BatchRestUtils.BATCH_BASE_URL, BatchStatus.STOPPED);
            log(methodName, "Execution " + executionId + " has stopped");
        }
        
        log(methodName, "All " + numJobs + " jobs have been stopped successfully");
    }


    private static void log(String method, String msg) {
        Log.info(PartitionReducerTest.class, method, msg);
    }

}
