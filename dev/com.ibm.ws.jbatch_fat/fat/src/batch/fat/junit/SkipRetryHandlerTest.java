/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
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

import com.ibm.ws.jbatch.test.FatUtils;

import batch.fat.util.BatchFATHelper;
import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class SkipRetryHandlerTest extends BatchFATHelper {

    private static final Class testClass = SkipRetryHandlerTest.class;

    @BeforeClass
    public static void setup() throws Exception {
        BatchFATHelper.setConfig(DFLT_SERVER_XML, testClass);
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
     * Skip test to ensure included exceptions are skipped over and the job still passes
     */
    @Test
    @AllowedFFDC({
                   "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                   "batch.fat.common.MyParentException",
                   "batch.fat.common.MyGrandchildException"
    })
    public void testSkipPassMultipleExceptions() throws Exception {
        test("SkipRetryHandler", "jslName=skipMultipleExceptions&testName=testSkipPassMultipleExceptions");
    }

    /**
     * Skip test to ensure that when the skip limit is reached the job fails
     */
    @Test
    @AllowedFFDC({
                   "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                   "batch.fat.common.MyParentException",
                   "batch.fat.common.MyGrandchildException"
    })
    public void testSkipFailTooManyExceptions() throws Exception {
        test("SkipRetryHandler", "jslName=skipMultipleExceptions&testName=testSkipFailTooManyExceptions");
    }

    /**
     * Skip test to ensure that when an excluded skip exception is hit that the job fails
     */
    @Test
    @AllowedFFDC({
                   "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                   "batch.fat.common.MyParentException",
                   "batch.fat.common.MyGrandchildException",
                   "batch.fat.common.MyChildException"
    })
    public void testSkipFailExcludedException() throws Exception {
        test("SkipRetryHandler", "jslName=skipMultipleExceptions&testName=testSkipFailExcludedException");
    }

    /**
     * Retry test to ensure included exceptions are retried and the job still passes
     */
    @Test
    @AllowedFFDC({
                   "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                   "batch.fat.common.MyParentException",
                   "batch.fat.common.MyGrandchildException"

    })
    public void testRetryPassMultipleExceptions() throws Exception {
        test("SkipRetryHandler", "jslName=retryMultipleExceptions&testName=testRetryPassMultipleExceptions");
    }

    /**
     * Retry test to ensure that when the retry limit is reached the job fails
     */
    @Test
    @AllowedFFDC({
                   "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                   "batch.fat.common.MyParentException",
                   "batch.fat.common.MyGrandchildException"
    })
    public void testRetryFailTooManyExceptions() throws Exception {
        test("SkipRetryHandler", "jslName=retryMultipleExceptions&testName=testRetryFailTooManyExceptions");
    }

    /**
     * Retry test to ensure that when an excluded retry exception is hit that the job fails
     */
    @Test
    @AllowedFFDC({
                   "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                   "batch.fat.common.MyParentException",
                   "batch.fat.common.MyGrandchildException",
                   "batch.fat.common.MyChildException"
    })
    public void testRetryFailExcludedException() throws Exception {
        test("SkipRetryHandler", "jslName=retryMultipleExceptions&testName=testRetryFailExcludedException");
    }

    /**
     * Retry with rollbacks test to ensure included exceptions are retried and the job still passes
     */
    @Test
    @AllowedFFDC({
                   "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                   "batch.fat.common.MyParentException",
                   "batch.fat.common.MyGrandchildException"

    })
    public void testRetryRollbackPassMultipleExceptions() throws Exception {
        test("SkipRetryHandler", "jslName=retryRollbackMultipleExceptions&testName=testRetryRollbackPassMultipleExceptions");
    }

    /**
     * Retry with rollbacks test to ensure that when the retry limit is reached the job fails
     */
    @Test
    @AllowedFFDC({
                   "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                   "batch.fat.common.MyParentException",
                   "batch.fat.common.MyGrandchildException"
    })
    public void testRetryRollbackFailTooManyExceptions() throws Exception {
        test("SkipRetryHandler", "jslName=retryRollbackMultipleExceptions&testName=testRetryRollbackFailTooManyExceptions");
    }

    /**
     * Retry with rollbacks test to ensure that when an excluded retry exception is hit that the job fails
     */
    @Test
    @AllowedFFDC({
                   "com.ibm.jbatch.container.exception.BatchContainerRuntimeException",
                   "batch.fat.common.MyParentException",
                   "batch.fat.common.MyGrandchildException",
                   "batch.fat.common.MyChildException"
    })
    public void testRetryRollbackFailExcludedException() throws Exception {
        test("SkipRetryHandler", "jslName=retryRollbackMultipleExceptions&testName=testRetryRollbackFailExcludedException");
    }
}
