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

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
// Seems unlikely to be affected by typical routine changes
public class MiscTest extends BatchFATHelper {

    private static final Class testClass = MiscTest.class;

    @BeforeClass
    public static void setup() throws Exception {
        BatchFATHelper.setConfig(DFLT_SERVER_XML, testClass);
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
