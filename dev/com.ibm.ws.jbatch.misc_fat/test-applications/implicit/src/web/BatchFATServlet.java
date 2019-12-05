/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import static org.junit.Assert.assertEquals;

import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.runtime.JobExecution;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import fat.util.JobWaiter;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/FATServlet")
public class BatchFATServlet extends FATServlet {

    public static Logger logger = Logger.getLogger("test");

    public static final String SP_VAL = "spVal";

    @Test
    public void testParameterizedCollectorMapper() throws Exception {
        logger.fine("Running test = testParameterizedCollectorMapper");

        Properties params = new Properties();
        params.put("numPartitions", "4");
        params.put("jobParam1", "jpValAA");
        new JobWaiter(60000).completeNewJob("CollectorPropertiesMapper", params);
    }

    @Test
    public void testParameterizedCollectorPlan() throws Exception {
        logger.fine("Running test = testParameterizedCollectorPlan");

        Properties params = new Properties();
        params.put("numPartitions", "3"); // For artifacts validating against number of partitions
        params.put("jobParam1", "jpValBB");
        new JobWaiter(60000).completeNewJob("CollectorPropertiesPlan", params);
    }

    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC({ "com.ibm.jbatch.container.exception.BatchContainerRuntimeException", "java.lang.IllegalStateException" })
    public void testDeserializeArrayCheckpoint() throws Exception {
        logger.fine("Running test = testDeserializeArrayCheckpoint");

        Properties params = new Properties();
        params.put("forceFailure", "11");

        new JobWaiter(60000).completeNewJobWithRestart("ArrayCheckpointDeserialize", params, 1);
    }

    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC({ "com.ibm.jbatch.container.exception.BatchContainerRuntimeException", "java.lang.IllegalStateException" })
    public void testDeserializeArrayUserData() throws Exception {
        logger.fine("Running test = testDeserializeArrayUserData");

        Properties params = new Properties();
        params.put("forceFailure", "11");
        params.put("userDataTest", "true");

        new JobWaiter(60000).completeNewJobWithRestart("ArrayCheckpointDeserialize", params, 1);
    }

    @Test
    @Mode(TestMode.FULL)
    public void testDeserializeArrayCollectorData() throws Exception {
        logger.fine("Running test = testDeserializeArrayUserData");

        new JobWaiter(60000).completeNewJob("ArrayUserDataDeserialize", null);
    }

    @Test
    @Mode(TestMode.FULL)
    public void testMapperZeroPartitions() throws Exception {
        logger.fine("Running test = testMapperZeroPartitions");

        Properties params = new Properties();
        params.put("numPartitions", "0");
        params.put("jobParam1", "jpValAA");
        new JobWaiter(60000).completeNewJob("CollectorPropertiesMapper", params);
    }

    @Test
    @Mode(TestMode.FULL)
    public void testPlanZeroPartitions() throws Exception {
        logger.fine("Running test = testPlanZeroPartitions");

        Properties params = new Properties();
        params.put("jobParam1", "jpValAA");
        params.put("sp", SP_VAL);
        params.put("numPartitions", "0"); // For the other artifacts like the StepListener validating against number of partitions
        JobExecution jobExec = new JobWaiter(60000).completeNewJob("ZeroPartitionPlan", params);
        assertEquals("Unexpected exit status", SP_VAL + "," + "COMMIT", jobExec.getExitStatus());
    }

    @Test
    @Mode(TestMode.FULL)
    public void testReducerCommit() throws Exception {
        logger.fine("Running test = testReducerCommit");

        Properties params = new Properties();
        params.put("jobParam1", "jpValAA");
        params.put("sp", SP_VAL);
        params.put("numPartitions", "3"); // For artifacts validating against number of partitions
        JobExecution jobExec = new JobWaiter(60000).completeNewJob("CollectorPropertiesPlan", params);
        assertEquals("Unexpected exit status", SP_VAL + "," + "COMMIT", jobExec.getExitStatus());
    }

    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC({ "com.ibm.jbatch.container.exception.BatchContainerRuntimeException", "java.lang.IllegalStateException" })
    public void testReducerRollback() throws Exception {
        logger.fine("Running test = testReducerRollback");

        Properties params = new Properties();
        params.put("jobParam1", "jpValAA");
        params.put("sp", SP_VAL);
        params.put("numPartitions", "3"); // For artifacts validating against number of partitions
        params.put("failOn", "2");
        params.put("doEndOfStepValidation", "false"); // Since we're forcing a failure we don't want to do this validation
        JobExecution jobExec = new JobWaiter(60000).submitExpectedFailingJob("CollectorPropertiesPlan", params);
        assertEquals("Unexpected exit status", SP_VAL + "," + "ROLLBACK", jobExec.getExitStatus());
    }

}
