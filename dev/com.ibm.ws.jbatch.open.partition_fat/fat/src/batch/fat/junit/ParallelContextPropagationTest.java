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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.jbatch.test.BatchAppUtils;
import com.ibm.ws.jbatch.test.FatUtils;

import batch.fat.util.BatchFATHelper;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ParallelContextPropagationTest extends BatchFATHelper {

    private static final Class testClass = ParallelContextPropagationTest.class;

    @BeforeClass
    public static void setup() throws Exception {
        server = LibertyServerFactory.getLibertyServer("batchFAT");
        BatchFATHelper.setConfig(DFLT_SERVER_XML, testClass);

        BatchAppUtils.addDropinsBatchFATWar(server);
        BatchAppUtils.addDropinsBonusPayoutWar(server);
        BatchAppUtils.addDropinsDbServletAppWar(server);

        BatchFATHelper.startServer(server, testClass);
        FatUtils.waitForSmarterPlanet(server);

        createDefaultRuntimeTables();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testPartitionJobExecId() throws Exception {
        test("ParallelPropagation", "jslName=partitionCtxPropagation&testName=testPartitionJobExecId");
    }

    @Test
    public void testPartitionJobInstanceId() throws Exception {
        test("ParallelPropagation", "jslName=partitionCtxPropagation&testName=testPartitionJobInstanceId");
    }

    @Test
    public void testPartitionStepExecId() throws Exception {
        test("ParallelPropagation", "jslName=partitionCtxPropagation&testName=testPartitionStepExecId");
    }

    @Test
    public void testSplitFlowJobExecId() throws Exception {
        test("ParallelPropagation", "jslName=splitFlowCtxPropagation&testName=testSplitFlowJobExecId");
    }

    @Test
    public void testSplitFlowJobInstanceId() throws Exception {
        test("ParallelPropagation", "jslName=splitFlowCtxPropagation&testName=testSplitFlowJobInstanceId");
    }

    @Test
    public void testSplitFlowStepExecId() throws Exception {
        test("ParallelPropagation", "jslName=splitFlowCtxPropagation&testName=testSplitFlowStepExecId");
    }

    @Test
    public void testCollectorPropertyResolver() throws Exception {
        test("ParallelPropagation", "jslName=PartitionPropertyResolverTest&testName=testCollectorPropertyResolver");
    }
}
