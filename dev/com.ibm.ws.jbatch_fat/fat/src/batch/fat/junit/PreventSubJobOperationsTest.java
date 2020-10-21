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
