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

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
// Seems unlikely to be affected by typical routine changes
public class MiscTest extends BatchFATHelper {

    private static final Class testClass = MiscTest.class;

    @BeforeClass
    public static void setup() throws Exception {
        BatchFATHelper.setConfig(DFLT_SERVER_XML, testClass);

        BatchAppUtils.addDropinsBatchFATWar(server);
        BatchAppUtils.addDropinsBonusPayoutWar(server);
        BatchAppUtils.addDropinsDbServletAppWar(server);

        BatchFATHelper.startServer(server, testClass);
        FatUtils.waitForSmarterPlanet(server);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKY0011W");
        }
    }

    @Test
    public void testPartitionProps() throws Exception {
        test("Basic", "jslName=PartitionProps");
    }

    @Test
    public void testNullPropOnJobExecution() throws Exception {
        test("NullPropOnJobExecutionServlet", "jslName=NullPropOnJobExec&testName=testNullPropOnJobExecution");
    }

    @Test
    public void testLastUpdatedJobExecution() throws Exception {
        test("LastUpdatedJobExecutionServlet", "jslName=LastUpdatedJobExec&testName=testLastUpdatedJobExecution");
    }

    @Test
    @ExpectedFFDC("java.lang.IllegalStateException")
    public void testFlowTransitionIllegal() throws Exception {
        test("FlowTransitionIllegalServlet", "jslName=flowTransitionIllegal&testName=flowTransitionIllegal");
    }

    @Test
    @ExpectedFFDC({ "com.ibm.jbatch.container.exception.BatchContainerRuntimeException", "batch.fat.common.util.TestForcedException" })
    public void testProcessItemException() throws Exception {
        test("ProcessItemExceptionServlet", "jslName=ProcessItemException&testName=testProcessItemException");
    }
}
