/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.junit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.jbatch.test.BatchAppUtils;
import com.ibm.ws.jbatch.test.FatUtils;

import batch.fat.util.BatchFATHelper;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class PreventSubJobOperationsTest extends BatchFATHelper {

    @BeforeClass
    public static void setup() throws Exception {

        BatchFATHelper.setConfig(DFLT_SERVER_XML, PreventSubJobOperationsTest.class);
        
        BatchAppUtils.addDropinsBatchFATWar(server);
        BatchAppUtils.addDropinsBonusPayoutWar(server);
        BatchAppUtils.addDropinsDbServletAppWar(server);

        server.startServer();
        FatUtils.waitForSmarterPlanet(server);

        //wait for the security keys get generated.
        FatUtils.waitForLTPA(server);

        // Standard runtime tables
        createDefaultRuntimeTables();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    @ExpectedFFDC({ "javax.batch.operations.NoSuchJobExecutionException", "com.ibm.jbatch.container.exception.BatchContainerRuntimeException", "java.lang.IllegalStateException" })
    public void testCannotOperateOnSplitFlowSubJobs() throws Exception {
        test("PreventSubJobOperations", "testName=testCannotOperateOnSplitFlowSubJobs");
    }

    @Test
    @ExpectedFFDC({ "javax.batch.operations.NoSuchJobExecutionException", "com.ibm.jbatch.container.exception.BatchContainerRuntimeException", "java.lang.IllegalStateException" })
    public void testCannotOperateOnPartitionSubJobs() throws Exception {
        test("PreventSubJobOperations", "testName=testCannotOperateOnPartitionSubJobs");
    }
}
