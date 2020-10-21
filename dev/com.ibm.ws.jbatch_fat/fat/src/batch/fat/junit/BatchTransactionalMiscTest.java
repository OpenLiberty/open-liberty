/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package batch.fat.junit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jbatch.test.FatUtils;

import batch.fat.util.BatchFATHelper;
import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

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
