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

import java.util.Properties;
import java.util.logging.Logger;

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
        params.put("numPartitions", "3");
        params.put("jobParam1", "jpValBB");
        new JobWaiter(60000).completeNewJob("CollectorPropertiesPlan", params);
    }
}
