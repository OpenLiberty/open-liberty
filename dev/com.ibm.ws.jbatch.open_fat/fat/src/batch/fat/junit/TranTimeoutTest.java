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
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 7)
public class TranTimeoutTest extends BatchFATHelper {

    private static final Class testClass = TranTimeoutTest.class;

    @BeforeClass
    public static void setup() throws Exception {
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
    @ExpectedFFDC({ "javax.persistence.PersistenceException", "javax.transaction.RollbackException",
                    "com.ibm.jbatch.container.exception.BatchContainerRuntimeException" })
    public void testJSLTransactionTimeoutStep1Fail() throws Exception {
        test("TranTimeout", "jslName=ChunkTranTimeout&variation=1"); // Doesn't use jslName actually
    }

    @Test
    @ExpectedFFDC({ "javax.persistence.PersistenceException", "javax.transaction.RollbackException",
                    "com.ibm.jbatch.container.exception.BatchContainerRuntimeException" })
    public void testJSLTransactionTimeoutStep2Fail() throws Exception {
        test("TranTimeout", "jslName=ChunkTranTimeout&variation=2"); // Doesn't use jslName actually
    }

    @Test
    public void testJSLTransactionTimeoutComplete() throws Exception {
        test("TranTimeout", "jslName=ChunkTranTimeout&variation=3"); // Doesn't use jslName actually
    }
}
