/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

import java.util.Properties;

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
    public static void setup() throws Exception {
        server = LibertyServerFactory.getLibertyServer("batchFAT");
        HttpUtils.trustAllCertificates();
        BatchFATHelper.setConfig("BatchManagementEnabledTests/server.xml", testClass);

        BatchAppUtils.addDropinsBatchFATWar(server);
        BatchAppUtils.addDropinsBonusPayoutWar(server);
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

    @ExpectedFFDC({ "com.ibm.jbatch.container.exception.BatchContainerRuntimeException" })
    @AllowedFFDC({ "java.lang.IllegalStateException" })
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

    private static void log(String method, String msg) {
        Log.info(PartitionReducerTest.class, method, msg);
    }

}
