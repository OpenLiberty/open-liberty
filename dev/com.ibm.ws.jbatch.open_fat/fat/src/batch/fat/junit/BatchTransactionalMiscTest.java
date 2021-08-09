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

import java.nio.file.Paths;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import com.ibm.ws.jbatch.test.BatchAppUtils;
import com.ibm.ws.jbatch.test.FatUtils;

import batch.fat.util.BatchFATHelper;
import batch.fat.util.BatchFatUtils;
import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE9Action;

/**
 * As bugs are raised via RI, which itself doesn't handle transactions in SE, let's
 * collect the misc. collection here.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
// Seems unlikely to be affected by typical routine changes
public class BatchTransactionalMiscTest extends BatchFATHelper {

    private static final Class testClass = BatchTransactionalMiscTest.class;

    @BeforeClass
    public static void setup() throws Exception {

        BatchAppUtils.addDropinsBatchFATWar(server);
        BatchAppUtils.addDropinsDbServletAppWar(server);

        BatchFATHelper.setConfig("BatchTransactionalMiscTests/server.xml", testClass);
        BatchFATHelper.startServer(server, testClass);
        FatUtils.waitForSmarterPlanet(server);

        // Standard runtime tables
        createDefaultRuntimeTables();

        // The other tests' app table is fine
        Log.info(BatchTransactionalMiscTest.class, "setup", "Creating output table");
        executeSql("jdbc/batch", getChunkOutTableSql(APP_OUT1));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKY0011W");
        }
    }

    /*
     * The interesting variation-specific logic is all in the servlet
     */

    @Test
    @ExpectedFFDC({ "batch.fat.common.ForceRollbackException", "com.ibm.jbatch.container.exception.BatchContainerRuntimeException" })
    public void testUncaughtExceptionThenExceptionOnReaderClose() throws Exception {
        test("BatchTransactionalMisc", "testName=testUncaughtExceptionThenExceptionOnReaderClose");
    }

    @Test
    @ExpectedFFDC({ "batch.fat.common.ForceRollbackException", "com.ibm.jbatch.container.exception.BatchContainerRuntimeException" })
    public void testRetryableExceptionWithRollbackButExceptionOnReaderClose() throws Exception {
        test("BatchTransactionalMisc", "testName=testRetryableExceptionWithRollbackButExceptionOnReaderClose");
    }

    @Test
    @AllowedFFDC({ "batch.fat.common.ForceRollbackException", "com.ibm.jbatch.container.exception.BatchContainerRuntimeException" })
    public void testRetryableExceptionWithRollbackSuccessfulCompletion() throws Exception {
        test("BatchTransactionalMisc", "testName=testRetryableExceptionWithRollbackSuccessfulCompletion");
    }

    @Test
    @AllowedFFDC({ "batch.fat.common.ForceRollbackException", "com.ibm.jbatch.container.exception.BatchContainerRuntimeException" })
    public void testRetryableExceptionWithRollbackThenMultipleChunksSuccessfulCompletion() throws Exception {
        test("BatchTransactionalMisc", "testName=testRetryableExceptionWithRollbackThenMultipleChunksSuccessfulCompletion");
    }

}
