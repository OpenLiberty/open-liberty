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

import batch.fat.util.BatchFATHelper;

import com.ibm.ws.jbatch.test.FatUtils;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ParallelContextPropagationTest extends BatchFATHelper {

    private static final Class testClass = ParallelContextPropagationTest.class;

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
