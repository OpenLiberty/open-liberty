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
import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class PartitionMetricsTest extends BatchFATHelper {

    private static final Class testClass = PartitionMetricsTest.class;

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
            server.stopServer("CWWKY0011W");
        }
    }

    @Test
    @AllowedFFDC({
                   "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                   "java.lang.RuntimeException"
    })
    public void testMetricsRerunStep() throws Exception {
        test("PartitionMetrics", "testName=testMetricsRerunStep");
    }

    @Test
    @AllowedFFDC({
                   "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                   "java.lang.RuntimeException"
    })
    public void testPartitionMetrics() throws Exception {
        test("PartitionMetrics", "testName=testPartitionMetrics");
    }

    @Test
    @AllowedFFDC({
                   "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                   "java.lang.RuntimeException"
    })
    public void testNestedSplitFlowPartitionMetrics() throws Exception {
        test("PartitionMetrics", "testName=testNestedSplitFlowPartitionMetrics");
    }

    @Test
    @AllowedFFDC({
                   "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                   "java.lang.RuntimeException"
    })
    public void testPartitionedRollbackMetric() throws Exception {
        test("PartitionMetrics", "testName=testPartitionedRollbackMetric");
    }

}
