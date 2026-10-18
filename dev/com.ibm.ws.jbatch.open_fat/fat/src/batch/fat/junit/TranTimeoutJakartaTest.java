/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test class for jakarta.transaction.global.timeout property (Jakarta EE 9+)
 * Tests that the Jakarta property key works correctly for batch transaction timeouts
 */
@RunWith(FATRunner.class)
public class TranTimeoutJakartaTest extends BatchFATHelper {

    private static final Class testClass = TranTimeoutJakartaTest.class;

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

    /**
     * Test that jakarta.transaction.global.timeout property causes step1 to timeout and fail
     */
    @Test
    @ExpectedFFDC({ "javax.persistence.PersistenceException", "javax.transaction.RollbackException",
                    "com.ibm.jbatch.container.exception.BatchContainerRuntimeException" })
    public void testJakartaTransactionTimeoutStep1Fail() throws Exception {
        test("TranTimeoutJakarta", "jslName=ChunkTranTimeoutJakarta&variation=1");
    }

    /**
     * Test that jakarta.transaction.global.timeout property causes step2 to timeout and fail
     * (step1 completes successfully)
     */
    @Test
    @ExpectedFFDC({ "javax.persistence.PersistenceException", "javax.transaction.RollbackException",
                    "com.ibm.jbatch.container.exception.BatchContainerRuntimeException" })
    public void testJakartaTransactionTimeoutStep2Fail() throws Exception {
        test("TranTimeoutJakarta", "jslName=ChunkTranTimeoutJakarta&variation=2");
    }

    /**
     * Test that jakarta.transaction.global.timeout property allows both steps to complete
     * when timeout is sufficient
     */
    @Test
    public void testJakartaTransactionTimeoutComplete() throws Exception {
        test("TranTimeoutJakarta", "jslName=ChunkTranTimeoutJakarta&variation=3");
    }
}
